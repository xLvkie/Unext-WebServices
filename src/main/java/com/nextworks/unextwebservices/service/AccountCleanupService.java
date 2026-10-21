package com.nextworks.unextwebservices.service;

import com.nextworks.unextwebservices.entity.InstitutionProfile;
import com.nextworks.unextwebservices.entity.RecruiterProfile;
import com.nextworks.unextwebservices.entity.User;
import com.nextworks.unextwebservices.repository.InstitutionProfileRepository;
import com.nextworks.unextwebservices.repository.RecruiterProfileRepository;
import com.nextworks.unextwebservices.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountCleanupService {

    private final RecruiterProfileRepository recruiterRepository;
    private final InstitutionProfileRepository institutionRepository;
    private final UserRepository userRepository;

    // Se ejecuta automáticamente todos los días a las 3:00 AM
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void deleteUnverifiedExpiredOrganizations() {
        LocalDateTime limitDate = LocalDateTime.now().minusDays(7);

        // 1. Limpiar Reclutadores no verificados después de 7 días
        List<RecruiterProfile> expiredRecruiters =
                recruiterRepository.findByIsVerifiedFalseAndCreatedAtBefore(limitDate);
        for (RecruiterProfile recruiter : expiredRecruiters) {
            User user = recruiter.getUser();
            recruiterRepository.delete(recruiter);
            userRepository.delete(user);
        }

        // 2. Limpiar Instituciones no verificadas después de 7 días
        List<InstitutionProfile> expiredInstitutions =
                institutionRepository.findByIsVerifiedFalseAndCreatedAtBefore(limitDate);
        for (InstitutionProfile institution : expiredInstitutions) {
            User user = institution.getUser();
            institutionRepository.delete(institution);
            userRepository.delete(user);
        }
    }
}