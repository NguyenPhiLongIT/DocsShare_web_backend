package com.docsshare_web_backend.saved_posts.dto.responses;

import com.docsshare_web_backend.account.dto.responses.UserResponse;
import com.docsshare_web_backend.forum_posts.dto.responses.ForumPostResponse;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class SavedPostsResponse {
    private Long id;
    private ForumPostResponse forumPost;
    private UserResponse user;
    private LocalDateTime savedAt;
}
