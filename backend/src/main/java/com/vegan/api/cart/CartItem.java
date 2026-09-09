package com.vegan.api.cart;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vegan.api.product.Product;
import com.vegan.api.user.User;
import jakarta.persistence.*;

@Entity
@Table(name = "cart_item")


public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    // 개선 전: productName, productBrand, productPrice, productImg, productStock 중복 저장
    // 개선 후: Product를 직접 참조 → 항상 최신 가격/재고/이미지 반영됨
    // (OrderItem의 스냅샷 패턴과 다른 이유: 장바구니는 결제 전이라 최신 정보가 맞음)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnore
    private Product product;

    private int selectedQuantity;

    protected CartItem() {}

    public CartItem(User user, Product product, int selectedQuantity) {
        this.user = user;
        this.product = product;
        this.selectedQuantity = selectedQuantity;
    }

    public void updateQuantity(int quantity) {
        this.selectedQuantity = quantity;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Long getProductId() { return product.getId(); }
    public String getProductName() { return product.getName(); }
    public String getProductBrand() { return product.getBrandName(); }
    public int getProductPrice() { return product.getPrice(); }
    public String getProductImg() { return product.getImageUrl(); }
    public int getProductStock() { return product.getStock(); }
    public int getSelectedQuantity() { return selectedQuantity; }
}
