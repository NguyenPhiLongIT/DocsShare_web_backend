package com.docsshare_web_backend.comments.domain;
import com.docsshare_web_backend.comments.dto.requests.CommentFilterRequest;
import com.docsshare_web_backend.comments.dto.requests.CommentRequest;
import com.docsshare_web_backend.comments.dto.requests.UpdateCommentRequest;
import com.docsshare_web_backend.comments.dto.responses.CommentResponse;
import com.docsshare_web_backend.comments.services.CommentService;
import jakarta.persistence.EntityNotFoundException;
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

@Slf4j
@RestController
@RequestMapping("/api/comments")
public class CommentController {
    @Autowired
    private CommentService commentService;

    @GetMapping("/{id}")
    public ResponseEntity<CommentResponse> getCommentById(@PathVariable long id){
        return ResponseEntity.ok(commentService.getCommentById(id));
    }
    @GetMapping("/forum-post/{forumPostId}")
    public ResponseEntity<Page<CommentResponse>> getCommentByForumPostId(
            @PathVariable long forumPostId,
            @ModelAttribute CommentFilterRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(defaultValue = "desc") String sort
    ){

        Sort sortPost = sort.equals("asc") ? Sort.by("createdAt").ascending() : Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sortPost);
        Page<CommentResponse> comment = commentService.getCommentByForumPostId(forumPostId, request, pageable);
        return ResponseEntity.ok(comment);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<CommentResponse>> getCommentByUserId(
            @PathVariable long userId,
            @ModelAttribute CommentFilterRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(defaultValue = "desc") String sort
    ){

        Sort sortPost = sort.equals("asc") ? Sort.by("createdAt").ascending() : Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sortPost);
        Page<CommentResponse> comment = commentService.getCommentByUserId(userId, request, pageable);
        return ResponseEntity.ok(comment);
    }

    @PostMapping("/create")
    public ResponseEntity<CommentResponse> createComment(@RequestBody CommentRequest commentRequest){
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.createComment(commentRequest));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommentResponse> updateCommentContent(
            @PathVariable("id") Long id,
            @RequestBody UpdateCommentRequest request) {
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        CommentResponse response = commentService.updateComment(id, request.getContent());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteComment(@PathVariable("id") long id) {
        try {
            commentService.deleteComment(id);
            return ResponseEntity.ok().body(Map.of(
                    "message", "Xoá bình luận thành công",
                    "commentId", id
            ));
        } catch (EntityNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "message", "Không tìm thấy bình luận có ID: " + id
            ));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "message", "Xảy ra lỗi khi xoá bình luận"
            ));
        }
    }
}
