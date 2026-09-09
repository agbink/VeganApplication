package com.vegan.api.order;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vegan.api.product.Product;
import jakarta.persistence.*;

@Entity
@Table(name = "order_item")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id")
   // @JsonIgnore // Orders -> items -> order -> items ... 무한 직렬화 방지
    private Orders order;

    @ManyToOne(fetch = FetchType.LAZY) // fetch join으로 함께 조회 (N+1 방지)
    @JoinColumn(name = "product_id")
    private Product product;

    // 주문 당시 스냅샷 - 나중에 상품 가격/이름이 바뀌어도 과거 주문 내역은 그대로 유지됨
    private String productNameSnapshot;
    private int priceAtOrder;
    private String imageUrlSnapshot;

    private int quantity;
    private boolean reviewed; // 기존 doReview

    protected OrderItem() {
    }

    public OrderItem(Product product, int quantity) {
        this.product = product;
        this.productNameSnapshot = product.getName();
        this.priceAtOrder = product.getPrice();
        this.imageUrlSnapshot = product.getImageUrl();
        this.quantity = quantity;
        this.reviewed = false;
    }

    void assignOrder(Orders order) {
        this.order = order;
    }

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public String getProductNameSnapshot() {
        return productNameSnapshot;
    }

    public int getPriceAtOrder() {
        return priceAtOrder;
    }

    public String getImageUrlSnapshot() {
        return imageUrlSnapshot;
    }

    public int getQuantity() {
        return quantity;
    }

    public boolean isReviewed() {
        return reviewed;
    }

    public void markReviewed() {
        this.reviewed = true;
    }
}
