package com.docsshare_web_backend.comments.filters;

import com.docsshare_web_backend.comments.dto.requests.CommentFilterRequest;
import com.docsshare_web_backend.comments.models.Comment;
import com.docsshare_web_backend.commons.filters.CommonFilter;
import com.docsshare_web_backend.documents.dto.requests.DocumentFilterRequest;
import com.docsshare_web_backend.documents.models.Document;
import org.springframework.data.jpa.domain.Specification;

public class CommentFilter {
    public static Specification<Comment> filterByRequest(CommentFilterRequest request) {
        return CommonFilter.filter(request, Comment.class);
        
    }
}
