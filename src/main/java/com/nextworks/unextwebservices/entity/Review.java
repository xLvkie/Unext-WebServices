package com.nextworks.unextwebservices.entity;

import com.nextworks.unextwebservices.entity.enums.ReviewStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "reviews",
        uniqueConstraints = @UniqueConstraint(columnNames = {"internship_agreement_id", "recruiter_profile_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiter_profile_id", nullable = false)
    private RecruiterProfile recruiterProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "postulant_profile_id", nullable = false)
    private PostulantProfile postulantProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "internship_agreement_id", nullable = false)
    private InternshipAgreement internshipAgreement;

    @Column(nullable = false)
    private Integer stars;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewStatus status;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        if (this.status == null) {
            this.status = ReviewStatus.DRAFT;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
