package com.docsshare_web_backend.forum_posts.services.impl;

import com.docsshare_web_backend.account.dto.responses.UserResponse;
import com.docsshare_web_backend.categories.models.Category;
import com.docsshare_web_backend.categories.repositories.CategoryRepository;
import com.docsshare_web_backend.forum_posts.dto.requests.ForumPostFilterRequest;
import com.docsshare_web_backend.forum_posts.dto.requests.ForumPostRequest;
import com.docsshare_web_backend.forum_posts.dto.responses.ForumPostResponse;
import com.docsshare_web_backend.forum_posts.filters.ForumPostFilter;
import com.docsshare_web_backend.forum_posts.models.ForumPost;
import com.docsshare_web_backend.forum_posts.repositories.ForumPostRepository;
import com.docsshare_web_backend.forum_posts.services.ForumPostService;
import com.docsshare_web_backend.users.repositories.UserRepository;
import com.docsshare_web_backend.commons.services.ToxicService;
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
public class ForumPostServiceImpl implements ForumPostService {
    @Autowired
    private ForumPostRepository forumPostRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ToxicService toxicService;

    private Pageable getPageable(Pageable pageable){
        return pageable != null ? pageable : Pageable.unpaged();
    }



    public static class ForumPostMapper {
        public static ForumPostResponse toForumPostResponse(ForumPost forumPost) {
            return ForumPostResponse.builder()
                    .id(forumPost.getId())
                    .title(forumPost.getTitle())
                    .content(forumPost.getContent())
                    .filePath(forumPost.getFilePath())
                    .isPublic(forumPost.getIsPublic() != null ? forumPost.getIsPublic().toString() : null)
                    .category(forumPost.getCategory() != null ? forumPost.getCategory().getName() : "")
                    .createdAt(forumPost.getCreatedAt())
                    .updateAt(forumPost.getUpdateAt())
                    .user(UserResponse.builder()
                            .id(forumPost.getUser().getId())
                            .name(forumPost.getUser().getName())
                            .avatar(forumPost.getUser().getAvatar())
                            .college(forumPost.getUser().getCollege())
                            .build())
                    .build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ForumPostResponse> getAllForumPosts(ForumPostFilterRequest request, Pageable pageable) {
        Specification<ForumPost> spec = ForumPostFilter.filterByRequest(request);
        return forumPostRepository.findAll(spec, getPageable(pageable)).map(ForumPostMapper::toForumPostResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ForumPostResponse getForumPostById(long id) {
        ForumPost forumPost = forumPostRepository.findById(id)
                .orElseThrow(()->new EntityNotFoundException("Forum post not found with id: " + id));
        return ForumPostMapper.toForumPostResponse(forumPost);
    }

    @Override
    public Page<ForumPostResponse> getForumPostByUserId(ForumPostFilterRequest request, long userId, Pageable pageable) {
        userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                "User not found with id: " + userId));
        Specification<ForumPost> spec = Specification
                .<ForumPost>where((root, query, cb) -> cb.equal(root.get("user").get("id"), userId))
                .and(ForumPostFilter.filterByRequest(request));
        return forumPostRepository.findAll(spec, pageable).map(ForumPostMapper::toForumPostResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ForumPostResponse> getForumPostByCategoryId(ForumPostFilterRequest request, long categoryId, Pageable pageable) {
        categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Category not found with id: " + categoryId));
        Specification<ForumPost> spec = Specification
                .<ForumPost>where((root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId))
                .and(ForumPostFilter.filterByRequest(request));
        return forumPostRepository.findAll(spec, pageable).map(ForumPostMapper::toForumPostResponse);
    }

    @Override
    @Transactional
    public ForumPostResponse createForumPost(ForumPostRequest request) {
        var user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "User not found with id: " + request.getUserId()));

        var category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(
                        () -> new EntityNotFoundException("Category not found with id: "
                                + request.getCategoryId()));
        toxicService.validateTextSafety(request.getTitle(), "Title");
        toxicService.validateTextSafety(request.getContent(), "Content");

        ForumPost forumPost = ForumPost.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .filePath(request.getFilePath())
                .isPublic(request.getIsPublic())
                .user(user)
                .category(category)
                .build();
        ForumPost savedForumPost = forumPostRepository.save(forumPost);

        return ForumPostMapper.toForumPostResponse(savedForumPost);
    }

    @Override
    public ForumPostResponse updateForumPost(long forumPostId, ForumPostRequest request) {
        ForumPost existingForumPost = forumPostRepository.findById(forumPostId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Forum post not found with id: " + forumPostId));

        toxicService.validateTextSafety(request.getTitle(), "Title");
        toxicService.validateTextSafety(request.getContent(), "Content");
        
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Category not found with id: "
                            + request.getCategoryId()));
            existingForumPost.setCategory(category);
        }

        // Chua cho sua category
        existingForumPost.setTitle(request.getTitle());
        existingForumPost.setContent(request.getContent());
        existingForumPost.setFilePath(request.getFilePath());
        existingForumPost.setIsPublic(request.getIsPublic());
//        exstingForumPost.setCategory(category);
        existingForumPost.setUpdateAt(LocalDateTime.now());

        ForumPost updatedForumPost = forumPostRepository.save(existingForumPost);
        return ForumPostMapper.toForumPostResponse(updatedForumPost);
    }

    @Override
    @Transactional
    public void deleteForumPost(long forumPostId) {
        ForumPost forumPost = forumPostRepository.findById(forumPostId)
                .orElseThrow(() -> new EntityNotFoundException("Forum post not found with id: " + forumPostId));
        forumPostRepository.delete(forumPost);
    }

}
