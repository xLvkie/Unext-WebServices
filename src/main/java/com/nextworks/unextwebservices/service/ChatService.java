package com.nextworks.unextwebservices.service;

import com.nextworks.unextwebservices.dto.chat.MessageRequestDTO;
import com.nextworks.unextwebservices.dto.chat.MessageResponseDTO;
import com.nextworks.unextwebservices.entity.*;
import com.nextworks.unextwebservices.entity.enums.ApplicationStatus;
import com.nextworks.unextwebservices.entity.enums.ChatContext;
import com.nextworks.unextwebservices.entity.enums.ConnectionStatus;
import com.nextworks.unextwebservices.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final JobApplicationRepository applicationRepository;
    private final PostulantProfileRepository postulantRepository;
    private final RecruiterProfileRepository recruiterRepository;
    private final UserConnectionRepository connectionRepository;

    @Transactional
    public MessageResponseDTO sendMessage(String email, MessageRequestDTO request) {
        User sender = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Remitente no encontrado"));

        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new RuntimeException("Destinatario no encontrado"));

        JobApplication application = null;
        if (request.getJobApplicationId() != null) {
            application = applicationRepository.findById(request.getJobApplicationId())
                    .orElseThrow(() -> new RuntimeException("Postulación no encontrada"));
        }

        String senderRole = sender.getRole().name();
        String receiverRole = receiver.getRole().name();

        // Verificamos si ambos usuarios ya tienen una conexión ACCEPTED en Mi Red
        boolean isAlreadyConnected = connectionRepository
                .findBetweenUsers(sender.getId(), receiver.getId())
                .map(c -> c.getStatus() == ConnectionStatus.ACCEPTED)
                .orElse(false);

        // O si ya existe historial de mensajes previo entre ambos (ej. el reclutador ya le habló)
        boolean hasPreviousConversation = !messageRepository
                .findChatHistory(sender.getId(), receiver.getId())
                .isEmpty();

        // Validación al segmento POSTULANTE
        if (senderRole.equals("POSTULANT")) {
            // Caso 1: Postulante -> Reclutador
            if (receiverRole.equals("RECRUITER")) {
                PostulantProfile postulant = postulantRepository.findByUserId(sender.getId())
                        .orElseThrow(() -> new RuntimeException("Perfil de postulante no encontrado"));
                RecruiterProfile recruiter = recruiterRepository.findByUserId(receiver.getId())
                        .orElseThrow(() -> new RuntimeException("Perfil de empresa no encontrado"));

                // Si no enviaron jobApplicationId desde el frontend, buscamos automáticamente su postulación con esta empresa
                if (application == null) {
                    List<JobApplication> myAppsWithRecruiter = applicationRepository
                            .findByPostulantProfileId(postulant.getId())
                            .stream()
                            .filter(a -> a.getJobOffer().getRecruiterProfile().getId().equals(recruiter.getId()))
                            .toList();

                    application = myAppsWithRecruiter.stream()
                            .filter(a -> a.getStatus() == ApplicationStatus.UNDER_REVIEW
                                    || a.getStatus() == ApplicationStatus.ACCEPTED)
                            .findFirst()
                            .orElse(myAppsWithRecruiter.isEmpty() ? null : myAppsWithRecruiter.get(0));
                }

                // Permitimos enviar el mensaje si:
                // 1) Ya están conectados en Mi Red, o
                // 2) El reclutador ya inició la conversación antes, o
                // 3) Tiene una postulación en etapa UNDER_REVIEW o ACCEPTED
                boolean hasValidApplicationStage = application != null &&
                        (application.getStatus() == ApplicationStatus.UNDER_REVIEW ||
                                application.getStatus() == ApplicationStatus.ACCEPTED);

                if (!isAlreadyConnected && !hasPreviousConversation && !hasValidApplicationStage) {
                    throw new RuntimeException("HTTP 403: Para enviar mensajes a esta empresa, tu postulación debe estar en etapa de evaluación o el reclutador debe haber habilitado el chat.");
                }
            }
            // Caso 2: Postulante -> Institución
            else if (receiverRole.equals("INSTITUTION")) {
                PostulantProfile postulant = postulantRepository.findByUserId(sender.getId())
                        .orElseThrow(() -> new RuntimeException("Perfil de postulante no encontrado"));

                if (!isAlreadyConnected && !hasPreviousConversation) {
                    if (postulant.getInstitutionProfile() == null) {
                        throw new RuntimeException("HTTP 403: No puedes contactar a esta institución porque no tienes un vínculo académico registrado.");
                    }
                    if (!postulant.getInstitutionProfile().getUser().getId().equals(receiver.getId())) {
                        throw new RuntimeException("HTTP 403: Solo puedes comunicarte con tu propia institución.");
                    }
                    if (!Boolean.TRUE.equals(postulant.getIsInstitutionVerified())) {
                        throw new RuntimeException("HTTP 403: Tu vínculo académico aún está en revisión. Espera la aprobación para usar el chat.");
                    }
                }
            }
        }

        ChatContext context = (application != null) ? ChatContext.JOB_APPLICATION : ChatContext.GENERAL;

        Message message = Message.builder()
                .contextType(context)
                .jobApplication(application)
                .sender(sender)
                .receiver(receiver)
                .content(request.getContent())
                .build();

        messageRepository.save(message);

        return mapToDTO(message);
    }

    @Transactional(readOnly = true)
    public List<MessageResponseDTO> getInbox(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        List<Message> inboxMessages = messageRepository.findInboxForUser(user.getId());

        return inboxMessages.stream().map(this::mapToDTO).toList();
    }

    @Transactional
    public List<MessageResponseDTO> getChatHistory(String email, UUID receiverId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        List<Message> history = messageRepository.findChatHistory(user.getId(), receiverId);

        boolean hasUnreadMessages = false;

        for (Message message : history) {
            if (message.getReceiver().getId().equals(user.getId()) && !message.getIsRead()) {
                message.setIsRead(true);
                hasUnreadMessages = true;
            }
        }

        if (hasUnreadMessages) {
            messageRepository.saveAll(history);
        }

        return history.stream().map(this::mapToDTO).toList();
    }

    private MessageResponseDTO mapToDTO(Message message) {
        return MessageResponseDTO.builder()
                .id(message.getId())
                .senderId(message.getSender().getId())
                .receiverId(message.getReceiver().getId())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .isRead(message.getIsRead())
                .contextType(message.getContextType())
                .build();
    }
}