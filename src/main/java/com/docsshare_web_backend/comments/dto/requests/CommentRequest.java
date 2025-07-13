package com.docsshare_web_backend.comments.dto.requests;

import com.docsshare_web_backend.comments.enums.CommentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentRequest {
    @NotBlank(message = "Content should not be blank")
    @NotNull(message = "Content should not be null")
    private String content;
    private String type;
    private Long userId;
    private Long forumPostId;

}
