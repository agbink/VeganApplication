package com.vegan.api.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Orders, Long> {

    // N+1 비교 테스트용 기준 조회: 연관 데이터를 fetch join하지 않습니다.
    List<Orders> findByUserIdOrderByOrderDateDesc(Long userId);

    // fetch join으로 주문 + 주문상품 + 상품을 한 번에 조회 (N+1 방지)
    @Query("select distinct o from Orders o " +
            "left join fetch o.items i " +
            "left join fetch i.product " +
            "where o.userId = :userId " +
            "order by o.orderDate desc")
    List<Orders> findByUserIdWithItems(@Param("userId") Long userId);

    @Query("select distinct o from Orders o " +
            "left join fetch o.items i " +
            "left join fetch i.product " +
            "where o.id = :orderId and o.userId = :userId")
    Optional<Orders> findByIdAndUserIdWithItems(@Param("orderId") Long orderId,
                                                 @Param("userId") Long userId);
}
