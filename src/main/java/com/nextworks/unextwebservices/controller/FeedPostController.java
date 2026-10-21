package com.nextworks.unextwebservices.controller;

import com.nextworks.unextwebservices.dto.feed.FeedCommentRequestDTO;
import com.nextworks.unextwebservices.dto.feed.FeedCommentResponseDTO;
import com.nextworks.unextwebservices.dto.feed.FeedPostRequestDTO;
import com.nextworks.unextwebservices.dto.feed.FeedPostResponseDTO;
import com.nextworks.unextwebservices.entity.User;
import com.nextworks.unextwebservices.service.FeedPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
@Tag(name = "11. Feed de Publicaciones", description = "Muro social interactivo para Postulantes, Empresas e Instituciones")
public class FeedPostController {

    private final FeedPostService feedPostService;

    @Operation(summary = "Crear una publicación en el feed")
    @PreAuthorize("hasAnyAuthority('POSTULANT', 'RECRUITER', 'INSTITUTION')")
    @PostMapping
    public ResponseEntity<FeedPostResponseDTO> createPost(
            Authentication authentication,
            @Valid @RequestBody FeedPostRequestDTO request) {
        User user = (User) authentication.getPrincipal();
        assert user != null;
        return ResponseEntity.ok(feedPostService.createPost(user.getEmail(), request));
    }

    @Operation(summary = "Obtener el feed global de publicaciones (ordenado por recientes)")
    @PreAuthorize("hasAnyAuthority('POSTULANT', 'RECRUITER', 'INSTITUTION')")
    @GetMapping
    public ResponseEntity<List<FeedPostResponseDTO>> getGlobalFeed(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        assert user != null;
        return ResponseEntity.ok(feedPostService.getGlobalFeed(user.getEmail()));
    }

    @Operation(summary = "Obtener las publicaciones de un usuario específico")
    @PreAuthorize("hasAnyAuthority('POSTULANT', 'RECRUITER', 'INSTITUTION')")
    @GetMapping("/user/{authorUserId}")
    public ResponseEntity<List<FeedPostResponseDTO>> getPostsByUser(
            Authentication authentication,
            @PathVariable UUID authorUserId) {
        User user = (User) authentication.getPrincipal();
        assert user != null;
        return ResponseEntity.ok(feedPostService.getPostsByUser(authorUserId, user.getEmail()));
    }

    @Operation(summary = "Dar o quitar 'Me gusta' a una publicación")
    @PreAuthorize("hasAnyAuthority('POSTULANT', 'RECRUITER', 'INSTITUTION')")
    @PostMapping("/{postId}/like")
    public ResponseEntity<FeedPostResponseDTO> toggleLike(
            Authentication authentication,
            @PathVariable UUID postId) {
        User user = (User) authentication.getPrincipal();
        assert user != null;
        return ResponseEntity.ok(feedPostService.toggleLike(postId, user.getEmail()));
    }

    @Operation(summary = "Eliminar una publicación propia")
    @PreAuthorize("hasAnyAuthority('POSTULANT', 'RECRUITER', 'INSTITUTION')")
    @DeleteMapping("/{postId}")
    public ResponseEntity<String> deletePost(
            Authentication authentication,
            @PathVariable UUID postId) {
        User user = (User) authentication.getPrincipal();
        assert user != null;
        feedPostService.deletePost(postId, user.getEmail());
        return ResponseEntity.ok("Publicación eliminada exitosamente");
    }

    @Operation(summary = "Comentar en una publicación")
    @PreAuthorize("hasAnyAuthority('POSTULANT', 'RECRUITER', 'INSTITUTION')")
    @PostMapping("/{postId}/comments")
    public ResponseEntity<FeedCommentResponseDTO> addComment(
            Authentication authentication,
            @PathVariable UUID postId,
            @Valid @RequestBody FeedCommentRequestDTO request) {
        User user = (User) authentication.getPrincipal();
        assert user != null;
        return ResponseEntity.ok(feedPostService.addComment(postId, user.getEmail(), request));
    }

    @Operation(summary = "Obtener todos los comentarios de una publicación")
    @PreAuthorize("hasAnyAuthority('POSTULANT', 'RECRUITER', 'INSTITUTION')")
    @GetMapping("/{postId}/comments")
    public ResponseEntity<List<FeedCommentResponseDTO>> getCommentsByPost(@PathVariable UUID postId) {
        return ResponseEntity.ok(feedPostService.getCommentsByPost(postId));
    }

    @Operation(summary = "Eliminar un comentario (propio o en publicación propia)")
    @PreAuthorize("hasAnyAuthority('POSTULANT', 'RECRUITER', 'INSTITUTION')")
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<String> deleteComment(
            Authentication authentication,
            @PathVariable UUID commentId) {
        User user = (User) authentication.getPrincipal();
        assert user != null;
        feedPostService.deleteComment(commentId, user.getEmail());
        return ResponseEntity.ok("Comentario eliminado exitosamente");
    }
}