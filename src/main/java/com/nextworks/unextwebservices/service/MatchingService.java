package com.nextworks.unextwebservices.service;

import com.nextworks.unextwebservices.dto.job.JobOfferResponseDTO;
import com.nextworks.unextwebservices.dto.job.JobRecommendationResponseDTO;
import com.nextworks.unextwebservices.entity.*;
import com.nextworks.unextwebservices.entity.enums.CompatibilityLabel;
import com.nextworks.unextwebservices.repository.DismissedJobRecommendationRepository;
import com.nextworks.unextwebservices.repository.JobApplicationRepository;
import com.nextworks.unextwebservices.repository.JobOfferRepository;
import com.nextworks.unextwebservices.repository.JobRequiredSkillRepository;
import com.nextworks.unextwebservices.repository.PostulantProfileRepository;
import com.nextworks.unextwebservices.repository.StudentSkillRepository;
import com.nextworks.unextwebservices.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchingService {

    public static final int MATCH_THRESHOLD = 80;
    public static final String INCOMPLETE_PROFILE_MESSAGE =
            "Debes completar tu perfil técnico para recibir sugerencias personalizadas.";
    public static final String NO_CANDIDATES_MESSAGE =
            "No se encontraron candidatos que coincidan con estos criterios académicos";
    public static final String MISSING_JOB_SKILLS_HINT =
            "Especifica las habilidades requeridas de la vacante para calcular la compatibilidad.";

    private final JobRequiredSkillRepository jobRequiredSkillRepository;
    private final StudentSkillRepository studentSkillRepository;
    private final PostulantProfileRepository postulantRepository;
    private final JobOfferRepository jobOfferRepository;
    private final JobApplicationRepository applicationRepository;
    private final DismissedJobRecommendationRepository dismissedRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public List<String> normalizeSkillNames(List<String> rawNames) {
        if (rawNames == null) {
            return List.of();
        }
        LinkedHashMap<String, String> unique = new LinkedHashMap<>();
        for (String raw : rawNames) {
            if (raw == null) {
                continue;
            }
            String trimmed = raw.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            unique.putIfAbsent(trimmed.toLowerCase(Locale.ROOT), trimmed);
        }
        return new ArrayList<>(unique.values());
    }

    public void replaceRequiredSkills(JobOffer offer, List<String> rawNames) {
        List<String> names = normalizeSkillNames(rawNames);
        if (names.isEmpty()) {
            throw new RuntimeException("Debes agregar al menos una habilidad requerida");
        }
        if (offer.getRequiredSkills() == null) {
            offer.setRequiredSkills(new ArrayList<>());
        }
        offer.getRequiredSkills().clear();
        for (String name : names) {
            offer.getRequiredSkills().add(JobRequiredSkill.builder()
                    .jobOffer(offer)
                    .name(name)
                    .build());
        }
    }

    public List<String> requiredSkillNames(JobOffer offer) {
        if (offer.getRequiredSkills() == null) {
            return List.of();
        }
        return offer.getRequiredSkills().stream()
                .map(JobRequiredSkill::getName)
                .toList();
    }

    public List<String> studentSkillNames(PostulantProfile postulant) {
        List<StudentSkill> skills = postulant.getSkills();
        if (skills == null || skills.isEmpty()) {
            skills = studentSkillRepository.findByPostulantProfileId(postulant.getId());
        }
        return skills.stream().map(StudentSkill::getName).toList();
    }

    public MatchResult evaluate(List<String> studentSkills, List<String> requiredSkills) {
        List<String> required = normalizeSkillNames(requiredSkills);
        if (required.isEmpty()) {
            return new MatchResult(null, CompatibilityLabel.NA, List.of(), MISSING_JOB_SKILLS_HINT);
        }

        Set<String> studentKeys = normalizeSkillNames(studentSkills).stream()
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        List<String> matched = required.stream()
                .filter(name -> studentKeys.contains(name.toLowerCase(Locale.ROOT)))
                .toList();

        int percent = (int) Math.round((matched.size() * 100.0) / required.size());
        CompatibilityLabel label;
        if (percent >= MATCH_THRESHOLD) {
            label = CompatibilityLabel.ALTA;
        } else if (percent >= 50) {
            label = CompatibilityLabel.MEDIA;
        } else {
            label = CompatibilityLabel.BAJA;
        }
        return new MatchResult(percent, label, matched, null);
    }

    public JobOfferResponseDTO toOfferDto(JobOffer offer) {
        return JobOfferResponseDTO.builder()
                .id(offer.getId())
                .companyName(offer.getRecruiterProfile().getCompanyName())
                .title(offer.getTitle())
                .description(offer.getDescription())
                .requirements(offer.getRequirements())
                .location(offer.getLocation())
                .modality(offer.getModality())
                .experienceLevel(offer.getExperienceLevel())
                .minSalary(offer.getMinSalary())
                .maxSalary(offer.getMaxSalary())
                .requiredSkills(requiredSkillNames(offer))
                .createdAt(offer.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<String> suggestSkills(String query) {
        String term = query == null ? "" : query.trim();
        if (term.length() < 1) {
            return List.of();
        }
        TreeSet<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        names.addAll(studentSkillRepository.suggestNames(term));
        names.addAll(jobRequiredSkillRepository.suggestNames(term));
        return new ArrayList<>(names);
    }

    @Transactional
    public void notifyCompatiblePostulants(JobOffer offer) {
        List<String> required = requiredSkillNames(offer);
        if (required.isEmpty()) {
            return;
        }
        List<PostulantProfile> postulants = postulantRepository.findAll();
        for (PostulantProfile postulant : postulants) {
            List<String> skills = studentSkillNames(postulant);
            if (skills.isEmpty()) {
                continue;
            }
            MatchResult match = evaluate(skills, required);
            if (match.percent() != null && match.percent() >= MATCH_THRESHOLD) {
                String title = "Vacante ideal para tu perfil";
                String content = "La vacante '" + offer.getTitle() + "' de "
                        + offer.getRecruiterProfile().getCompanyName()
                        + " coincide en un " + match.percent()
                        + "% con tus habilidades. Puedes postular de inmediato.";
                notificationService.createNotification(postulant.getUser(), title, content);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<JobRecommendationResponseDTO> getRecommendations(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        PostulantProfile profile = postulantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil de postulante no encontrado"));

        List<String> skills = studentSkillNames(profile);
        if (skills.isEmpty()) {
            throw new RuntimeException(INCOMPLETE_PROFILE_MESSAGE);
        }

        Set<UUID> dismissed = dismissedRepository.findByPostulantProfileId(profile.getId()).stream()
                .map(item -> item.getJobOffer().getId())
                .collect(Collectors.toSet());

        return jobOfferRepository.findByIsActiveTrue().stream()
                .filter(offer -> !dismissed.contains(offer.getId()))
                .filter(offer -> !applicationRepository.existsByJobOfferIdAndPostulantProfileId(offer.getId(), profile.getId()))
                .map(offer -> {
                    MatchResult match = evaluate(skills, requiredSkillNames(offer));
                    return new OfferMatch(offer, match);
                })
                .filter(item -> item.match.percent() != null && item.match.percent() >= MATCH_THRESHOLD)
                .sorted(Comparator.comparingInt((OfferMatch item) -> item.match.percent()).reversed())
                .map(item -> JobRecommendationResponseDTO.builder()
                        .id(item.offer.getId())
                        .companyName(item.offer.getRecruiterProfile().getCompanyName())
                        .title(item.offer.getTitle())
                        .description(item.offer.getDescription())
                        .location(item.offer.getLocation())
                        .modality(item.offer.getModality())
                        .experienceLevel(item.offer.getExperienceLevel())
                        .minSalary(item.offer.getMinSalary())
                        .maxSalary(item.offer.getMaxSalary())
                        .requiredSkills(requiredSkillNames(item.offer))
                        .matchedSkills(item.match.matchedSkills())
                        .compatibilityPercent(item.match.percent())
                        .compatibilityLabel(item.match.label())
                        .createdAt(item.offer.getCreatedAt())
                        .build())
                .toList();
    }

    @Transactional
    public String dismissRecommendation(String email, UUID jobId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        PostulantProfile profile = postulantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil de postulante no encontrado"));
        JobOffer offer = jobOfferRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Vacante no encontrada"));

        if (dismissedRepository.existsByPostulantProfileIdAndJobOfferId(profile.getId(), offer.getId())) {
            return "Esta vacante ya no aparecerá en tus recomendaciones.";
        }

        dismissedRepository.save(DismissedJobRecommendation.builder()
                .postulantProfile(profile)
                .jobOffer(offer)
                .build());
        return "Dejamos de mostrar esta vacante en tus recomendaciones.";
    }

    public record MatchResult(
            Integer percent,
            CompatibilityLabel label,
            List<String> matchedSkills,
            String hint
    ) {
    }

    private record OfferMatch(JobOffer offer, MatchResult match) {
    }
}
