package com.vegan.api.admin;

import com.vegan.api.order.OrderRepository;
import com.vegan.api.order.Orders;
import com.vegan.api.review.ReviewRepository;
import com.vegan.api.review.Review;
import com.vegan.api.user.User;
import com.vegan.api.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository;
    private final AdminAuthorizationService adminAuthorizationService;

    public AdminController(UserRepository userRepository,
                           OrderRepository orderRepository,
                           ReviewRepository reviewRepository,
                           AdminAuthorizationService adminAuthorizationService) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.reviewRepository = reviewRepository;
        this.adminAuthorizationService = adminAuthorizationService;
    }

    // 어드민 공통 체크: JWT가 있어야 접근 가능
    // GET /api/admin/users -> 전체 회원 목록
    @GetMapping("/users")
    public List<User> getUsers(@RequestAttribute(required = false) Long userId) {
        adminAuthorizationService.requireAdmin(userId);
        return userRepository.findAll();
    }

    // GET /api/admin/orders -> 전체 주문 목록 (최신순)
    @GetMapping("/orders")
    public List<Orders> getAllOrders(@RequestAttribute(required = false) Long userId) {
        adminAuthorizationService.requireAdmin(userId);
        return orderRepository.findAll(
                org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "orderDate"));
    }

    // DELETE /api/admin/orders/{id} -> 주문 삭제
    @DeleteMapping("/orders/{id}")
    @Transactional
    public void deleteOrder(@RequestAttribute(required = false) Long userId,
                            @PathVariable Long id) {
        adminAuthorizationService.requireAdmin(userId);
        Orders order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."));
        orderRepository.delete(order);
    }

    // PATCH /api/admin/orders/{id}/state -> 배송완료 처리
    @PatchMapping("/orders/{id}/state")
    @Transactional
    public Orders updateOrderState(@RequestAttribute(required = false) Long userId,
                                   @PathVariable Long id) {
        adminAuthorizationService.requireAdmin(userId);
        Orders order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."));
        order.setOrderState("배송완료");
        return orderRepository.save(order);
    }

    // GET /api/admin/reviews -> 전체 리뷰 목록 (최신순)
    @GetMapping("/reviews")
    public List<Review> getAllReviews(@RequestAttribute(required = false) Long userId) {
        adminAuthorizationService.requireAdmin(userId);
        return reviewRepository.findAll(
                org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
    }

    // DELETE /api/admin/reviews/{id} -> 리뷰 강제 삭제 (소유자 확인 없음)
    @DeleteMapping("/reviews/{id}")
    @Transactional
    public void deleteReview(@RequestAttribute(required = false) Long userId,
                             @PathVariable Long id) {
        adminAuthorizationService.requireAdmin(userId);
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다."));
        reviewRepository.delete(review);
    }
}
