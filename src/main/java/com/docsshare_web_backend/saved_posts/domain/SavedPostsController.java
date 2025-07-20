package com.docsshare_web_backend.saved_posts.domain;

import com.docsshare_web_backend.saved_documents.dto.requests.SavedDocumentsFilterRequest;
import com.docsshare_web_backend.saved_documents.dto.requests.SavedDocumentsRequest;
import com.docsshare_web_backend.saved_documents.dto.responses.SavedDocumentsResponse;
import com.docsshare_web_backend.saved_posts.dto.requests.SavedPostsFilterResquest;
import com.docsshare_web_backend.saved_posts.dto.requests.SavedPostsRequest;
import com.docsshare_web_backend.saved_posts.dto.responses.SavedPostsResponse;
import com.docsshare_web_backend.saved_posts.services.SavedPostsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/saved-posts")
public class SavedPostsController {
    @Autowired
    SavedPostsService savedPostsService;

    @GetMapping("/{userId}")
    public ResponseEntity<Page<SavedPostsResponse>> getSavedPostsByUserId(
            @PathVariable long userId,
            @ModelAttribute SavedPostsFilterResquest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(defaultValue = "desc") String sort){

        Sort sortOrder = sort.equals("asc") ? Sort.by("createdAt").ascending() : Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sortOrder);
        Page<SavedPostsResponse> savedPosts = savedPostsService.getSavedPostsByUserId(request, userId, pageable);
        return ResponseEntity.ok(savedPosts);
    }

    @PostMapping("/save")
    public ResponseEntity<SavedPostsResponse> savePost(@RequestBody SavedPostsRequest request) {
        return ResponseEntity.ok(savedPostsService.savePost(request));
    }

    @DeleteMapping("/unsave")
    public ResponseEntity<?> unsavePost(@RequestBody SavedPostsRequest request){
        savedPostsService.unsavePost(request);
        return ResponseEntity.ok(Map.of("message", "Forum post unsaved successfully"));
    }
}
