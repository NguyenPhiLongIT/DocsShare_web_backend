package com.docsshare_web_backend.forum_posts.domain;

import com.docsshare_web_backend.documents.dto.requests.DocumentFilterRequest;
import com.docsshare_web_backend.documents.dto.requests.DocumentRequest;
import com.docsshare_web_backend.documents.dto.responses.DocumentResponse;
import com.docsshare_web_backend.forum_posts.dto.requests.ForumPostFilterRequest;
import com.docsshare_web_backend.forum_posts.dto.requests.ForumPostRequest;
import com.docsshare_web_backend.forum_posts.dto.responses.ForumPostResponse;
import com.docsshare_web_backend.forum_posts.services.ForumPostService;
import jakarta.persistence.EntityNotFoundException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/forum-posts")
public class ForumPostController {
    @Autowired
    private ForumPostService forumPostService;

    @GetMapping
    public ResponseEntity<Page<ForumPostResponse>> getAllForumPost(
            @ModelAttribute ForumPostFilterRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(defaultValue = "desc") String sort
            ){

        Sort sortPost = sort.equals("asc") ? Sort.by("createdAt").ascending() : Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sortPost);
        Page<ForumPostResponse> forumPost = forumPostService.getAllForumPosts(request, pageable);
        return ResponseEntity.ok(forumPost);
    }

    @GetMapping("/{forumPostId}")
    public ResponseEntity<ForumPostResponse> getForumPost(@PathVariable long forumPostId){
        return ResponseEntity.ok(forumPostService.getForumPostById(forumPostId));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<Page<ForumPostResponse>> getForumPostByCategoryId(
            @PathVariable long categoryId,
            @ModelAttribute ForumPostFilterRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(defaultValue = "desc") String sort
    ){
        Sort sortPost = sort.equals("asc") ? Sort.by("createdAt").ascending() : Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sortPost);
        Page<ForumPostResponse> forumPost = forumPostService.getForumPostByCategoryId(request, categoryId, pageable);
        return ResponseEntity.ok(forumPost);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<ForumPostResponse>> getForumPostByUserId(
            @PathVariable long userId,
            @ModelAttribute ForumPostFilterRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(defaultValue = "desc") String sort
    ){
        Sort sortPost = sort.equals("asc") ? Sort.by("createdAt").ascending() : Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sortPost);
        Page<ForumPostResponse> forumPost = forumPostService.getForumPostByUserId(request, userId, pageable);
        return ResponseEntity.ok(forumPost);
    }

    @GetMapping("/document/{documentId}")
    public ResponseEntity<Page<ForumPostResponse>> getForumPostByDocumentId(
            @PathVariable long documentId,
            @ModelAttribute ForumPostFilterRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(defaultValue = "desc") String sort
    ){
        Sort sortPost = sort.equals("asc") ? Sort.by("createdAt").ascending() : Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sortPost);
        Page<ForumPostResponse> forumPost = forumPostService.getForumPostByDocumentId(request, documentId, pageable);
        return ResponseEntity.ok(forumPost);
    }

    @PostMapping("/create")
    public ResponseEntity<ForumPostResponse> createForumPost(
            @RequestBody ForumPostRequest forumPostRequest
    ){
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(forumPostService.createForumPost(forumPostRequest));
    }

    @PutMapping("/update/{forumPostId}")
    public ResponseEntity<ForumPostResponse> updateForumPost(
            @PathVariable long forumPostId,
            @RequestBody ForumPostRequest request) {
        return ResponseEntity.ok(forumPostService.updateForumPost(forumPostId, request));
    }

    @DeleteMapping("/{forumPostId}")
    public ResponseEntity<?> deleteForumPost(@PathVariable("forumPostId") long id) {
        try {
            forumPostService.deleteForumPost(id);
            return ResponseEntity.ok().body(Map.of(
                    "message", "Xoá bài viết thành công",
                    "forumPostId", id
            ));
        } catch (EntityNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "message", "Không tìm thấy bài viết có ID: " + id
            ));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "message", "Xảy ra lỗi khi xoá bài viết"
            ));
        }
    }

    @GetMapping("/tags/by-category")
    public ResponseEntity<Set<String>> getTagsByCategory(@RequestParam Long categoryId) {
        return ResponseEntity.ok(forumPostService.getTagsByCategoryId(categoryId));
    }

    @GetMapping("/tags/by-document")
    public ResponseEntity<Set<String>> getTagsByDocument(@RequestParam Long documentId){
        return ResponseEntity.ok(forumPostService.getTagsByDocumentId(documentId));
    }

    @PostMapping("/incrementView/{forumPostId}")
    public ResponseEntity<ForumPostResponse> incrementView(@PathVariable long forumPostId) {
        log.debug("[ForumPostController] Increment view count for Forum post with id {}", forumPostId);
        return ResponseEntity.ok(forumPostService.incrementView(forumPostId));
    }

}
