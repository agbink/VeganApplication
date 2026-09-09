package com.vegan.api.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

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
