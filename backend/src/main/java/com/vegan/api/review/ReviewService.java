package com.vegan.api.review;

import com.vegan.api.review.dto.ReviewCreateRequest;
import com.vegan.api.product.Product;
import com.vegan.api.product.ProductRepository;
import com.vegan.api.user.User;
import com.vegan.api.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public ReviewService(ReviewRepository reviewRepository, UserRepository userRepository,
                         ProductRepository productRepository) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    public List<Review> getReviewsByProduct(Long productId) {
        return reviewRepository.findByProductId(productId);
    }

    public List<Review> getMyReviews(Long userId) {
        return reviewRepository.findByUser_Id(userId);
    }

    @Transactional
    public Review createReview(Long userId, ReviewCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        if (reviewRepository.existsByUser_IdAndProductId(userId, request.getProductId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 리뷰를 작성했습니다.");
        }
        if (request.getRating() < 1 || request.getRating() > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "별점은 1점에서 5점 사이여야 합니다.");
        }
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "리뷰 내용을 입력해주세요.");
        }
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."));

        Review review = new Review(
                user,
                request.getProductId(),
                product.getName(),
                product.getImageUrl(),
                request.getContent(),
                request.getRating(),
                request.getReviewImageUrl()
        );
        return reviewRepository.save(review);
    }

    @Transactional
    public void deleteReview(Long userId, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다."));
        if (!review.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "삭제 권한이 없습니다.");
        }
        reviewRepository.delete(review);
    }
}
