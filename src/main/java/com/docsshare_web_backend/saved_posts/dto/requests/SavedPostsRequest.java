package com.docsshare_web_backend.saved_posts.dto.requests;

import com.docsshare_web_backend.forum_posts.models.ForumPost;
import lombok.*;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedPostsRequest {
    private Long forumPostId;
    private Long userId;
}
