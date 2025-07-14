package com.docsshare_web_backend.follow.domain;

import com.docsshare_web_backend.follow.dto.requests.FollowFilterRequest;
import com.docsshare_web_backend.follow.dto.requests.FollowRequest;
import com.docsshare_web_backend.follow.dto.responses.FollowResponse;
import com.docsshare_web_backend.follow.services.FollowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/follow")
public class FollowController {
    @Autowired
    private FollowService followService;

//    @GetMapping
//    public ResponseEntity<Page<FollowResponse>> getAllOrder(
//            @ModelAttribute FollowFilterRequest request,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "9") int size,
//            @RequestParam(defaultValue = "desc") String sort) {
//        log.debug("Received request to get all order with filter: {}, page: {}, size: {}, sort: {}",
//                request, page, size, sort);
//        Sort sortOrder = sort.equals("asc") ? Sort.by("createdAt").ascending() : Sort.by("createdAt").descending();
//        Pageable pageable = PageRequest.of(page, size, sortOrder);
//        Page<FollowResponse> orders = followService.getAllOrder(request, pageable);
//        return ResponseEntity.ok(orders);
//    }
//
//    @GetMapping("/{orderId}")
//    public ResponseEntity<FollowResponse> getOrder(@PathVariable long orderId) {
//        log.debug("[OrderController] Get Order with id {}", orderId);
//        return ResponseEntity.ok(followService.getOrder(orderId));
//    }
//
//    @GetMapping("/slug/{slug}")
//    public ResponseEntity<OrderResponse> getDocumentBySlug(@PathVariable String slug) {
//        log.debug("[DocumentController] Get Document by slug {}", slug);
//        return ResponseEntity.ok(orderService.getDocumentBySlug(slug));
//    }
//
//    @GetMapping("/category/{categoryId}")
//    public ResponseEntity<Page<OrderResponse>> getDocumentsByCategoryId(
//            @PathVariable long categoryId,
//            @ModelAttribute OrderFilterRequest request,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "9") int size,
//            @RequestParam(defaultValue = "desc") String sort) {
//
//        Sort sortOrder = sort.equals("asc") ? Sort.by("createdAt").ascending() : Sort.by("createdAt").descending();
//        Pageable pageable = PageRequest.of(page, size, sortOrder);
//        Page<OrderResponse> documents = orderService.getDocumentsByCategoryId(request, categoryId, pageable);
//        return ResponseEntity.ok(documents);
//    }
//
//    @GetMapping("/need-approved")
//    public ResponseEntity<Page<OrderResponse>> getDocumentsNeedApproved(
//            @ModelAttribute OrderFilterRequest request,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "9") int size,
//            @RequestParam(defaultValue = "desc") String sort){
//
//        Sort sortOrder = sort.equals("asc") ? Sort.by("createdAt").ascending() : Sort.by("createdAt").descending();
//        Pageable pageable = PageRequest.of(page, size, sortOrder);
//        Page<OrderResponse> documents = orderService.getDocumentsNeedApproved(request, pageable);
//        return ResponseEntity.ok(documents);
//    }
//

    @PostMapping("/create")
    public ResponseEntity<FollowResponse> createFollow(@RequestBody FollowRequest followRequest) {
        log.debug("[FollowController] Create Follow {}", followRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(followService.createFollow(followRequest));
    }

    @DeleteMapping("/api/v1/follow/{followingId}/unfollow")
    public ResponseEntity<Map<String, String>> unfollow(
            @PathVariable Long followingId,
            @RequestParam Long followerId) {

        followService.unfollow(followerId, followingId);
        return ResponseEntity.ok(Map.of("message", "Hủy follow thành công"));
    }


    // Lấy danh sách người mình đang theo dõi
    @GetMapping("/following/{followerId}")
    public ResponseEntity<List<FollowResponse>> getFollowings(@PathVariable Long followerId) {
        log.debug("[FollowController] Get following list for followerId={}", followerId);
        return ResponseEntity.ok(followService.getFollowings(followerId));
    }

    // Lấy danh sách người theo dõi mình
    @GetMapping("/follower/{followingId}")
    public ResponseEntity<List<FollowResponse>> getFollowers(@PathVariable Long followingId) {
        log.debug("[FollowController] Get following list for followingId={}", followingId);
        return ResponseEntity.ok(followService.getFollowers(followingId));
    }
    @GetMapping("/count/followers/{followingId}")
    public ResponseEntity<Long> countFollowers(@PathVariable Long followingId) {
        long count = followService.countFollowers(followingId);
        return ResponseEntity.ok(count);
    }
    @GetMapping("/count/following/{followerId}")
    public ResponseEntity<Long> countFollowing(@PathVariable Long followerId) {
        long count = followService.countFollowing(followerId);
        return ResponseEntity.ok(count);
    }

}
