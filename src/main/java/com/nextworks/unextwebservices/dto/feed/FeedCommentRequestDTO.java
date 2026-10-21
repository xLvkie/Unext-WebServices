package com.nextworks.unextwebservices.dto.feed;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FeedCommentRequestDTO {
    @NotBlank(message = "El comentario no puede estar vacío")
    private String content;
}