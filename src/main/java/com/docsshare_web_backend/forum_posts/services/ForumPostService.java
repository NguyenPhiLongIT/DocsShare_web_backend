package com.docsshare_web_backend.forum_posts.services;

import com.docsshare_web_backend.forum_posts.dto.requests.ForumPostFilterRequest;
import com.docsshare_web_backend.forum_posts.dto.requests.ForumPostRequest;
import com.docsshare_web_backend.forum_posts.dto.responses.ForumPostResponse;
import com.docsshare_web_backend.forum_posts.dto.responses.TopForumPostReportResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
public interface ForumPostService {
    Page<ForumPostResponse> getAllForumPosts(ForumPostFilterRequest request, Pageable pageable);
    ForumPostResponse getForumPostById(long id);
    Page<ForumPostResponse> getForumPostByUserId(ForumPostFilterRequest request, long userId, Pageable pageable);
    Page<ForumPostResponse> getForumPostByDocumentId(ForumPostFilterRequest request, long documentId, Pageable pageable);
    Page<ForumPostResponse> getForumPostByCategoryId(ForumPostFilterRequest request, long categoryId, Pageable pageable);
    ForumPostResponse createForumPost(ForumPostRequest request);
    ForumPostResponse updateForumPost(long forumPostId, ForumPostRequest forumPostRequest);
    void deleteForumPost(long forumPostId);
    Set<String> getTagsByCategoryId(Long categoryId);
    Set<String> getTagsByDocumentId(Long documentId);
    ForumPostResponse incrementView(long forumPostId);
    List<TopForumPostReportResponse> getTopForumPostsBetween(LocalDate from, LocalDate to, int top);

    

}
