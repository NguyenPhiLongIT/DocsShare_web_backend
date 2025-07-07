package com.docsshare_web_backend.forum_posts.dto.responses;

import com.docsshare_web_backend.account.dto.responses.AccountResponse;
import com.docsshare_web_backend.account.dto.responses.UserResponse;
import com.docsshare_web_backend.forum_posts.enums.ForumPostStatus;
import com.docsshare_web_backend.forum_posts.enums.ForumPostType;
import com.docsshare_web_backend.users.models.User;
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
    private UserResponse user;
    private String category;
    private LocalDateTime createdAt;
    private LocalDateTime updateAt;
}
