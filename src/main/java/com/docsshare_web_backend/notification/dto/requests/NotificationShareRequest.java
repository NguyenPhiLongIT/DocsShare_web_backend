package com.docsshare_web_backend.notification.dto.requests;

import java.util.List;

import com.docsshare_web_backend.notification.enums.NotificationType;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationShareRequest {
    private Long senderId;
    private List<Long> receiverIds;
    private NotificationType type;
    private Long targetId;
}
