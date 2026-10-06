package com.nextworks.unextwebservices.entity;

import com.nextworks.unextwebservices.entity.enums.AgreementStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "internship_agreements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InternshipAgreement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_profile_id", nullable = false)
    private InstitutionProfile institutionProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiter_profile_id", nullable = false)
    private RecruiterProfile recruiterProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "postulant_profile_id", nullable = false)
    private PostulantProfile postulantProfile;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "weekly_hours", nullable = false)
    private Integer weeklyHours;

    @Column(name = "document_url")
    private String documentUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AgreementStatus status;

    @Column(columnDefinition = "TEXT")
    private String observations;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = AgreementStatus.PENDING;
        }
    }
}
