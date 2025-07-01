package com.docsshare_web_backend.follow.services.impl;

import com.docsshare_web_backend.documents.models.Document;
import com.docsshare_web_backend.documents.repositories.DocumentRepository;
import com.docsshare_web_backend.follow.dto.requests.FollowFilterRequest;
import com.docsshare_web_backend.follow.dto.requests.FollowRequest;
import com.docsshare_web_backend.follow.dto.responses.FollowResponse;
import com.docsshare_web_backend.follow.filters.FollowFilter;
import com.docsshare_web_backend.follow.models.Follow;
import com.docsshare_web_backend.follow.models.FollowId;
import com.docsshare_web_backend.follow.repositories.FollowRepository;
import com.docsshare_web_backend.follow.services.FollowService;
import com.docsshare_web_backend.payment.models.Payment;
import com.docsshare_web_backend.payment.repositories.PaymentRepository;
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
import java.util.List;

@Slf4j
@Service
public class FollowServiceImpl implements FollowService {
    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private UserRepository userRepository;

    private Pageable getPageable(Pageable pageable) {
        return pageable != null ? pageable : Pageable.unpaged();
    }


    public static class FollowMapper {
                public static FollowResponse toFollowResponse(Follow follow) {
                    return FollowResponse.builder()
                            .followerId(follow.getFollower().getId())
                            .followerName(follow.getFollower().getName()) // Giả sử User có getName()
                            .followingId(follow.getFollowing().getId())
                            .followingName(follow.getFollowing().getName()) // Giả sử User có getName()
                            .createdAt(follow.getCreateAt())
                            .build();
                }

        }
//
//        @Override
//        @Transactional(readOnly = true)
//        public Page<FollowResponse> getAllOrder(FollowFilterRequest request, Pageable pageable) {
//                Specification<Follow> spec = FollowFilter.filterByRequest(request);
//                return followRepository.findAll(spec, getPageable(pageable))
//                                .map(FollowMapper::toFollowResponse);
//        }
//
//        @Override
//        @Transactional(readOnly = true)
//        public FollowResponse getOrder(long id) {
//                Follow follow = followRepository.findById(id)
//                                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + id));
//
//                return FollowMapper.toFollowResponse(follow);
//        }

    @Override
    @Transactional
    public FollowResponse createFollow(FollowRequest request) {
        // 1. Lấy thông tin người theo dõi (follower)
        User follower = userRepository.findById(request.getFollowerId())
                .orElseThrow(() -> new EntityNotFoundException("Follower not found with id: " + request.getFollowerId()));

        // 2. Lấy thông tin người được theo dõi (following)
        User following = userRepository.findById(request.getFollowingId())
                .orElseThrow(() -> new EntityNotFoundException("Following not found with id: " + request.getFollowingId()));

        // 3. Kiểm tra xem mối quan hệ đã tồn tại chưa
        if (followRepository.existsByFollowerIdAndFollowingId(
                request.getFollowerId(),
                request.getFollowingId())) {
            throw new IllegalStateException("Đã follow người dùng này rồi");
        }

        // 4. Tạo và lưu Follow
        Follow follow = Follow.builder()
                .follower(follower)
                .following(following)
                .build();
        Follow savedFollow = followRepository.save(follow);

        // 5. Trả về FollowResponse
        return FollowMapper.toFollowResponse(savedFollow);
    }

    @Override
    @Transactional
    public void unfollow(Long followerId, Long followingId) {
        // 1. Tìm bản ghi follow giữa follower và following
        Follow follow = followRepository
                .findByFollowerIdAndFollowingId(followerId, followingId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Follow relationship not found between followerId " + followerId + " and followingId " + followingId
                ));

        // 2. Xoá bản ghi đó
        followRepository.delete(follow);
    }

@Override
@Transactional
public List<FollowResponse> getFollowers(Long followerId) {
    List<Follow> follows = followRepository.findByFollowingId(followerId);
    return follows.stream()
            .map(FollowMapper::toFollowResponse)
            .toList();
}

@Override
@Transactional
public List<FollowResponse> getFollowings(Long followingId) {
    List<Follow> follows = followRepository.findByFollowerId(followingId);
    return follows.stream()
            .map(FollowMapper::toFollowResponse)
            .toList();
}

    @Override
    @Transactional
    public long countFollowing(Long followerId) {
        return followRepository.countByFollowerId(followerId);
    }

    @Override
    @Transactional
    public long countFollowers(Long followingId) {
        return followRepository.countByFollowingId(followingId);
    }

}
