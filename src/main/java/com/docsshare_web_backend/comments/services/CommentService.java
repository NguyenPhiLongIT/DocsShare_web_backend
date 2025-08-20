package com.docsshare_web_backend.comments.services;

import com.docsshare_web_backend.comments.dto.requests.CommentFilterRequest;
import com.docsshare_web_backend.comments.dto.requests.CommentRequest;
import com.docsshare_web_backend.comments.dto.responses.CommentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public interface CommentService {
    Page<CommentResponse> getAllComments(CommentFilterRequest request, Pageable pageable);
    Page<CommentResponse> getCommentByForumPostId(long forumPostId, CommentFilterRequest request, Pageable pageable);
    Page<CommentResponse> getCommentByUserId(long userId, CommentFilterRequest request, Pageable pageable);
    CommentResponse getCommentById(long id);
    CommentResponse createComment(CommentRequest request);
    CommentResponse updateComment(long commentId, String newContent);
    void deleteComment(long commentId);


}
