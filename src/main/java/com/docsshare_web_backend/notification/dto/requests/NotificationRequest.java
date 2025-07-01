package com.docsshare_web_backend.notification.dto.requests;

import com.docsshare_web_backend.notification.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {

    @NotBlank(message = "Content should not be blank")
    @NotNull(message = "Content should not be null")
    private String content;

    @NotNull(message = "Notification type must be provided")
    private NotificationType type;

    private boolean isRead = false;

    @NotNull(message = "User ID must be provided")
    private Long userId;
}
