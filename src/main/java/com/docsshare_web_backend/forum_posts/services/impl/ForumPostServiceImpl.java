package com.docsshare_web_backend.forum_posts.services.impl;

import com.docsshare_web_backend.account.dto.responses.UserResponse;
import com.docsshare_web_backend.categories.models.Category;
import com.docsshare_web_backend.categories.repositories.CategoryRepository;
import com.docsshare_web_backend.documents.models.Document;
import com.docsshare_web_backend.documents.repositories.DocumentRepository;
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
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

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
    private DocumentRepository documentRepository;
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
                    .category(
                            forumPost.getDocument() != null && forumPost.getDocument().getCategory() != null
                                    ? forumPost.getDocument().getCategory().getName()
                                    : (forumPost.getCategory() != null ? forumPost.getCategory().getName() : "")
                    )
                    .createdAt(forumPost.getCreatedAt())
                    .updateAt(forumPost.getUpdateAt())
                    .views(forumPost.getViews())
                    .tags(forumPost.getTags())
                    .linkDocument(forumPost.getDocument() != null ? forumPost.getDocument().getSlug() : null)
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
    public Page<ForumPostResponse> getForumPostByDocumentId(ForumPostFilterRequest request, long documentId, Pageable pageable) {
        documentRepository.findById(documentId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Document not found with id: " + documentId));

        Specification<ForumPost> spec = Specification
                .<ForumPost>where((root, query, cb) -> cb.equal(root.get("document").get("id"), documentId));

        // Kết hợp thêm các điều kiện lọc khác nếu có
        spec = spec.and(ForumPostFilter.filterByRequest(request));

        return forumPostRepository.findAll(spec, pageable)
                .map(ForumPostMapper::toForumPostResponse);
    }


    //    @Override
//    @Transactional(readOnly = true)
//    public Page<ForumPostResponse> getForumPostByCategoryId(ForumPostFilterRequest request, long categoryId, Pageable pageable) {
//        categoryRepository.findById(categoryId)
//                .orElseThrow(() -> new EntityNotFoundException(
//                        "Category not found with id: " + categoryId));
//        Specification<ForumPost> spec = Specification
//                .<ForumPost>where((root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId))
//                .and(ForumPostFilter.filterByRequest(request));
//        return forumPostRepository.findAll(spec, pageable).map(ForumPostMapper::toForumPostResponse);
//    }
//@Override
//@Transactional(readOnly = true)
//public Page<ForumPostResponse> getForumPostByCategoryId(ForumPostFilterRequest request, long categoryId, Pageable pageable) {
//    categoryRepository.findById(categoryId)
//            .orElseThrow(() -> new EntityNotFoundException(
//                    "Category not found with id: " + categoryId));
//
//    Specification<ForumPost> spec = Specification
//            .<ForumPost>where((root, query, cb) -> cb.or(
//                    cb.and(
//                            cb.equal(root.get("category").get("id"), categoryId),
//                            cb.isNull(root.get("document"))
//                    ),
//                    cb.equal(root.get("document").get("category").get("id"), categoryId)
//            ))
//            .and(ForumPostFilter.filterByRequest(request));
//
//    return forumPostRepository.findAll(spec, pageable)
//            .map(ForumPostMapper::toForumPostResponse);
//}
@Override
@Transactional(readOnly = true)
public Page<ForumPostResponse> getForumPostByCategoryId(ForumPostFilterRequest request, long categoryId, Pageable pageable) {
    // Kiểm tra categoryId có tồn tại không
    categoryRepository.findById(categoryId)
            .orElseThrow(() -> new EntityNotFoundException(
                    "Category not found with id: " + categoryId));

    Specification<ForumPost> spec = (root, query, cb) -> {
        // LEFT JOIN tới document và document.category
        Join<Object, Object> documentJoin = root.join("document", JoinType.LEFT);
        Join<Object, Object> documentCategoryJoin = documentJoin.join("category", JoinType.LEFT);

        // Điều kiện: (forum_post.category_id = categoryId AND document IS NULL)
        Predicate condition1 = cb.and(
                cb.equal(root.get("category").get("id"), categoryId),
                cb.isNull(root.get("document"))
        );

        // Điều kiện: (document.category_id = categoryId)
        Predicate condition2 = cb.equal(documentCategoryJoin.get("id"), categoryId);

        // Kết hợp 2 điều kiện bằng OR
        return cb.or(condition1, condition2);
    };

    // Kết hợp thêm bộ lọc nếu có
    spec = spec.and(ForumPostFilter.filterByRequest(request));

    return forumPostRepository.findAll(spec, pageable)
            .map(ForumPostMapper::toForumPostResponse);
}



    @Override
    @Transactional
    public ForumPostResponse createForumPost(ForumPostRequest request) {
        var user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "User not found with id: " + request.getUserId()));

        Document document = null;
        Category category = null;

        if (request.getDocumentId() != null) {
            // Nếu có documentId → bỏ qua category
            document = documentRepository.findById(request.getDocumentId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Document not found with id: " + request.getDocumentId()));
        } else {
            // Nếu không có documentId → bắt buộc phải có categoryId
            if (request.getCategoryId() == null) {
                throw new IllegalArgumentException("Category ID must be provided if Document ID is null.");
            }

            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Category not found with id: " + request.getCategoryId()));
        }

        toxicService.validateTextSafety(request.getTitle(), "Title");
        toxicService.validateTextSafety(request.getContent(), "Content");

        ForumPost forumPost = ForumPost.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .filePath(request.getFilePath())
                .isPublic(request.getIsPublic())
                .tags(request.getTags())
                .user(user)
                .category(category)
                .document(document)
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
        existingForumPost.setTags(request.getTags());
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

    @Override
    public Set<String> getTagsByDocumentId(Long documentId) {
        return forumPostRepository.findDistinctTagsByDocumentId(documentId);
    }

    @Override
    public Set<String> getTagsByCategoryId(Long categoryId) {
        return forumPostRepository.findDistinctTagsByCategoryId(categoryId);
    }

}
