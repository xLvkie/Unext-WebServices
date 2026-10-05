package com.nextworks.unextwebservices.service;

import com.nextworks.unextwebservices.entity.InstitutionProfile;
import com.nextworks.unextwebservices.entity.PostulantProfile;
import com.nextworks.unextwebservices.entity.User;
import com.nextworks.unextwebservices.repository.InstitutionProfileRepository;
import com.nextworks.unextwebservices.repository.PostulantProfileRepository;
import com.nextworks.unextwebservices.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PostulantService {

    private final UserRepository userRepository;
    private final PostulantProfileRepository postulantRepository;
    private final InstitutionProfileRepository institutionRepository;

    @Transactional
    public String linkInstitution(String email, UUID institutionId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        PostulantProfile postulant = postulantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil de postulante no encontrado"));

        InstitutionProfile institution = institutionRepository.findById(institutionId)
                .orElseThrow(() -> new RuntimeException("Institución no encontrada"));

        // Vinculamos al estudiante y lo marcamos como NO verificado
        postulant.setInstitutionProfile(institution);
        postulant.setIsInstitutionVerified(false);
        postulantRepository.save(postulant);

        return "Solicitud de vinculación enviada exitosamente a la institución.";
    }
}