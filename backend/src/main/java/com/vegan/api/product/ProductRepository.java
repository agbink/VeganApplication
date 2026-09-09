package com.vegan.api.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    List<Product> findAllByOrderByCreatedAtDesc();

    List<Product> findByCategoryOrderByCreatedAtDesc(int category);

    List<Product> findAllByOrderBySalesCountDesc();

    List<Product> findByNameContainingIgnoreCaseOrBrandNameContainingIgnoreCase(String name, String brandName);

    // 페이지네이션 버전
    Page<Product> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Product> findByCategoryOrderByCreatedAtDesc(int category, Pageable pageable);

    Page<Product> findAllByOrderBySalesCountDesc(Pageable pageable);

    Page<Product> findByNameContainingIgnoreCaseOrBrandNameContainingIgnoreCase(String name, String brandName, Pageable pageable);
}
