package com.vegan.api.product;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "product",
        indexes = {
                @Index(name = "idx_product_category_created", columnList = "category, createdAt DESC"),
                @Index(name = "idx_product_sales_count", columnList = "salesCount DESC")
        })
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String brandName;

    @Column(nullable = false)
    private int price;

    private String imageUrl;
    private String detailImageUrl;

    private int stock;
    private int category;

    @Column(length = 1000)
    private String description;

    private int salesCount;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Product(String name, String brandName, int price,
                   String imageUrl, String detailImageUrl,
                   int stock, int category, String description) {
        this.name = name;
        this.brandName = brandName;
        this.price = price;
        this.imageUrl = imageUrl;
        this.detailImageUrl = detailImageUrl;
        this.stock = stock;
        this.category = category;
        this.description = description;
        this.salesCount = 0;
    }

    // 주문(Order) 생성 시 호출됨 - @Getter만 있고 setter가 없어서,
    // 의미 있는 비즈니스 메서드로 상태를 바꿉니다.
    public void decreaseStock(int quantity) {
        if (this.stock < quantity) {
            throw new IllegalStateException("재고가 부족합니다: " + this.name);
        }
        this.stock -= quantity;
    }

    public void increaseSalesCount(int quantity) {
        this.salesCount += quantity;
    }

    // 어드민 상품 수정
    public void update(String name, String brandName, int price,
                       String imageUrl, String detailImageUrl,
                       int stock, int category, String description, int salesCount) {
        this.name = name;
        this.brandName = brandName;
        this.price = price;
        this.imageUrl = imageUrl;
        this.detailImageUrl = detailImageUrl;
        this.stock = stock;
        this.category = category;
        this.description = description;
        this.salesCount = salesCount;
    }
}