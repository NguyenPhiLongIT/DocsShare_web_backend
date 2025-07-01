package com.docsshare_web_backend.forum_posts.dto.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForumPostRequest {
    private String title;
    @NotBlank(message = "Content should not be blank")
    @NotNull(message = "Content should not be null")
    private String content;
    private String file;
    private String type;
    private Boolean isPublic;
    private Long userId;
    private Long categoryId;
}
