package com.nextworks.unextwebservices.service;

import com.nextworks.unextwebservices.dto.connection.ConnectionStatusResponseDTO;
import com.nextworks.unextwebservices.entity.*;
import com.nextworks.unextwebservices.entity.enums.ConnectionStatus;
import com.nextworks.unextwebservices.entity.enums.Role;
import com.nextworks.unextwebservices.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConnectionService {

    private final UserConnectionRepository connectionRepository;
    private final UserRepository userRepository;
    private final PostulantProfileRepository postulantRepository;
    private final RecruiterProfileRepository recruiterRepository;
    private final InstitutionProfileRepository institutionRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public ConnectionStatusResponseDTO getStatusWithUser(String currentEmail, UUID targetUserId) {
        User currentUser = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario destino no encontrado"));

        Optional<UserConnection> existing = connectionRepository.findBetweenUsers(currentUser.getId(), targetUser.getId());
        if (existing.isPresent()) {
            UserConnection conn = existing.get();
            if (conn.getStatus() == ConnectionStatus.ACCEPTED) {
                return ConnectionStatusResponseDTO.builder()
                        .connectionId(conn.getId())
                        .targetUserId(targetUserId)
                        .status("CONNECTED")
                        .canConnect(true)
                        .build();
            }
            if (conn.getStatus() == ConnectionStatus.PENDING) {
                boolean sentByMe = conn.getRequester().getId().equals(currentUser.getId());
                return ConnectionStatusResponseDTO.builder()
                        .connectionId(conn.getId())
                        .targetUserId(targetUserId)
                        .status(sentByMe ? "PENDING_SENT" : "PENDING_RECEIVED")
                        .canConnect(true)
                        .build();
            }
        }

        // Evaluamos si existe alguna restricción según el segmento
        String restriction = evaluateConnectionRestriction(currentUser, targetUser);

        return ConnectionStatusResponseDTO.builder()
                .targetUserId(targetUserId)
                .status("NONE")
                .canConnect(restriction == null)
                .restrictionReason(restriction)
                .build();
    }

    @Transactional
    public ConnectionStatusResponseDTO sendConnectionRequest(String currentEmail, UUID targetUserId) {
        User currentUser = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario destino no encontrado"));

        if (currentUser.getId().equals(targetUser.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puedes enviarte una solicitud a ti mismo.");
        }

        Optional<UserConnection> existing = connectionRepository.findBetweenUsers(currentUser.getId(), targetUser.getId());
        if (existing.isPresent() && existing.get().getStatus() != ConnectionStatus.REJECTED) {
            return getStatusWithUser(currentEmail, targetUserId);
        }

        String restriction = evaluateConnectionRestriction(currentUser, targetUser);
        if (restriction != null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, restriction);
        }

        UserConnection conn = existing.orElseGet(() -> UserConnection.builder()
                .requester(currentUser)
                .addressee(targetUser)
                .build());

        conn.setRequester(currentUser);
        conn.setAddressee(targetUser);
        conn.setStatus(ConnectionStatus.PENDING);
        connectionRepository.save(conn);

        notificationService.createNotification(
                targetUser,
                "Nueva solicitud para conectar en tu red",
                resolveDisplayName(currentUser) + " desea conectar contigo en Unext."
        );

        return ConnectionStatusResponseDTO.builder()
                .connectionId(conn.getId())
                .targetUserId(targetUserId)
                .status("PENDING_SENT")
                .canConnect(true)
                .build();
    }

    @Transactional
    public ConnectionStatusResponseDTO acceptConnectionRequest(String currentEmail, UUID connectionId) {
        User currentUser = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        UserConnection conn = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitud no encontrada"));

        if (!conn.getAddressee().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para aceptar esta solicitud");
        }

        conn.setStatus(ConnectionStatus.ACCEPTED);
        connectionRepository.save(conn);

        notificationService.createNotification(
                conn.getRequester(),
                "¡Solicitud de conexión aceptada!",
                resolveDisplayName(currentUser) + " aceptó tu solicitud de conexión. Ya forman parte de la misma red."
        );

        return ConnectionStatusResponseDTO.builder()
                .connectionId(conn.getId())
                .targetUserId(conn.getRequester().getId())
                .status("CONNECTED")
                .canConnect(true)
                .build();
    }

    @Transactional
    public void removeOrCancelConnection(String currentEmail, UUID targetUserId) {
        User currentUser = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        connectionRepository.findBetweenUsers(currentUser.getId(), targetUserId)
                .ifPresent(connectionRepository::delete);
    }

    /* =========================================================================
       REGLAS DE NEGOCIO DE CONEXIÓN POR SEGMENTO (POSTULANT, RECRUITER, INSTITUTION)
       ========================================================================= */
    private String evaluateConnectionRestriction(User requester, User target) {
        Role reqRole = requester.getRole();
        Role targetRole = target.getRole();

        // 1. La INSTITUCIÓN no tiene restricciones con ningún segmento
        if (reqRole == Role.INSTITUTION) {
            return null;
        }

        // 2. Reglas cuando quien solicita es un POSTULANT
        if (reqRole == Role.POSTULANT) {
            // Postulante -> Postulante: Sin restricción
            if (targetRole == Role.POSTULANT) {
                return null;
            }

            PostulantProfile postulant = postulantRepository.findByUserId(requester.getId()).orElse(null);
            if (postulant == null) {
                return "Primero debes completar tu perfil de postulante.";
            }

            // Postulante -> Empresa (RECRUITER): Solo si ya postuló a una vacante de esa empresa
            if (targetRole == Role.RECRUITER) {
                RecruiterProfile recruiter = recruiterRepository.findByUserId(target.getId()).orElse(null);
                if (recruiter == null) return "Perfil de empresa no disponible.";

                boolean hasApplied = jobApplicationRepository.hasPostulantAppliedToRecruiter(postulant.getId(), recruiter.getId());
                if (!hasApplied) {
                    return "Para conectar con esta empresa, primero debes haber postulado a una de sus vacantes activas.";
                }
                return null;
            }

            // Postulante -> Institución (INSTITUTION): Solo si está verificado con dicha institución
            if (targetRole == Role.INSTITUTION) {
                InstitutionProfile inst = institutionRepository.findByUserId(target.getId()).orElse(null);
                if (inst == null) return "Perfil institucional no disponible.";

                boolean verifiedWithThisInst = Boolean.TRUE.equals(postulant.getIsInstitutionVerified())
                        && postulant.getInstitutionProfile() != null
                        && postulant.getInstitutionProfile().getId().equals(inst.getId());

                if (!verifiedWithThisInst) {
                    return "Solo puedes conectar con una institución educativa una vez que haya validado tu vínculo académico.";
                }
                return null;
            }
        }

        // 3. Reglas cuando quien solicita es un RECRUITER
        if (reqRole == Role.RECRUITER) {
            if (targetRole == Role.POSTULANT || targetRole == Role.RECRUITER) {
                return null;
            }
            if (targetRole == Role.INSTITUTION) {
                return "Las empresas solo pueden conectar con instituciones mediante solicitud formal de convenio.";
            }
        }

        return null;
    }

    private String resolveDisplayName(User user) {
        return switch (user.getRole()) {
            case POSTULANT -> postulantRepository.findByUserId(user.getId())
                    .map(p -> p.getFirstName() + " " + p.getLastName())
                    .orElse(user.getEmail());
            case RECRUITER -> recruiterRepository.findByUserId(user.getId())
                    .map(RecruiterProfile::getCompanyName)
                    .orElse(user.getEmail());
            case INSTITUTION -> institutionRepository.findByUserId(user.getId())
                    .map(InstitutionProfile::getName)
                    .orElse(user.getEmail());
        };
    }
}