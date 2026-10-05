package com.nextworks.unextwebservices.controller;

import com.nextworks.unextwebservices.dto.MessageRequestDTO;
import com.nextworks.unextwebservices.dto.MessageResponseDTO;
import com.nextworks.unextwebservices.entity.User;
import com.nextworks.unextwebservices.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/send")
    public ResponseEntity<MessageResponseDTO> sendMessage(
            Authentication authentication,
            @Valid @RequestBody MessageRequestDTO request) {

        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(chatService.sendMessage(user.getEmail(), request));
        /*
        Para enviar un mensaje se pide rellenar los campos receiverId (!),
        content (!) y jobApplicationId (1)
        El user debe hacer uso del token y el user debe especificar el uuid del que recibe el mensaje
         */
    }

    @GetMapping("/inbox")
    public ResponseEntity<List<MessageResponseDTO>> getInbox(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(chatService.getInbox(user.getEmail()));
        /*
        Retorna una lista con los ultimos mensajes de todos los chats
        El user debe hacer uso del token
         */
    }

    @GetMapping("/history/{receiverId}")
    public ResponseEntity<List<MessageResponseDTO>> getChatHistory(
            Authentication authentication,
            @PathVariable UUID receiverId) {

        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(chatService.getChatHistory(user.getEmail(), receiverId));
        /*
        Retorna el historial de un chat mediante el id del que recibe el mensaje
        El user debe hacer uso del token
         */
    }
}