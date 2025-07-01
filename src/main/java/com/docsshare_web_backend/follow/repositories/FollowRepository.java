package com.docsshare_web_backend.follow.repositories;

import com.docsshare_web_backend.follow.models.Follow;
import com.docsshare_web_backend.users.models.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long>, JpaSpecificationExecutor<Follow> {
    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);
    Optional<Follow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);
    List<Follow> findByFollowingId(Long followingId);
    List<Follow> findByFollowerId(Long followerId);
    long countByFollowerId(Long followerId);
    long countByFollowingId(Long followingId);


}