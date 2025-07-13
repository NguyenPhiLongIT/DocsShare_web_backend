package com.docsshare_web_backend.saved_posts.services;

import com.docsshare_web_backend.saved_documents.dto.requests.SavedDocumentsFilterRequest;
import com.docsshare_web_backend.saved_documents.dto.responses.SavedDocumentsResponse;
import com.docsshare_web_backend.saved_posts.dto.requests.SavedPostsFilterResquest;
import com.docsshare_web_backend.saved_posts.dto.requests.SavedPostsRequest;
import com.docsshare_web_backend.saved_posts.dto.responses.SavedPostsResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public interface SavedPostsService {
    Page<SavedPostsResponse> getSavedPostsByUserId(SavedPostsFilterResquest request, long userId, Pageable pageable);
    SavedPostsResponse savePost(SavedPostsRequest request);
    void unsavePost(SavedPostsRequest request);
}
