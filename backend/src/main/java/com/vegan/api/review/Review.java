package com.vegan.api.review;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vegan.api.user.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "review",
        indexes = @Index(name = "idx_review_product", columnList = "productId"))
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    private Long productId;
    private String productName;
    private String productImg;
    private String username;

    private String content;
    private float rating;
    private String reviewImageUrl;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }

    protected Review() {}

    public Review(User user, Long productId, String productName, String productImg,
                  String content, float rating, String reviewImageUrl) {
        this.user = user;
        this.productId = productId;
        this.productName = productName;
        this.productImg = productImg;
        this.username = user.getUsername();
        this.content = content;
        this.rating = rating;
        this.reviewImageUrl = reviewImageUrl;
    }

    public Long getId() { return id; }
    public Long getUserId() { return user.getId(); }
    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getProductImg() { return productImg; }
    public String getUsername() { return username; }
    public String getContent() { return content; }
    public float getRating() { return rating; }
    public String getReviewImageUrl() { return reviewImageUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
