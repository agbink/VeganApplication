package com.vegan.api.product;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    // 페이지네이션 버전
    // 무한 스크롤은 전체 개수가 필요 없으므로 Slice로 받아 COUNT(*) 쿼리를 생략한다.
    Slice<Product> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

    Slice<Product> findByCategoryOrderByCreatedAtDescIdDesc(int category, Pageable pageable);

    Slice<Product> findAllByOrderBySalesCountDescIdDesc(Pageable pageable);

    // 부분 문자열 검색 (LIKE '%keyword%'). COUNT 없이 Slice로 다음 페이지 여부만 판단
    Slice<Product> findByNameContainingIgnoreCaseOrBrandNameContainingIgnoreCaseOrderByIdDesc(
            String name, String brandName, Pageable pageable);
}
