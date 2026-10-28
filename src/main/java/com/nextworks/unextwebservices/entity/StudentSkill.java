package com.nextworks.unextwebservices.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "student_skills")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "postulant_profile_id", nullable = false)
    private PostulantProfile postulantProfile;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "mastery_level", nullable = false, length = 50)
    private String masteryLevel; // BASICO, INTERMEDIO, AVANZADO

    @Column(name = "is_top_skill")
    private Boolean isTopSkill;

    @Column(name = "is_validated_by_institution")
    private Boolean isValidatedByInstitution;

    @Column(name = "validated_institution_name", length = 150)
    private String validatedInstitutionName;

    @Column(name = "validated_institution_logo_url", columnDefinition = "TEXT")
    private String validatedInstitutionLogoUrl;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.isValidatedByInstitution == null) {
            this.isValidatedByInstitution = false;
        }
        if (this.isTopSkill == null) {
            this.isTopSkill = false;
        }
    }
}
