package com.docsshare_web_backend.notification.filters;

import com.docsshare_web_backend.commons.filters.CommonFilter;
import com.docsshare_web_backend.notification.dto.requests.NotificationFilterRequest;
import com.docsshare_web_backend.notification.models.Notification;
import org.springframework.data.jpa.domain.Specification;

public class NotificationFilter {
    public static Specification<Notification> filterByRequest(NotificationFilterRequest request) {
        return CommonFilter.filter(request, Notification.class);
    }
}
