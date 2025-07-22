package com.docsshare_web_backend.saved_posts.services.impl;

import com.docsshare_web_backend.account.dto.responses.UserResponse;
import com.docsshare_web_backend.forum_posts.dto.responses.ForumPostResponse;
import com.docsshare_web_backend.forum_posts.models.ForumPost;
import com.docsshare_web_backend.forum_posts.repositories.ForumPostRepository;
import com.docsshare_web_backend.saved_posts.dto.requests.SavedPostsFilterResquest;
import com.docsshare_web_backend.saved_posts.dto.requests.SavedPostsRequest;
import com.docsshare_web_backend.saved_posts.dto.responses.SavedPostsResponse;
import com.docsshare_web_backend.saved_posts.filters.SavedPostsFilter;
import com.docsshare_web_backend.saved_posts.models.SavedPosts;
import com.docsshare_web_backend.saved_posts.repositories.SavedPostsRepository;
import com.docsshare_web_backend.saved_posts.services.SavedPostsService;
import com.docsshare_web_backend.users.models.User;
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
import java.util.Optional;

@Slf4j
@Service
public class SavedPostsServiceImpl implements SavedPostsService {
    @Autowired
    private SavedPostsRepository savedPostsRepository;

    @Autowired
    private ForumPostRepository forumPostRepository;

    @Autowired
    private UserRepository userRepository;

    private Pageable getPageable(Pageable pageable) {
        return pageable != null ? pageable : Pageable.unpaged();
    }

    public static class SavedPostsMapper{
        public static SavedPostsResponse toSavedPostsResponse(SavedPosts savedPosts){
            return SavedPostsResponse.builder()
                    .id(savedPosts.getId())
                    .forumPost(ForumPostResponse.builder()
                            .id(savedPosts.getForumPost().getId())
                            .title(savedPosts.getForumPost().getTitle())
                            .content(savedPosts.getForumPost().getContent())
                            .filePath(savedPosts.getForumPost().getFilePath())
                            .isPublic(savedPosts.getForumPost().getIsPublic() != null ? savedPosts.getForumPost().getIsPublic().toString() : null)
//                            .user(comment.getForumPost().getUser() != null ? comment.getForumPost().getUser().getName() : "")
                            .user(UserResponse.builder()
                                    .id(savedPosts.getForumPost().getUser().getId())
                                    .name(savedPosts.getForumPost().getUser().getName())
                                    .avatar(savedPosts.getForumPost().getUser().getAvatar())
                                    .build())
                            .linkDocument(
                                    savedPosts.getForumPost().getDocument() != null && savedPosts.getForumPost().getDocument().getSlug() != null
                                            ? savedPosts.getForumPost().getDocument().getSlug()
                                            : null
                            )

                            .category(savedPosts.getForumPost().getCategory() != null ? savedPosts.getForumPost().getCategory().getName() : "")
                            .createdAt(savedPosts.getForumPost().getCreatedAt())
                            .updateAt(savedPosts.getForumPost().getUpdateAt())
                            .build())
                    .user(UserResponse.builder()
                            .id(savedPosts.getUser().getId())
                            .name(savedPosts.getUser().getName())
                            .avatar(savedPosts.getUser().getAvatar())
                            .college(savedPosts.getUser().getCollege())
                            .build())
                    .build();
        }
    }


    @Override
    @Transactional(readOnly = true)
    public Page<SavedPostsResponse> getSavedPostsByUserId(SavedPostsFilterResquest request, long userId, Pageable pageable) {
        userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "User not found with id: " + userId));
        Specification<SavedPosts> spec = Specification
                .<SavedPosts>where((root, query, cb) -> cb.equal(root.get("user").get("id"), userId))
                .and(SavedPostsFilter.filterByRequest(request));
        return savedPostsRepository.findAll(spec, pageable).map(SavedPostsMapper::toSavedPostsResponse);
    }

    @Override
    @Transactional
    public SavedPostsResponse savePost(SavedPostsRequest request) {
        ForumPost forumPost = forumPostRepository.findById(request.getForumPostId())
                .orElseThrow(() -> new EntityNotFoundException("Forum post not found with id: " + request.getForumPostId()));

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + request.getUserId()));

        Optional<SavedPosts> existing = savedPostsRepository.findByUserIdAndForumPostId(request.getUserId(), request.getForumPostId());
        if (existing.isPresent()) {
            return SavedPostsMapper.toSavedPostsResponse(existing.get());
        }

        SavedPosts saved = new SavedPosts();
        saved.setUser(user);
        saved.setForumPost(forumPost);
        saved.setSavedAt(LocalDateTime.now());

        SavedPosts savedResult = savedPostsRepository.save(saved);
        return SavedPostsMapper.toSavedPostsResponse(savedResult);
    }

    @Override
    public void unsavePost(SavedPostsRequest request) {
        Optional<SavedPosts> existing = savedPostsRepository.findByUserIdAndForumPostId(request.getUserId(), request.getForumPostId());
        if (existing.isPresent()) {
            savedPostsRepository.delete(existing.get());
        } else {
            throw new EntityNotFoundException("Saved forum post not found for userId=" + request.getUserId() + " and forumPostId=" + request.getForumPostId());
        }
    }
}
