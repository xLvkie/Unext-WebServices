package com.nextworks.unextwebservices.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "feed_posts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedPost {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_user_id", nullable = false)
    private User author;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "feed_post_media", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "media_url", columnDefinition = "TEXT")
    @Builder.Default
    private List<String> mediaUrls = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "feed_post_likes",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @Builder.Default
    private Set<User> likedByUsers = new HashSet<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<FeedComment> comments = new ArrayList<>();

    @Column(name = "is_important")
    @Builder.Default
    private Boolean isImportant = false;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.isImportant == null) {
            this.isImportant = false;
        }
        if (!this.isImportant && this.expiresAt == null) {
            this.expiresAt = LocalDateTime.now().plusDays(7);
        }
    }
}