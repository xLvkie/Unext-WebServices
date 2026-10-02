package com.nextworks.unextwebservices.entity;

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

    // Relación con la Institución a la que se le pide la validación
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_profile_id", nullable = false)
    private InstitutionProfile institutionProfile;

    @Column(name = "knowledge_title", nullable = false, length = 150)
    private String knowledgeTitle;

    @Column(name = "evidence_url", nullable = false)
    private String evidenceUrl;

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