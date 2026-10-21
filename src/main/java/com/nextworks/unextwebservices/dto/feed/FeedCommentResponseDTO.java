package com.nextworks.unextwebservices.dto.feed;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class FeedCommentResponseDTO {
    private UUID id;
    private UUID postId;
    private UUID authorUserId;
    private UUID authorProfileId;
    private String authorRole;
    private String authorName;
    private String authorHeadline;
    private String authorAvatarUrl;
    private Boolean isAuthorVerified;
    private String content;
    private LocalDateTime createdAt;
}