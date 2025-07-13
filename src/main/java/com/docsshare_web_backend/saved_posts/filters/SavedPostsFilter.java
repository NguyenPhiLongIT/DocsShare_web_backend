package com.docsshare_web_backend.saved_posts.filters;

import com.docsshare_web_backend.commons.filters.CommonFilter;
import com.docsshare_web_backend.saved_posts.dto.requests.SavedPostsFilterResquest;
import com.docsshare_web_backend.saved_posts.models.SavedPosts;
import org.springframework.data.jpa.domain.Specification;

public class SavedPostsFilter {
    public static Specification<SavedPosts> filterByRequest(SavedPostsFilterResquest request){
        return CommonFilter.filter(request, SavedPosts.class);
    }
}
