package com.docsshare_web_backend.comments.dto.responses;

import com.docsshare_web_backend.account.dto.responses.AccountResponse;
import com.docsshare_web_backend.account.dto.responses.UserResponse;
import com.docsshare_web_backend.forum_posts.dto.responses.ForumPostResponse;
import com.docsshare_web_backend.forum_posts.models.ForumPost;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    private Long id;
    private String content;
    private String type;
    private Boolean isHiden;
    private LocalDateTime createdAt;
    private LocalDateTime updateAt;
    private UserResponse user;
    private ForumPostResponse forumPost;
}
