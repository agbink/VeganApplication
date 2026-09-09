package com.vegan.api.product;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // page 파라미터가 없으면 기존처럼 전체 List 반환(하위호환),
    // page가 있으면 Page 객체(content, totalPages, totalElements 등) 반환
    @GetMapping
    public Object getProducts(@RequestParam(required = false) Integer category,
                              @RequestParam(required = false) Integer page,
                              @RequestParam(defaultValue = "20") int size) {
        if (page != null) {
            Pageable pageable = PageRequest.of(page, size);
            if (category != null) return productService.getByCategory(category, pageable);
            return productService.getAll(pageable);
        }
        if (category != null) return productService.getByCategory(category);
        return productService.getAll();
    }

    @GetMapping("/best")
    public Object getBestSellers(@RequestParam(required = false) Integer page,
                                 @RequestParam(defaultValue = "20") int size) {
        if (page != null) return productService.getBestSellers(PageRequest.of(page, size));
        return productService.getBestSellers();
    }

    @GetMapping("/search")
    public Object searchProducts(@RequestParam String keyword,
                                 @RequestParam(required = false) Integer page,
                                 @RequestParam(defaultValue = "20") int size) {

        if (page != null) return productService.search(keyword, PageRequest.of(page, size));
        return productService.search(keyword);
    }

    @GetMapping("/{id}")
    public Product getProduct(@PathVariable Long id) {
        return productService.getById(id);
    }

    // 어드민: 상품 추가
    @PostMapping
    public Product createProduct(@RequestAttribute(required = false) Long userId,
                                 @RequestBody Map<String, Object> body) {
        if (userId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        return productService.createProduct(body);
    }

    // 어드민: 상품 수정
    @PutMapping("/{id}")
    public Product updateProduct(@RequestAttribute(required = false) Long userId,
                                 @PathVariable Long id,
                                 @RequestBody Map<String, Object> body) {
        if (userId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        return productService.updateProduct(id, body);
    }

    // 어드민: 상품 삭제
    @DeleteMapping("/{id}")
    public void deleteProduct(@RequestAttribute(required = false) Long userId,
                              @PathVariable Long id) {
        if (userId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        productService.deleteProduct(id);
    }
}
