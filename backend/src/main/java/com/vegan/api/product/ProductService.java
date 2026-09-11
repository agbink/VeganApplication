package com.vegan.api.product;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // "products" 캐시: 첫 호출만 DB 조회, 이후 60초 동안 Redis에서 바로 반환
    @Cacheable(value = "products", key = "'all'")
    public List<Product> getAll() {
        return productRepository.findAllByOrderByCreatedAtDesc();
    }

    @Cacheable(value = "products", key = "'category:' + #category")
    public List<Product> getByCategory(int category) {
        return productRepository.findByCategoryOrderByCreatedAtDesc(category);
    }

    @Cacheable(value = "products", key = "'best'")
    public List<Product> getBestSellers() {
        return productRepository.findAllByOrderBySalesCountDesc();
    }

    public List<Product> search(String keyword) {
        return productRepository.findByNameContainingIgnoreCaseOrBrandNameContainingIgnoreCase(keyword, keyword);
    }

    // 페이지네이션 버전
    // (Page 객체는 Redis 직렬화가 까다로워서 content만 캐시하는 별도 설계가 필요 - 여기선 캐시 제외)
    public Page<Product> getAll(Pageable pageable) {
        return productRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    public Page<Product> getByCategory(int category, Pageable pageable) {
        return productRepository.findByCategoryOrderByCreatedAtDesc(category, pageable);
    }

    public Page<Product> getBestSellers(Pageable pageable) {
        return productRepository.findAllByOrderBySalesCountDesc(pageable);
    }

    public Page<Product> search(String keyword, Pageable pageable) {
        return productRepository.findByNameContainingIgnoreCaseOrBrandNameContainingIgnoreCase(keyword, keyword, pageable);
    }

    public Product getById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다. id=" + id));
    }

    // 어드민: 상품 추가 - 상품이 바뀌면 목록 캐시 전체 무효화
    @CacheEvict(value = "products", allEntries = true)
    public Product createProduct(Map<String, Object> body) {
        Product p = new Product(
                (String) body.get("name"),
                (String) body.get("brandName"),
                toInt(body.get("price")),
                (String) body.get("imageUrl"),
                (String) body.get("detailImageUrl"),
                toInt(body.get("stock")),
                toInt(body.get("category")),
                (String) body.getOrDefault("description", "")
        );
        return productRepository.save(p);
    }

    // 어드민: 상품 수정
    @CacheEvict(value = "products", allEntries = true)
    @Transactional
    public Product updateProduct(Long id, Map<String, Object> body) {
        Product p = getById(id);
        p.update(
                (String) body.get("name"),
                (String) body.get("brandName"),
                toInt(body.get("price")),
                (String) body.get("imageUrl"),
                (String) body.get("detailImageUrl"),
                toInt(body.get("stock")),
                toInt(body.get("category")),
                (String) body.getOrDefault("description", ""),
                toInt(body.getOrDefault("salesCount", 0))
        );
        return p;
    }

    // 어드민: 상품 삭제
    @CacheEvict(value = "products", allEntries = true)
    public void deleteProduct(Long id) {
        Product p = getById(id);
        productRepository.delete(p);
    }

    private int toInt(Object val) {
        if (val instanceof Integer) return (Integer) val;
        if (val instanceof Long) return ((Long) val).intValue();
        if (val instanceof String) return Integer.parseInt((String) val);
        return 0;
    }
}
