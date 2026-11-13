package com.vegan.api.product;

import com.vegan.api.admin.AdminAuthorizationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final AdminAuthorizationService adminAuthorizationService;

    public ProductController(ProductService productService, AdminAuthorizationService adminAuthorizationService) {
        this.productService = productService;
        this.adminAuthorizationService = adminAuthorizationService;
    }

    // 목록 API는 항상 페이지 단위로 응답한다.
    // page를 생략하면 첫 페이지, size는 1~MAX_PAGE_SIZE로 제한해 한 번에 전체 상품을 내려주지 않는다.
    static final int DEFAULT_PAGE_SIZE = 20;
    static final int MAX_PAGE_SIZE = 100;

    @GetMapping
    public ProductSliceResponse getProducts(@RequestParam(required = false) Integer category,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size) {
        if (category != null) return productService.getByCategory(category, safePage(page), safeSize(size));
        return productService.getAll(safePage(page), safeSize(size));
    }

    @GetMapping("/best")
    public ProductSliceResponse getBestSellers(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size) {
        return productService.getBestSellers(safePage(page), safeSize(size));
    }

    @GetMapping("/search")
    public ProductSliceResponse searchProducts(@RequestParam String keyword,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size) {
        return productService.search(keyword, safePage(page), safeSize(size));
    }

    private static int safePage(int page) {
        return Math.max(page, 0);
    }

    private static int safeSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }

    @GetMapping("/{id}")
    public Product getProduct(@PathVariable Long id) {
        return productService.getById(id);
    }

    // 어드민: 상품 추가
    @PostMapping
    public Product createProduct(@RequestAttribute(required = false) Long userId,
                                 @RequestBody Map<String, Object> body) {
        adminAuthorizationService.requireAdmin(userId);
        return productService.createProduct(body);
    }

    // 어드민: 상품 수정
    @PutMapping("/{id}")
    public Product updateProduct(@RequestAttribute(required = false) Long userId,
                                 @PathVariable Long id,
                                 @RequestBody Map<String, Object> body) {
        adminAuthorizationService.requireAdmin(userId);
        return productService.updateProduct(id, body);
    }

    // 어드민: 상품 삭제
    @DeleteMapping("/{id}")
    public void deleteProduct(@RequestAttribute(required = false) Long userId,
                              @PathVariable Long id) {
        adminAuthorizationService.requireAdmin(userId);
        productService.deleteProduct(id);
    }
}
