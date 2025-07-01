package com.docsshare_web_backend.follow.dto.responses;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowResponse { // ID của bản ghi follow
    private Long followerId;    // Người theo dõi
    private String followerName;
    private Long followingId;   // Người được theo dõi
    private String followingName;
    private LocalDateTime createdAt;
}
