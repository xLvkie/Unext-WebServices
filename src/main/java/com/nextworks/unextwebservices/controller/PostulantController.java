package com.nextworks.unextwebservices.controller;

import com.nextworks.unextwebservices.entity.User;
import com.nextworks.unextwebservices.service.PostulantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/postulants")
@RequiredArgsConstructor
public class PostulantController {

    private final PostulantService postulantService;

    @PatchMapping("/me/institution/{institutionId}")
    public ResponseEntity<String> linkInstitution(
            Authentication authentication,
            @PathVariable UUID institutionId) {

        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(postulantService.linkInstitution(user.getEmail(), institutionId));
        /*
        Envia a revisión el estado de su asociación con la institución deseada
        El user requiere uso del token
         */
    }
}
