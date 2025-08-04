package com.docsshare_web_backend.notification.dto.requests;

import com.docsshare_web_backend.notification.enums.NotificationType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class NotificationFilterRequest {

    private String q; // Tìm kiếm theo tên

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate createdAt_from;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate createdAt_to;

    private Boolean isRead;
    private NotificationType type;
}
