package com.nextworks.unextwebservices.service;

import com.nextworks.unextwebservices.dto.feed.FeedCommentRequestDTO;
import com.nextworks.unextwebservices.dto.feed.FeedCommentResponseDTO;
import com.nextworks.unextwebservices.dto.feed.FeedPostRequestDTO;
import com.nextworks.unextwebservices.dto.feed.FeedPostResponseDTO;
import com.nextworks.unextwebservices.entity.*;
import com.nextworks.unextwebservices.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FeedPostService {

    private final FeedPostRepository feedPostRepository;
    private final FeedCommentRepository feedCommentRepository;
    private final UserRepository userRepository;
    private final PostulantProfileRepository postulantRepository;
    private final RecruiterProfileRepository recruiterRepository;
    private final InstitutionProfileRepository institutionRepository;

    // 1. CREAR PUBLICACIÓN
    @Transactional
    public FeedPostResponseDTO createPost(String email, FeedPostRequestDTO request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        validateAuthorCanPublish(user);

        FeedPost post = FeedPost.builder()
                .author(user)
                .content(request.getContent().trim())
                .mediaUrls(request.getMediaUrls() != null ? request.getMediaUrls() : new ArrayList<>())
                .build();

        feedPostRepository.save(post);
        return mapToDTO(post, user.getId());
    }

    // 2. OBTENER EL FEED GENERAL
    @Transactional(readOnly = true)
    public List<FeedPostResponseDTO> getGlobalFeed(String email) {
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        return feedPostRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(post -> mapToDTO(post, currentUser.getId()))
                .toList();
    }

    // 3. OBTENER PUBLICACIONES DE UN USUARIO ESPECÍFICO
    @Transactional(readOnly = true)
    public List<FeedPostResponseDTO> getPostsByUser(UUID authorUserId, String currentEmail) {
        User currentUser = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        return feedPostRepository.findByAuthorIdOrderByCreatedAtDesc(authorUserId)
                .stream()
                .map(post -> mapToDTO(post, currentUser.getId()))
                .toList();
    }

    // 4. DAR O QUITAR "ME GUSTA" (TOGGLE LIKE)
    @Transactional
    public FeedPostResponseDTO toggleLike(UUID postId, String email) {
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        FeedPost post = feedPostRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Publicación no encontrada"));

        boolean alreadyLiked = post.getLikedByUsers().stream()
                .anyMatch(u -> u.getId().equals(currentUser.getId()));

        if (alreadyLiked) {
            post.getLikedByUsers().removeIf(u -> u.getId().equals(currentUser.getId()));
        } else {
            post.getLikedByUsers().add(currentUser);
        }

        feedPostRepository.save(post);
        return mapToDTO(post, currentUser.getId());
    }

    // 5. ELIMINAR PUBLICACIÓN
    @Transactional
    public void deletePost(UUID postId, String email) {
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        FeedPost post = feedPostRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Publicación no encontrada"));

        if (!post.getAuthor().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para eliminar esta publicación");
        }

        feedPostRepository.delete(post);
    }

    // 6. AGREGAR UN COMENTARIO A UNA PUBLICACIÓN
    @Transactional
    public FeedCommentResponseDTO addComment(UUID postId, String email, FeedCommentRequestDTO request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        validateAuthorCanPublish(user);

        FeedPost post = feedPostRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Publicación no encontrada"));

        FeedComment comment = FeedComment.builder()
                .post(post)
                .author(user)
                .content(request.getContent().trim())
                .build();

        feedCommentRepository.save(comment);
        return mapCommentToDTO(comment);
    }

    // 7. LISTAR COMENTARIOS DE UNA PUBLICACIÓN
    @Transactional(readOnly = true)
    public List<FeedCommentResponseDTO> getCommentsByPost(UUID postId) {
        if (!feedPostRepository.existsById(postId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Publicación no encontrada");
        }

        return feedCommentRepository.findByPostIdOrderByCreatedAtAsc(postId)
                .stream()
                .map(this::mapCommentToDTO)
                .toList();
    }

    // 8. ELIMINAR UN COMENTARIO (Autor del comentario o dueño del post)
    @Transactional
    public void deleteComment(UUID commentId, String email) {
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        FeedComment comment = feedCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comentario no encontrado"));

        boolean isCommentAuthor = comment.getAuthor().getId().equals(currentUser.getId());
        boolean isPostOwner = comment.getPost().getAuthor().getId().equals(currentUser.getId());

        if (!isCommentAuthor && !isPostOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para eliminar este comentario");
        }

        feedCommentRepository.delete(comment);
    }

    /* ==========================================================
       MÉTODOS AUXILIARES PRIVADOS (Resolución Polimórfica de Autor)
       ========================================================== */
    private record AuthorInfo(UUID profileId, String name, String headline, String avatarUrl, Boolean isVerified) {}

    private AuthorInfo resolveAuthorData(User author) {
        UUID profileId = null;
        String name = author.getEmail();
        String headline = "";
        String avatarUrl = null;
        Boolean isVerified = false;

        switch (author.getRole()) {
            case POSTULANT -> {
                PostulantProfile p = postulantRepository.findByUserId(author.getId()).orElse(null);
                if (p != null) {
                    profileId = p.getId();
                    name = p.getFirstName() + " " + p.getLastName();
                    headline = p.getHeadline() != null ? p.getHeadline() :
                            (p.getCareer() != null ? "Estudiante de " + p.getCareer() : "Postulante");
                    isVerified = Boolean.TRUE.equals(p.getIsInstitutionVerified());
                }
            }
            case RECRUITER -> {
                RecruiterProfile r = recruiterRepository.findByUserId(author.getId()).orElse(null);
                if (r != null) {
                    profileId = r.getId();
                    name = r.getCompanyName();
                    headline = r.getIndustry() != null ? r.getIndustry() : "Empresa Reclutadora";
                    avatarUrl = r.getCompanyLogoUrl();
                    isVerified = Boolean.TRUE.equals(r.getIsValidated());
                }
            }
            case INSTITUTION -> {
                InstitutionProfile i = institutionRepository.findByUserId(author.getId()).orElse(null);
                if (i != null) {
                    profileId = i.getId();
                    name = i.getName();
                    headline = "Institución Educativa · " + i.getDomain();
                    avatarUrl = i.getLogoUrl();
                    isVerified = Boolean.TRUE.equals(i.getIsVerified());
                }
            }
        }
        return new AuthorInfo(profileId, name, headline, avatarUrl, isVerified);
    }

    private void validateAuthorCanPublish(User user) {
        switch (user.getRole()) {
            case POSTULANT -> {
                PostulantProfile p = postulantRepository.findByUserId(user.getId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Completa tu perfil antes de interactuar en el feed"));
                if (!Boolean.TRUE.equals(p.getIsVerified())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tu perfil debe estar activo para interactuar");
                }
            }
            case RECRUITER -> {
                RecruiterProfile r = recruiterRepository.findByUserId(user.getId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Completa tu perfil de empresa antes de interactuar en el feed"));
                if (!Boolean.TRUE.equals(r.getIsVerified())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Debes verificar tu cuenta corporativa para interactuar en el feed");
                }
            }
            case INSTITUTION -> {
                InstitutionProfile i = institutionRepository.findByUserId(user.getId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Completa tu perfil institucional antes de interactuar en el feed"));
                if (!Boolean.TRUE.equals(i.getIsVerified())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Debes verificar tu cuenta institucional para interactuar en el feed");
                }
            }
        }
    }

    private FeedPostResponseDTO mapToDTO(FeedPost post, UUID currentUserId) {
        User author = post.getAuthor();
        AuthorInfo info = resolveAuthorData(author);

        boolean likedByMe = post.getLikedByUsers().stream()
                .anyMatch(u -> u.getId().equals(currentUserId));

        long commentsCount = feedCommentRepository.countByPostId(post.getId());

        return FeedPostResponseDTO.builder()
                .id(post.getId())
                .authorUserId(author.getId())
                .authorProfileId(info.profileId())
                .authorRole(author.getRole().name())
                .authorName(info.name())
                .authorHeadline(info.headline())
                .authorAvatarUrl(info.avatarUrl())
                .isAuthorVerified(info.isVerified())
                .content(post.getContent())
                .mediaUrls(post.getMediaUrls())
                .likesCount(post.getLikedByUsers().size())
                .commentsCount(commentsCount)
                .likedByMe(likedByMe)
                .createdAt(post.getCreatedAt())
                .build();
    }

    private FeedCommentResponseDTO mapCommentToDTO(FeedComment comment) {
        User author = comment.getAuthor();
        AuthorInfo info = resolveAuthorData(author);

        return FeedCommentResponseDTO.builder()
                .id(comment.getId())
                .postId(comment.getPost().getId())
                .authorUserId(author.getId())
                .authorProfileId(info.profileId())
                .authorRole(author.getRole().name())
                .authorName(info.name())
                .authorHeadline(info.headline())
                .authorAvatarUrl(info.avatarUrl())
                .isAuthorVerified(info.isVerified())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}