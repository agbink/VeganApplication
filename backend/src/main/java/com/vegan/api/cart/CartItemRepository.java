package com.vegan.api.cart;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // 내 장바구니 전체 조회 (인덱스: idx_cart_user 사용)
    List<CartItem> findByUser_Id(Long userId);

    // 특정 항목 조회 (수량 변경/삭제용)
    Optional<CartItem> findByIdAndUser_Id(Long id, Long userId);

    // 같은 상품 이미 담겼는지 확인 (중복 담기 방지용)
    Optional<CartItem> findByUser_IdAndProduct_Id(Long userId, Long productId);

    // 장바구니 전체 비우기 (결제 완료 후)
    void deleteByUser_Id(Long userId);
}
