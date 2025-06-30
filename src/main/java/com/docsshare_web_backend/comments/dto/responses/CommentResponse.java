package com.docsshare_web_backend.comments.dto.responses;

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
    private int id;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updateAt;
    private String userName;
    private ForumPostResponse forumPost;
}
