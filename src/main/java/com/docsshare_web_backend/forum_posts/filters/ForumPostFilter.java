package com.docsshare_web_backend.forum_posts.filters;

import com.docsshare_web_backend.commons.filters.CommonFilter;
import com.docsshare_web_backend.forum_posts.dto.requests.ForumPostFilterRequest;
import com.docsshare_web_backend.forum_posts.models.ForumPost;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.List;

public class ForumPostFilter {
    public static Specification<ForumPost> filterByRequest(ForumPostFilterRequest request){
        Specification<ForumPost> spec = CommonFilter.filter(request, ForumPost.class);

        if (request.getIsPublic() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("isPublic"), request.getIsPublic())
            );
        }

        if (request.getQ() != null && !request.getQ().isBlank()) {
            String q = "%" + request.getQ().toLowerCase().trim() + "%";

            spec = spec.and((root, query, cb) -> {
                query.distinct(true); // rất quan trọng khi join

                Predicate titlePredicate = cb.like(cb.lower(root.get("title")), q);

                // join vào Set<String> – phải dùng joinSet, KHÔNG dùng as(String.class)
                Join<ForumPost, String> tagJoin = root.joinSet("tags", JoinType.LEFT);

                // dùng tagJoin trực tiếp vì mỗi phần tử trong Set đã là String
                Predicate tagPredicate = cb.like(cb.lower(tagJoin), q);

                return cb.or(titlePredicate, tagPredicate);
            });
        }

        if (request.getHasDocument() != null) {
            spec = spec.and((root, query, cb) -> {
                if (request.getHasDocument()) {
                    return cb.isNotNull(root.get("document"));
                } else {
                    return cb.isNull(root.get("document"));
                }
            });
        }

        return spec;
    }
}
