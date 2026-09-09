package com.vegan.api;

public class ApiReview {
    private long id;
    private long productId;
    private String productName;
    private String productImg;
    private String username;
    private String content;
    private float rating;
    private String reviewImageUrl;
    private String createdAt;

    public long getId() { return id; }
    public long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getProductImg() { return productImg; }
    public String getUsername() { return username; }
    public String getContent() { return content; }
    public float getRating() { return rating; }
    public String getReviewImageUrl() { return reviewImageUrl; }
    public String getCreatedAt() { return createdAt; }
}
