package com.nextworks.unextwebservices.dto.feed;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class FeedPostResponseDTO {
    private UUID id;
    private UUID authorUserId;
    private UUID authorProfileId;
    private String authorRole;
    private String authorName;
    private String authorHeadline;
    private String authorAvatarUrl;
    private Boolean isAuthorVerified;
    private String content;
    private List<String> mediaUrls;
    private int likesCount;
    private long commentsCount;
    private boolean likedByMe;
    private LocalDateTime createdAt;
}