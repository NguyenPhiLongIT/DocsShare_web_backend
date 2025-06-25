package com.docsshare_web_backend.documents.filters;

import com.docsshare_web_backend.documents.dto.requests.DocumentFilterRequest;
import com.docsshare_web_backend.documents.models.Document;
import com.docsshare_web_backend.commons.filters.CommonFilter;
import org.springframework.data.jpa.domain.Specification;

public class DocumentFilter {
    public static Specification<Document> filterByRequest(DocumentFilterRequest request) {
        return CommonFilter.filter(request, Document.class);
    }
}
