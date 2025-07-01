package com.docsshare_web_backend.forum_posts.filters;

import com.docsshare_web_backend.commons.filters.CommonFilter;
import com.docsshare_web_backend.forum_posts.dto.requests.ForumPostFilterRequest;
import com.docsshare_web_backend.forum_posts.models.ForumPost;
import org.springframework.data.jpa.domain.Specification;

public class ForumPostFilter {
    public static Specification<ForumPost> filterByRequest(ForumPostFilterRequest request){
        Specification<ForumPost> spec = CommonFilter.filter(request, ForumPost.class);

        if (request.getIsPublic() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("isPublic"), request.getIsPublic())
            );
        }

        return spec;
    }
}
