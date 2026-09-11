package com.vegan.api.review;

import com.vegan.api.review.dto.ReviewCreateRequest;
import com.vegan.api.security.JwtTokenProvider;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final JwtTokenProvider jwtTokenProvider;

    public ReviewController(ReviewService reviewService, JwtTokenProvider jwtTokenProvider) {
        this.reviewService = reviewService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    // GET /api/reviews?productId=1 - 상품별 리뷰 조회 (비로그인도 가능)
    @GetMapping
    public List<Review> getReviewsByProduct(@RequestParam Long productId) {
        return reviewService.getReviewsByProduct(productId);
    }

    // GET /api/reviews/my - 내 리뷰 목록
    @GetMapping("/my")
    public List<Review> getMyReviews(@RequestHeader("Authorization") String authorization) {
        Long userId = jwtTokenProvider.getUserId(authorization);
        return reviewService.getMyReviews(userId);
    }

    // POST /api/reviews - 리뷰 작성
    @PostMapping
    public Review createReview(@RequestHeader("Authorization") String authorization,
                               @RequestBody ReviewCreateRequest request) {
        Long userId = jwtTokenProvider.getUserId(authorization);
        return reviewService.createReview(userId, request);
    }

    // DELETE /api/reviews/{reviewId} - 리뷰 삭제
    @DeleteMapping("/{reviewId}")
    public void deleteReview(@RequestHeader("Authorization") String authorization,
                             @PathVariable Long reviewId) {
        Long userId = jwtTokenProvider.getUserId(authorization);
        reviewService.deleteReview(userId, reviewId);
    }
}
