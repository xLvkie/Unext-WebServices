package com.nextworks.unextwebservices.dto.feed;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class FeedPostRequestDTO {
    @NotBlank(message = "El contenido de la publicación no puede estar vacío")
    private String content;

    // Lista opcional de URLs de fotos, videos o documentos adjuntos
    private List<String> mediaUrls;
}