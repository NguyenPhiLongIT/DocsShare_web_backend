package com.docsshare_web_backend.forum_posts.repositories;

import com.docsshare_web_backend.forum_posts.models.ForumPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface ForumPostRepository extends JpaRepository<ForumPost, Long>, JpaSpecificationExecutor<ForumPost>{
    @Query("SELECT DISTINCT t FROM ForumPost fp JOIN fp.tags t WHERE fp.category.id = :categoryId")
    Set<String> findDistinctTagsByCategoryId(@Param("categoryId") Long categoryId);

    @Query("SELECT DISTINCT t FROM ForumPost f JOIN f.tags t WHERE f.document.id = :documentId")
    Set<String> findDistinctTagsByDocumentId(@Param("documentId") Long documentId);
}
