package com.vegan.api.cart;

import com.vegan.api.cart.dto.CartAddRequest;
import com.vegan.api.product.Product;
import com.vegan.api.product.ProductRepository;
import com.vegan.api.user.User;
import com.vegan.api.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public CartService(CartItemRepository cartItemRepository,
                       UserRepository userRepository,
                       ProductRepository productRepository) {
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<CartItem> getCart(Long userId) {
        return cartItemRepository.findByUser_Id(userId);
    }

    @Transactional
    public CartItem addItem(Long userId, CartAddRequest request) {
        if (request.getSelectedQuantity() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "수량은 1개 이상이어야 합니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        // 서버에서 직접 상품 조회 (앱이 보내준 가격/재고 데이터 신뢰 안 함)
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."));

        // 재고 체크
        if (product.getStock() < request.getSelectedQuantity()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "재고가 부족합니다: " + product.getName());
        }

        // 이미 담긴 상품이면 수량만 추가 (중복 row 생성 안 함)
        Optional<CartItem> existing = cartItemRepository
                .findByUser_IdAndProduct_Id(userId, request.getProductId());

        if (existing.isPresent()) {
            int nextQuantity = existing.get().getSelectedQuantity() + request.getSelectedQuantity();
            if (nextQuantity > product.getStock()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "재고가 부족합니다: " + product.getName());
            }
            existing.get().updateQuantity(nextQuantity);
            return existing.get();
        }

        return cartItemRepository.save(
                new CartItem(user, product, request.getSelectedQuantity())
        );
    }

    @Transactional
    public CartItem updateQuantity(Long userId, Long itemId, int quantity) {
        CartItem item = cartItemRepository.findByIdAndUser_Id(itemId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "장바구니 항목을 찾을 수 없습니다."));
        if (quantity < 1 || quantity > item.getProductStock()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "수량은 1개 이상이며 현재 재고를 초과할 수 없습니다.");
        }
        item.updateQuantity(quantity);
        return item;
    }

    @Transactional
    public void deleteItem(Long userId, Long itemId) {
        CartItem item = cartItemRepository.findByIdAndUser_Id(itemId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "장바구니 항목을 찾을 수 없습니다."));
        cartItemRepository.delete(item);
    }

    @Transactional
    public void clearCart(Long userId) {
        cartItemRepository.deleteByUser_Id(userId);
    }
}
