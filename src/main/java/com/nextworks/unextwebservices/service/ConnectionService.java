package com.nextworks.unextwebservices.service;

import com.nextworks.unextwebservices.dto.connection.ConnectionStatusResponseDTO;
import com.nextworks.unextwebservices.dto.connection.NetworkOverviewDTO;
import com.nextworks.unextwebservices.dto.connection.NetworkPersonCardDTO;
import com.nextworks.unextwebservices.entity.*;
import com.nextworks.unextwebservices.entity.enums.ConnectionStatus;
import com.nextworks.unextwebservices.entity.enums.Role;
import com.nextworks.unextwebservices.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
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
    private final UserFollowRepository followRepository;

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

        boolean autoAccept = currentUser.getRole() == Role.RECRUITER
                && targetUser.getRole() == Role.POSTULANT;

        conn.setStatus(autoAccept ? ConnectionStatus.ACCEPTED : ConnectionStatus.PENDING);
        connectionRepository.save(conn);

        notificationService.createNotification(
                targetUser,
                autoAccept ? "¡Nueva conexión corporativa!" : "Nueva solicitud para conectar en tu red",
                autoAccept
                        ? resolveDisplayName(currentUser) + " se ha conectado contigo para conversar por mensajería."
                        : resolveDisplayName(currentUser) + " desea conectar contigo en Unext."
        );

        return ConnectionStatusResponseDTO.builder()
                .connectionId(conn.getId())
                .targetUserId(targetUserId)
                .status(autoAccept ? "CONNECTED" : "PENDING_SENT")
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

    @Transactional(readOnly = true)
    public NetworkOverviewDTO getNetworkOverview(String currentEmail) {
        User currentUser = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        UUID myUserId = currentUser.getId();

        // Invitaciones recibidas pendientes
        List<UserConnection> receivedList = connectionRepository
                .findByAddresseeIdAndStatusOrderByCreatedAtDesc(myUserId, ConnectionStatus.PENDING);

        // Invitaciones enviadas pendientes
        List<UserConnection> sentList = connectionRepository
                .findByRequesterIdAndStatusOrderByCreatedAtDesc(myUserId, ConnectionStatus.PENDING);

        // Contactos confirmados (ACCEPTED)
        List<UserConnection> acceptedList = connectionRepository
                .findAllByUserAndStatus(myUserId, ConnectionStatus.ACCEPTED);

        // Usuarios seguidos
        List<UserFollow> followList = followRepository.findByFollowerIdOrderByCreatedAtDesc(myUserId);
        java.util.Set<UUID> followedUserIds = followList.stream()
                .map(f -> f.getFollowed().getId())
                .collect(java.util.stream.Collectors.toSet());

        List<NetworkPersonCardDTO> receivedDTOs = receivedList.stream()
                .map(c -> buildPersonCard(currentUser, c.getRequester(), c.getId(), "PENDING_RECEIVED",
                        followedUserIds.contains(c.getRequester().getId()), c.getCreatedAt()))
                .filter(java.util.Objects::nonNull)
                .toList();

        List<NetworkPersonCardDTO> sentDTOs = sentList.stream()
                .map(c -> buildPersonCard(currentUser, c.getAddressee(), c.getId(), "PENDING_SENT",
                        followedUserIds.contains(c.getAddressee().getId()), c.getCreatedAt()))
                .filter(java.util.Objects::nonNull)
                .toList();

        List<NetworkPersonCardDTO> connectionsDTOs = acceptedList.stream()
                .map(c -> {
                    User other = c.getRequester().getId().equals(myUserId) ? c.getAddressee() : c.getRequester();
                    return buildPersonCard(currentUser, other, c.getId(), "CONNECTED",
                            followedUserIds.contains(other.getId()), c.getCreatedAt());
                })
                .filter(java.util.Objects::nonNull)
                .toList();

        List<NetworkPersonCardDTO> followingDTOs = followList.stream()
                .map(f -> {
                    User target = f.getFollowed();
                    Optional<UserConnection> conn = connectionRepository.findBetweenUsers(myUserId, target.getId());
                    String status = conn.map(c -> c.getStatus() == ConnectionStatus.ACCEPTED ? "CONNECTED"
                                    : (c.getRequester().getId().equals(myUserId) ? "PENDING_SENT" : "PENDING_RECEIVED"))
                            .orElse("NONE");
                    UUID connId = conn.map(UserConnection::getId).orElse(null);
                    return buildPersonCard(currentUser, target, connId, status, true, f.getCreatedAt());
                })
                .filter(java.util.Objects::nonNull)
                .toList();

        // IDs que ya tienen vínculo activo (para no repetirlos en Recomendaciones de conexión)
        java.util.Set<UUID> excludedFromRecs = new java.util.HashSet<>();
        excludedFromRecs.add(myUserId);
        receivedDTOs.forEach(d -> excludedFromRecs.add(d.getUserId()));
        sentDTOs.forEach(d -> excludedFromRecs.add(d.getUserId()));
        connectionsDTOs.forEach(d -> excludedFromRecs.add(d.getUserId()));

        // Recomendaciones (Postulantes verificados, Empresas verificadas e Instituciones verificadas)
        java.util.List<NetworkPersonCardDTO> recommendations = new java.util.ArrayList<>();

        postulantRepository.findByIsVerifiedTrue().forEach(p -> {
            UUID uid = p.getUser().getId();
            if (!excludedFromRecs.contains(uid)) {
                NetworkPersonCardDTO card = buildPersonCard(currentUser, p.getUser(), null, "NONE",
                        followedUserIds.contains(uid), p.getCreatedAt());
                if (card != null) recommendations.add(card);
            }
        });

        recruiterRepository.findByIsVerifiedTrue().forEach(r -> {
            UUID uid = r.getUser().getId();
            if (!excludedFromRecs.contains(uid)) {
                NetworkPersonCardDTO card = buildPersonCard(currentUser, r.getUser(), null, "NONE",
                        followedUserIds.contains(uid), null);
                if (card != null) recommendations.add(card);
            }
        });

        institutionRepository.findByIsVerifiedTrue().forEach(i -> {
            UUID uid = i.getUser().getId();
            if (!excludedFromRecs.contains(uid)) {
                NetworkPersonCardDTO card = buildPersonCard(currentUser, i.getUser(), null, "NONE",
                        followedUserIds.contains(uid), null);
                if (card != null) recommendations.add(card);
            }
        });

        return NetworkOverviewDTO.builder()
                .connectionsCount(connectionsDTOs.size())
                .receivedInvitationsCount(receivedDTOs.size())
                .sentInvitationsCount(sentDTOs.size())
                .followingCount(followingDTOs.size())
                .receivedInvitations(receivedDTOs)
                .sentInvitations(sentDTOs)
                .connections(connectionsDTOs)
                .following(followingDTOs)
                .recommendations(recommendations)
                .build();
    }

    @Transactional
    public boolean toggleFollow(String currentEmail, UUID targetUserId) {
        User currentUser = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario destino no encontrado"));

        if (currentUser.getId().equals(targetUser.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puedes seguirte a ti mismo.");
        }

        Optional<UserFollow> existing = followRepository.findByFollowerIdAndFollowedId(currentUser.getId(), targetUserId);
        if (existing.isPresent()) {
            followRepository.delete(existing.get());
            return false; // Dejó de seguir
        } else {
            UserFollow follow = UserFollow.builder()
                    .follower(currentUser)
                    .followed(targetUser)
                    .build();
            followRepository.save(follow);
            return true; // Ahora lo sigue
        }
    }

    private NetworkPersonCardDTO buildPersonCard(
            User currentUser,
            User targetUser,
            UUID connectionId,
            String connectionStatus,
            boolean isFollowing,
            java.time.LocalDateTime createdAt
    ) {
        String restriction = evaluateConnectionRestriction(currentUser, targetUser);

        switch (targetUser.getRole()) {
            case POSTULANT -> {
                PostulantProfile p = postulantRepository.findByUserId(targetUser.getId()).orElse(null);
                if (p == null) return null;
                String fullName = (p.getFirstName() + " " + p.getLastName()).trim();
                String headline = (p.getHeadline() != null && !p.getHeadline().isBlank())
                        ? p.getHeadline()
                        : (p.getCareer() != null ? "Estudiante de " + p.getCareer() : "Talento Profesional en Unext");
                String subtitle = p.getInstitutionProfile() != null
                        ? p.getInstitutionProfile().getName()
                        : "Comunidad Unext";

                return NetworkPersonCardDTO.builder()
                        .connectionId(connectionId)
                        .userId(targetUser.getId())
                        .profileId(p.getId())
                        .role("POSTULANT")
                        .name(fullName)
                        .headline(headline)
                        .subtitle(subtitle)
                        .avatarUrl(p.getAvatarUrl())
                        .bannerUrl(p.getBannerUrl())
                        .bannerText(p.getBannerText() != null ? p.getBannerText() : p.getCareer())
                        .isVerified(Boolean.TRUE.equals(p.getIsInstitutionVerified()))
                        .connectionStatus(connectionStatus)
                        .isFollowing(isFollowing)
                        .canConnect(restriction == null)
                        .restrictionReason(restriction)
                        .createdAt(createdAt)
                        .build();
            }
            case RECRUITER -> {
                RecruiterProfile r = recruiterRepository.findByUserId(targetUser.getId()).orElse(null);
                if (r == null) return null;
                return NetworkPersonCardDTO.builder()
                        .connectionId(connectionId)
                        .userId(targetUser.getId())
                        .profileId(r.getId())
                        .role("RECRUITER")
                        .name(r.getCompanyName())
                        .headline(r.getIndustry() != null ? r.getIndustry() : "Sector Corporativo")
                        .subtitle("Empresa Reclutadora · RUC " + r.getRuc())
                        .avatarUrl(r.getCompanyLogoUrl())
                        .bannerUrl(null)
                        .bannerText(r.getCompanyName())
                        .isVerified(Boolean.TRUE.equals(r.getIsValidated()) || Boolean.TRUE.equals(r.getIsVerified()))
                        .connectionStatus(connectionStatus)
                        .isFollowing(isFollowing)
                        .canConnect(restriction == null)
                        .restrictionReason(restriction)
                        .createdAt(createdAt)
                        .build();
            }
            case INSTITUTION -> {
                InstitutionProfile i = institutionRepository.findByUserId(targetUser.getId()).orElse(null);
                if (i == null) return null;
                return NetworkPersonCardDTO.builder()
                        .connectionId(connectionId)
                        .userId(targetUser.getId())
                        .profileId(i.getId())
                        .role("INSTITUTION")
                        .name(i.getName())
                        .headline("Institución Educativa Oficial")
                        .subtitle("Dominio: " + i.getDomain())
                        .avatarUrl(i.getLogoUrl())
                        .bannerUrl(null)
                        .bannerText(i.getName())
                        .isVerified(Boolean.TRUE.equals(i.getIsVerified()))
                        .connectionStatus(connectionStatus)
                        .isFollowing(isFollowing)
                        .canConnect(restriction == null)
                        .restrictionReason(restriction)
                        .createdAt(createdAt)
                        .build();
            }
        }
        return null;
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