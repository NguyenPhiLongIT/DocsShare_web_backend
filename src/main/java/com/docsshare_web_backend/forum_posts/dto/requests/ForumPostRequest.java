package com.docsshare_web_backend.forum_posts.dto.requests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForumPostRequest {
    @NotBlank(message = "Title should not be blank")
    @NotNull(message = "Title should not be null")
    private String title;
    @NotBlank(message = "Content should not be blank")
    @NotNull(message = "Content should not be null")
    private String content;
//    @JsonIgnore
    private String filePath;
    private String type;
    private Boolean isPublic;
    private Long userId;
    private Long categoryId;
}
