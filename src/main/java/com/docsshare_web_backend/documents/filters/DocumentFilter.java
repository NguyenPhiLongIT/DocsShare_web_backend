package com.docsshare_web_backend.documents.filters;

import com.docsshare_web_backend.documents.dto.requests.DocumentFilterRequest;
import com.docsshare_web_backend.documents.models.Document;
import com.docsshare_web_backend.commons.filters.CommonFilter;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

public class DocumentFilter {
    public static Specification<Document> filterByRequest(DocumentFilterRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Gọi filter chung từ CommonFilter
            Specification<Document> baseSpec = CommonFilter.filter(request, Document.class);
            Predicate basePredicate = baseSpec.toPredicate(root, query, criteriaBuilder);
            if (basePredicate != null) predicates.add(basePredicate);

            // Custom: Lọc theo price (free = 0.0, paid > 0.0)
            if (request.getPrice() != null) {
                if (request.getPrice() == 0.0) {
                    predicates.add(criteriaBuilder.equal(root.get("price"), 0.0));
                } else if (request.getPrice() == 1.0) {
                    predicates.add(criteriaBuilder.greaterThan(root.get("price"), 0.0));
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
