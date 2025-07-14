package com.docsshare_web_backend.saved_posts.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.docsshare_web_backend.saved_posts.dto.responses.SavePostsCountProjection;
import com.docsshare_web_backend.saved_posts.models.SavedPosts;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedPostsRepository
        extends JpaRepository<SavedPosts, Long>, JpaSpecificationExecutor<SavedPosts> {

    Optional<SavedPosts> findByUserIdAndForumPostId(Long userId, Long forumPostId);
    boolean existsByUserIdAndForumPostId(Long userId, Long forumPostId);

    @Query("""
        SELECT sd.forumPost.id AS forumPostId, COUNT(sd) AS saveCount
        FROM SavedPosts sd
        WHERE sd.forumPost.id IN :forumPostIds
        GROUP BY sd.forumPost.id
    """)
    List<SavePostsCountProjection> countSavesByForumPostIds(@Param("forumPostIds") List<Long> forumPostIds);

    @Query("SELECT sd.forumPost.id FROM SavedPosts sd WHERE sd.user.id = :userId AND sd.forumPost.id IN :forumPostIds")
    List<Long> findSavedPostIdsByUserId(@Param("userId") Long userId, @Param("forumPostIds") List<Long> forumPostIds);

}
