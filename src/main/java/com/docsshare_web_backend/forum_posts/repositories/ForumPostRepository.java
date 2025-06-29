package com.docsshare_web_backend.forum_posts.repositories;

import com.docsshare_web_backend.forum_posts.models.ForumPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ForumPostRepository extends JpaRepository<ForumPost, Long>, JpaSpecificationExecutor<ForumPost>{

}
