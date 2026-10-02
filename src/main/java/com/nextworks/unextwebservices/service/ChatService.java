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

        JobApplication application = applicationRepository.findById(request.getJobApplicationId())
                .orElseThrow(() -> new RuntimeException("Postulación no encontrada"));

        // REGLA DE NEGOCIO ESTRICTA: El chat solo se habilita en UNDER_REVIEW
        if (application.getStatus() != ApplicationStatus.UNDER_REVIEW) {
            throw new RuntimeException("HTTP 403: No puedes enviar mensajes. La postulación no está en etapa de revisión.");
        }

        Message message = Message.builder()
                .contextType(ChatContext.JOB_APPLICATION)
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

    @Transactional(readOnly = true)
    public List<MessageResponseDTO> getChatHistory(String email, UUID receiverId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        List<Message> history = messageRepository.findChatHistory(user.getId(), receiverId);

        return history.stream().map(this::mapToDTO).toList();
    }

    // Método auxiliar para no repetir código de mapeo
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
