package com.docsshare_web_backend.account.filters;

import com.docsshare_web_backend.account.dto.requests.AccountFilterRequest;
import com.docsshare_web_backend.users.models.User;
import com.docsshare_web_backend.commons.filters.CommonFilter;
import org.springframework.data.jpa.domain.Specification;

public class AccountFilter {
    public static Specification<User> filterByRequest(AccountFilterRequest request) {
        return CommonFilter.filter(request, User.class);
    }
}
