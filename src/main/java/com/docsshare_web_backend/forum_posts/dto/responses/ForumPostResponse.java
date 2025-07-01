package com.docsshare_web_backend.forum_posts.dto.responses;

import com.docsshare_web_backend.forum_posts.enums.ForumPostStatus;
import com.docsshare_web_backend.forum_posts.enums.ForumPostType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForumPostResponse {
    private int id;
    private String title;
    private String content;
    private String file;
    private String type;
    private String isPublic;
    private String user;
    private String category;
    private LocalDateTime createdAt;
    private LocalDateTime updateAt;
}
