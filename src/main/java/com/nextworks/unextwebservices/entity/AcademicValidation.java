package com.nextworks.unextwebservices.entity;

import com.nextworks.unextwebservices.entity.enums.ValidationStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "academic_validations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcademicValidation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "postulant_profile_id", nullable = false)
    private PostulantProfile postulantProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_profile_id", nullable = false)
    private InstitutionProfile institutionProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technical_skill_id", nullable = false)
    private StudentSkill technicalSkill;

    @Column(name = "evidence_url", nullable = false)
    private String evidenceUrl;

    @Column(columnDefinition = "TEXT")
    private String observation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ValidationStatus status;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = ValidationStatus.PENDING;
        }
    }
}