package com.nextworks.unextwebservices.service;

import com.nextworks.unextwebservices.dto.MessageRequestDTO;
import com.nextworks.unextwebservices.dto.MessageResponseDTO;
import com.nextworks.unextwebservices.entity.*;
import com.nextworks.unextwebservices.repository.JobApplicationRepository;
import com.nextworks.unextwebservices.repository.MessageRepository;
import com.nextworks.unextwebservices.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final JobApplicationRepository applicationRepository;

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
        // Validación al segmeto POSTULANTE
        if (senderRole.equals("POSTULANT")) {
            // Caso 1: Postulante -> Reclutador
            if (receiverRole.equals("RECRUITER")) {
                if (application == null) {
                    throw new RuntimeException("HTTP 400: Se requiere el ID de una postulación para contactar a una empresa.");
                }
                if (application.getStatus() != ApplicationStatus.UNDER_REVIEW) {
                    throw new RuntimeException("HTTP 403: No puedes enviar mensajes. La postulación no está en etapa de revisión.");
                }
            }
            // Caso 2: Postulante -> Institución
            else if (receiverRole.equals("INSTITUTION")) {
                // TODO: Implementar lógica de conexión Postulante - Institución
                throw new RuntimeException("HTTP 403: La validación de conexión con la institución aún no está configurada.");
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
