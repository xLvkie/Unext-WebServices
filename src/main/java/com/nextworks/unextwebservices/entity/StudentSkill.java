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

    // Relación: Muchas habilidades pertenecen a un solo perfil de postulante
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "postulant_profile_id", nullable = false)
    private PostulantProfile postulantProfile;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "mastery_level", nullable = false, length = 50)
    private String masteryLevel; // Ej: BASICO, INTERMEDIO, AVANZADO

    @Column(name = "is_validated_by_institution")
    private Boolean isValidatedByInstitution;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Este método se ejecuta automáticamente justo antes de guardar en la base de datos
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.isValidatedByInstitution == null) {
            this.isValidatedByInstitution = false; // Por defecto nace sin validar
        }
    }
}
