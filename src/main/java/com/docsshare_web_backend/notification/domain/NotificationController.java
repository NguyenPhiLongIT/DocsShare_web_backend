package com.docsshare_web_backend.notification.domain;

import com.docsshare_web_backend.notification.dto.requests.NotificationFilterRequest;
import com.docsshare_web_backend.notification.dto.requests.NotificationRequest;
import com.docsshare_web_backend.notification.dto.requests.NotificationShareRequest;
import com.docsshare_web_backend.notification.dto.responses.NotificationResponse;
import com.docsshare_web_backend.notification.enums.NotificationType;
import com.docsshare_web_backend.notification.services.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/notification")
public class NotificationController {
    @Autowired
    private NotificationService notificationService;

    @PostMapping("/share")
    public ResponseEntity<?> shareContent(@RequestBody NotificationShareRequest request) {
        notificationService.shareContent(request);
        return ResponseEntity.ok().build();
    }
}
