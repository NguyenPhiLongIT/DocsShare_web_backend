package com.docsshare_web_backend.account.filters;

import com.docsshare_web_backend.account.dto.requests.AccountFilterRequest;
import com.docsshare_web_backend.users.models.User;
import com.docsshare_web_backend.commons.filters.CommonFilter;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

public class AccountFilter {
    public static Specification<User> filterByRequest(AccountFilterRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (request.getQ() != null && !request.getQ().trim().isEmpty()) {
                String pattern = "%" + request.getQ().trim().toLowerCase() + "%";
                predicates.add(
                        cb.or(
                                cb.like(cb.lower(root.get("name")), pattern),
                                cb.like(cb.lower(root.get("email")), pattern)
                        )
                );
            }

            Specification<User> commonSpec = CommonFilter.filter(request, User.class);
            Predicate commonPredicate = commonSpec.toPredicate(root, query, cb);
            if (commonPredicate != null) {
                predicates.add(commonPredicate);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
