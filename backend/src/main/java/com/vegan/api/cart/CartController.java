package com.vegan.api.cart;

import com.vegan.api.cart.dto.CartAddRequest;
import com.vegan.api.security.JwtTokenProvider;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;
    private final JwtTokenProvider jwtTokenProvider;

    public CartController(CartService cartService, JwtTokenProvider jwtTokenProvider) {
        this.cartService = cartService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    // GET /api/cart - 내 장바구니 조회
    @GetMapping
    public List<CartItem> getCart(@RequestHeader("Authorization") String authorization) {
        Long userId = jwtTokenProvider.getUserId(authorization);
        return cartService.getCart(userId);
    }

    // POST /api/cart - 장바구니 추가
    @PostMapping
    public CartItem addItem(@RequestHeader("Authorization") String authorization,
                            @RequestBody CartAddRequest request) {
        Long userId = jwtTokenProvider.getUserId(authorization);
        return cartService.addItem(userId, request);
    }

    // PUT /api/cart/{itemId} - 수량 변경
    @PutMapping("/{itemId}")
    public CartItem updateQuantity(@RequestHeader("Authorization") String authorization,
                                   @PathVariable Long itemId,
                                   @RequestBody Map<String, Integer> body) {
        Long userId = jwtTokenProvider.getUserId(authorization);
        Integer quantity = body.get("quantity");
        if (quantity == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "quantity가 필요합니다.");
        }
        return cartService.updateQuantity(userId, itemId, quantity);
    }

    // DELETE /api/cart/{itemId} - 항목 삭제
    @DeleteMapping("/{itemId}")
    public void deleteItem(@RequestHeader("Authorization") String authorization,
                           @PathVariable Long itemId) {
        Long userId = jwtTokenProvider.getUserId(authorization);
        cartService.deleteItem(userId, itemId);
    }

    // DELETE /api/cart - 장바구니 전체 비우기 (주문 후)
    @DeleteMapping
    public void clearCart(@RequestHeader("Authorization") String authorization) {
        Long userId = jwtTokenProvider.getUserId(authorization);
        cartService.clearCart(userId);
    }
}
