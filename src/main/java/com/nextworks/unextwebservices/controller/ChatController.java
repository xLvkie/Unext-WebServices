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
    }

    @GetMapping("/inbox")
    public ResponseEntity<List<MessageResponseDTO>> getInbox(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(chatService.getInbox(user.getEmail()));
    }

    @GetMapping("/history/{receiverId}")
    public ResponseEntity<List<MessageResponseDTO>> getChatHistory(
            Authentication authentication,
            @PathVariable UUID receiverId) {

        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(chatService.getChatHistory(user.getEmail(), receiverId));
    }
}