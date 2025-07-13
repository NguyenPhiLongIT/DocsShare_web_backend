package com.docsshare_web_backend.saved_documents.filters;

import com.docsshare_web_backend.saved_documents.dto.requests.SavedDocumentsFilterRequest;
import com.docsshare_web_backend.saved_documents.models.SavedDocuments;
import com.docsshare_web_backend.commons.filters.CommonFilter;

import org.springframework.data.jpa.domain.Specification;

public class SavedDocumentsFilter {
    public static Specification<SavedDocuments> filterByRequest(SavedDocumentsFilterRequest request) {
        return CommonFilter.filter(request, SavedDocuments.class);
    }
}
