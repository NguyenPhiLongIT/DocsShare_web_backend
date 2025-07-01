package com.docsshare_web_backend.comments.repositories;

import com.docsshare_web_backend.comments.dto.responses.CommentResponse;
import com.docsshare_web_backend.comments.models.Comment;
import com.docsshare_web_backend.forum_posts.models.ForumPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CommentRepository extends JpaRepository<Comment, Long>, JpaSpecificationExecutor<Comment> {
    Page<Comment> findByForumPostId(long forumPostId, Pageable pageable);
    Page<Comment> findByUserId(long userId, Pageable pageable);
}
