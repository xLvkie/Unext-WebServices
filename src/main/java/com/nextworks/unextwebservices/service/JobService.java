package com.nextworks.unextwebservices.service;

import com.nextworks.unextwebservices.dto.ApplicationResponseDTO;
import com.nextworks.unextwebservices.dto.JobOfferResponseDTO;
import com.nextworks.unextwebservices.entity.*;
import com.nextworks.unextwebservices.repository.JobApplicationRepository;
import com.nextworks.unextwebservices.repository.JobOfferRepository;
import com.nextworks.unextwebservices.repository.PostulantProfileRepository;
import com.nextworks.unextwebservices.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobOfferRepository jobOfferRepository;
    private final JobApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final PostulantProfileRepository postulantRepository;

    @Transactional(readOnly = true)
    public List<JobOfferResponseDTO> searchJobs(String keyword, JobModality modality, ExperienceLevel experience) {
        String searchKeyword = (keyword == null) ? "" : keyword;
        List<JobOffer> offers = jobOfferRepository.searchActiveOffers(searchKeyword, modality, experience);

        return offers.stream()
                .map(offer -> JobOfferResponseDTO.builder()
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
                        .createdAt(offer.getCreatedAt())
                        .build())
                .toList();
    }

    @Transactional
    public ApplicationResponseDTO applyToJob(String email, UUID jobId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        PostulantProfile profile = postulantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil no encontrado"));

        JobOffer offer = jobOfferRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Vacante no encontrada"));

        if (!offer.getIsActive()) {
            throw new RuntimeException("Esta vacante ya no acepta postulaciones");
        }
        
        if (applicationRepository.existsByJobOfferIdAndPostulantProfileId(offer.getId(), profile.getId())) {
            throw new RuntimeException("Ya has postulado a esta vacante anteriormente");
        }

        JobApplication application = JobApplication.builder()
                .jobOffer(offer)
                .postulantProfile(profile)
                .build();

        applicationRepository.save(application);

        return ApplicationResponseDTO.builder()
                .id(application.getId())
                .jobTitle(offer.getTitle())
                .companyName(offer.getRecruiterProfile().getCompanyName())
                .status(application.getStatus())
                .appliedAt(application.getCreatedAt())
                .build();
    }
}