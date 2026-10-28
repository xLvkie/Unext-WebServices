package com.nextworks.unextwebservices.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "postulant_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostulantProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    private String avatarUrl;

    @Column(name = "banner_url", columnDefinition = "TEXT")
    private String bannerUrl;

    @Column(name = "banner_text", length = 35)
    private String bannerText;

    @Column(name = "cv_url", columnDefinition = "TEXT")
    private String cvUrl;

    @Column(name = "cv_file_name", length = 255)
    private String cvFileName;

    @Column(name = "cv_file_size", length = 50)
    private String cvFileSize;

    @Column(length = 120)
    private String headline;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Apartado academico
    @Column(name = "has_university_base")
    @Builder.Default
    private Boolean hasUniversityBase = false;

    @Column(name = "student_code", length = 50)
    private String studentCode;

    @Column(length = 150)
    private String career;

    @Column(name = "current_cycle")
    private Integer currentCycle;

    @OneToMany(mappedBy = "postulantProfile", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<StudentSkill> skills;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_profile_id")
    private InstitutionProfile institutionProfile;

    @Column(name = "is_institution_verified")
    private Boolean isInstitutionVerified;

    @Column(name = "is_verified")
    @Builder.Default
    private Boolean isVerified = false;

    @Column(name = "institutional_email", length = 150)
    private String institutionalEmail;

    @Column(name = "academic_status", length = 30)
    private String academicStatus;

    @Column(name = "academic_evidence_url", columnDefinition = "TEXT")
    private String academicEvidenceUrl;

    @Column(name = "academic_evidence_name", length = 255)
    private String academicEvidenceName;
}
