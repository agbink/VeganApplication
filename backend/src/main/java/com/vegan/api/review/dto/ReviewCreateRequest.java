package com.vegan.api.review.dto;

public class ReviewCreateRequest {
    private Long productId;
    private String productName;
    private String productImg;
    private String content;
    private float rating;
    private String reviewImageUrl;

    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getProductImg() { return productImg; }
    public String getContent() { return content; }
    public float getRating() { return rating; }
    public String getReviewImageUrl() { return reviewImageUrl; }
}
