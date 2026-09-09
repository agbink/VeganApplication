package com.vegan.api.review;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByProductId(Long productId);
    List<Review> findByUser_Id(Long userId);
    boolean existsByUser_IdAndProductId(Long userId, Long productId);
    void deleteByUser_Id(Long userId);
}
