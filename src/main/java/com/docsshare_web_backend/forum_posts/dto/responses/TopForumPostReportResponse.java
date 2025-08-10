package com.docsshare_web_backend.forum_posts.dto.responses;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class TopForumPostReportResponse {
    private Long postId;
    private String title;
    private Long viewCount;
    private Long savedCount;
    private Long commentsCount;
    private Long totalInteraction;
    private String authorName;
    private LocalDateTime createdAt;
    private String category;
    private String linkDocument;
}
