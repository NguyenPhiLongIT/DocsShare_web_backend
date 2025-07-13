package com.docsshare_web_backend.comments.services.impl;

import com.docsshare_web_backend.account.dto.responses.AccountResponse;
import com.docsshare_web_backend.account.dto.responses.UserResponse;
import com.docsshare_web_backend.comments.dto.requests.CommentFilterRequest;
import com.docsshare_web_backend.comments.dto.requests.CommentRequest;
import com.docsshare_web_backend.comments.dto.responses.CommentResponse;
import com.docsshare_web_backend.comments.enums.CommentType;
import com.docsshare_web_backend.comments.filters.CommentFilter;
import com.docsshare_web_backend.comments.models.Comment;
import com.docsshare_web_backend.comments.repositories.CommentRepository;
import com.docsshare_web_backend.comments.services.CommentService;
import com.docsshare_web_backend.commons.services.ToxicService;
import com.docsshare_web_backend.documents.filters.DocumentFilter;
import com.docsshare_web_backend.documents.models.Document;
import com.docsshare_web_backend.forum_posts.dto.responses.ForumPostResponse;
import com.docsshare_web_backend.forum_posts.filters.ForumPostFilter;
import com.docsshare_web_backend.forum_posts.models.ForumPost;
import com.docsshare_web_backend.forum_posts.repositories.ForumPostRepository;
import com.docsshare_web_backend.users.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
public class CommentServiceImpl implements CommentService {
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    ForumPostRepository forumPostRepository;
    @Autowired
    private ToxicService toxicService;

    private Pageable getPageable(Pageable pageable){
        return pageable != null ? pageable : Pageable.unpaged();
    }

    public static class CommentMapper{
        public static CommentResponse toCommentResponse(Comment comment){
            return CommentResponse.builder()
                    .id(comment.getId())
                    .content(comment.getContent())
                    .type(comment.getType() != null ? comment.getType().toString() : null)
                    .isHiden(comment.isHiden())
                    .createdAt(comment.getCreatedAt())
                    .updateAt(comment.getUpdateAt())
                    .user(UserResponse.builder()
                            .id(comment.getUser().getId())
                            .name(comment.getUser().getName())
                            .avatar(comment.getUser().getAvatar())
                            .build())
                    .forumPost(ForumPostResponse.builder()
                            .id(comment.getForumPost().getId())
                            .title(comment.getForumPost().getTitle())
                            .content(comment.getForumPost().getContent())
                            .filePath(comment.getForumPost().getFilePath())
                            .isPublic(comment.getForumPost().getIsPublic() != null ? comment.getForumPost().getIsPublic().toString() : null)
//                            .user(comment.getForumPost().getUser() != null ? comment.getForumPost().getUser().getName() : "")
                            .user(UserResponse.builder()
                                    .id(comment.getForumPost().getUser().getId())
                                    .name(comment.getForumPost().getUser().getName())
                                    .avatar(comment.getForumPost().getUser().getAvatar())
                                    .build())
                            .category(comment.getForumPost().getCategory() != null ? comment.getForumPost().getCategory().getName() : "")
                            .createdAt(comment.getForumPost().getCreatedAt())
                            .updateAt(comment.getForumPost().getUpdateAt())
                            .build())
                    .build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommentResponse> getCommentByForumPostId(long forumPostId, CommentFilterRequest request, Pageable pageable) {
        forumPostRepository.findById(forumPostId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Forum post not found with id: " + forumPostId));

        Specification<Comment> spec = Specification
                .<Comment>where((root, query, cb) -> cb.equal(root.get("forumPost").get("id"),
                        forumPostId))
                .and(CommentFilter.filterByRequest(request));

        Page<Comment> comments = commentRepository.findAll(spec, getPageable(pageable));

        return comments.map(CommentMapper::toCommentResponse);
    }

    @Override
    public Page<CommentResponse> getCommentByUserId(long userId, CommentFilterRequest request, Pageable pageable) {
        commentRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "User not found with id: " + userId));
        Specification<Comment> spec = Specification
                .<Comment>where((root, query, cb) -> cb.equal(root.get("user").get("id"),
                        userId))
                .and(CommentFilter.filterByRequest(request));

        Page<Comment> comments = commentRepository.findAll(spec, getPageable(pageable));

        return comments.map(CommentMapper::toCommentResponse);
    }


    @Override
    public CommentResponse getCommentById(long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(()->new EntityNotFoundException("Comment post not found with id: " + id));
        return CommentMapper.toCommentResponse(comment);
    }

    @Override
    @Transactional
    public CommentResponse createComment(CommentRequest request) {
        var user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "User not found with id: " + request.getUserId()));

        var forumPost = forumPostRepository.findById(request.getForumPostId())
                .orElseThrow(
                        () -> new EntityNotFoundException("Forum post not found with id: "
                                + request.getForumPostId()));

        toxicService.validateTextSafety(request.getContent(), "Contend");

        CommentType type = request.getType() != null
                ? CommentType.valueOf(request.getType())
                : CommentType.NORMAL;

        boolean isHidden = CommentType.REPORT.equals(type);


        Comment comment = Comment.builder()
                .content(request.getContent())
                .type(type)
                .user(user)
                .isHiden(isHidden)
                .forumPost(forumPost)
                .build();
        Comment savedComment = commentRepository.save(comment);
        return CommentMapper.toCommentResponse(savedComment);
    }

    @Override
    public CommentResponse updateComment(long commentId, String newContent) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Comment not found with id: " + commentId));

        toxicService.validateTextSafety(newContent, "Contend");
        comment.setContent(newContent);
        comment.setUpdateAt(LocalDateTime.now());

        Comment updated = commentRepository.save(comment);
        return CommentMapper.toCommentResponse(updated);
    }

    @Override
    public void deleteComment(long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Comment not found with id: " + commentId));
        commentRepository.delete(comment);

    }
}
