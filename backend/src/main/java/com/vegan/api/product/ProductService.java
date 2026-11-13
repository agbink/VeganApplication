package com.vegan.api.product;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
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

    // "products" 캐시: 페이지별로 첫 호출만 DB 조회, 이후 60초 동안 Redis에서 바로 반환
    @Cacheable(value = "products", key = "'all:' + #page + ':' + #size")
    public ProductSliceResponse getAll(int page, int size) {
        return ProductSliceResponse.from(
                productRepository.findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(page, size)));
    }

    @Cacheable(value = "products", key = "'category:' + #category + ':' + #page + ':' + #size")
    public ProductSliceResponse getByCategory(int category, int page, int size) {
        return ProductSliceResponse.from(
                productRepository.findByCategoryOrderByCreatedAtDescIdDesc(category, PageRequest.of(page, size)));
    }

    @Cacheable(value = "products", key = "'best:' + #page + ':' + #size")
    public ProductSliceResponse getBestSellers(int page, int size) {
        return ProductSliceResponse.from(
                productRepository.findAllByOrderBySalesCountDescIdDesc(PageRequest.of(page, size)));
    }

    /**
     * 상품명·브랜드 부분 문자열 검색 (LIKE '%keyword%').
     * ORDER BY id DESC + LIMIT이라 매칭이 많은 검색어는 앞쪽에서 금방 20건을 찾고 멈춘다.
     * 매칭이 적거나 없으면 테이블 끝까지 훑으므로 데이터가 커지면 느려진다.
     * (ngram FULLTEXT는 흔한 검색어에서 오히려 크게 느려져 채택하지 않음 - docs/category-list-fix.md)
     */
    public ProductSliceResponse search(String keyword, int page, int size) {
        String trimmed = keyword == null ? "" : keyword.trim();
        if (trimmed.isEmpty()) {
            return ProductSliceResponse.of(List.of(), page, size, true);
        }
        return ProductSliceResponse.from(
                productRepository.findByNameContainingIgnoreCaseOrBrandNameContainingIgnoreCaseOrderByIdDesc(
                        trimmed, trimmed, PageRequest.of(page, size)));
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
