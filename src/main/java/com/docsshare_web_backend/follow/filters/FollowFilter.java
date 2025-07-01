package com.docsshare_web_backend.follow.filters;

import com.docsshare_web_backend.commons.filters.CommonFilter;
import com.docsshare_web_backend.follow.dto.requests.FollowFilterRequest;
import com.docsshare_web_backend.follow.models.Follow;
import org.springframework.data.jpa.domain.Specification;

public class FollowFilter {
    public static Specification<Follow> filterByRequest(FollowFilterRequest request) {
        return CommonFilter.filter(request, Follow.class);
    }
}
