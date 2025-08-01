package com.docsshare_web_backend.notification.services;

import com.docsshare_web_backend.notification.dto.requests.NotificationFilterRequest;
import com.docsshare_web_backend.notification.dto.requests.NotificationRequest;
import com.docsshare_web_backend.notification.dto.requests.NotificationShareRequest;
import com.docsshare_web_backend.notification.dto.responses.NotificationResponse;
import com.docsshare_web_backend.notification.enums.NotificationType;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public interface NotificationService {
    void shareContent(NotificationShareRequest request);
}
