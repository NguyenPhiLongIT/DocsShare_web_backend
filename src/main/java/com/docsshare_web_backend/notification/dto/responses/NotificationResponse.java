package com.docsshare_web_backend.notification.dto.responses;

import com.docsshare_web_backend.notification.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private Long id;

    private String content;

    private NotificationType type;

    private boolean isRead;

    private String link;

    private Long targetId;

    private LocalDateTime createdAt;

    private String senderName;
    
    private Long userId;
}
