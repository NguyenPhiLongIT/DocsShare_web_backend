package com.docsshare_web_backend.forum_posts.repositories;

import com.docsshare_web_backend.forum_posts.models.ForumPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface ForumPostRepository extends JpaRepository<ForumPost, Long>, JpaSpecificationExecutor<ForumPost>{
    @Query("SELECT DISTINCT t FROM ForumPost fp JOIN fp.tags t WHERE fp.category.id = :categoryId")
    Set<String> findDistinctTagsByCategoryId(@Param("categoryId") Long categoryId);

    @Query("SELECT DISTINCT t FROM ForumPost f JOIN f.tags t WHERE f.document.id = :documentId")
    Set<String> findDistinctTagsByDocumentId(@Param("documentId") Long documentId);

//    @Query("""
//    SELECT fp.id,
//           fp.title,
//           fp.views,
//           COUNT(DISTINCT sp.id) as savedCount,
//           COUNT(DISTINCT c.id) as commentCount,
//           (fp.views + COUNT(DISTINCT sp.id) + COUNT(DISTINCT c.id)) as totalInteraction,
//           u.name,
//           fp.createdAt,
//           c2.name,
//           fp.document.id
//    FROM ForumPost fp
//    JOIN fp.user u
//    LEFT JOIN fp.category c2
//    LEFT JOIN fp.savedPosts sp
//    LEFT JOIN fp.comments c
//    WHERE fp.createdAt BETWEEN :fromDateTime AND :toDateTime
//    GROUP BY fp.id, fp.title, fp.views, u.name, fp.createdAt, c2.name, fp.document.id
//    ORDER BY totalInteraction DESC
//""")
//    List<Object[]> findTopForumPostsBetweenDates(
//            @Param("fromDateTime") LocalDateTime fromDateTime,
//            @Param("toDateTime") LocalDateTime toDateTime,
//            Pageable pageable);

    @Query("""
    SELECT fp.id,
           fp.title,
           fp.views,
           COUNT(DISTINCT sp.id),
           COUNT(DISTINCT c.id),
           (fp.views + COUNT(DISTINCT sp.id) + COUNT(DISTINCT c.id)),
           u.name,
           fp.createdAt,
           c2,
           d
    FROM ForumPost fp
    JOIN fp.user u
    LEFT JOIN fp.category c2
    LEFT JOIN fp.savedPosts sp
    LEFT JOIN fp.comments c
    LEFT JOIN fp.document d
    WHERE fp.createdAt BETWEEN :fromDateTime AND :toDateTime
    GROUP BY fp.id, fp.title, fp.views, u.name, fp.createdAt, c2, d
    ORDER BY (fp.views + COUNT(DISTINCT sp.id) + COUNT(DISTINCT c.id)) DESC
""")
    List<Object[]> findTopForumPostsBetweenDates(
            @Param("fromDateTime") LocalDateTime fromDateTime,
            @Param("toDateTime") LocalDateTime toDateTime,
            Pageable pageable);



}
