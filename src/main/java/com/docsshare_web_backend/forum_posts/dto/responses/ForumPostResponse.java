package com.docsshare_web_backend.forum_posts.dto.responses;
import com.docsshare_web_backend.account.dto.responses.UserResponse;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForumPostResponse {
    private Long id;
    private String title;
    private String content;
    private String filePath;
    private Set<String> tags;
    private Integer reads;
    private String isPublic;
    private UserResponse user;
    private String category;
    private LocalDateTime createdAt;
    private LocalDateTime updateAt;
}
