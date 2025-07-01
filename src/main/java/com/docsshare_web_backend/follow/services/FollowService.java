package com.docsshare_web_backend.follow.services;

import com.docsshare_web_backend.follow.dto.requests.FollowFilterRequest;
import com.docsshare_web_backend.follow.dto.requests.FollowRequest;
import com.docsshare_web_backend.follow.dto.responses.FollowResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface FollowService {
    //Page<FollowResponse> getAllFollow(FollowFilterRequest request, Pageable pageable);
    //FollowResponse getFollow(long id);
    void unfollow(Long followerId, Long followingId);
    FollowResponse createFollow(FollowRequest request);
    List<FollowResponse> getFollowings(Long followingId);
    List<FollowResponse> getFollowers(Long followerId);
    long countFollowing(Long followerId);
    long countFollowers(Long followingId);


}
