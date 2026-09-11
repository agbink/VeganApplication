package com.vegan.api;

/**
 * Spring Boot의 /api/products 응답(JSON)을 그대로 받기 위한 DTO 클래스.
 */
public class ApiProduct implements java.io.Serializable {
    private long id;
    private String name;
    private String brandName;
    private int price;
    private String imageUrl;
    private String detailImageUrl;
    private int stock;
    private int category;
    private String description;
    private int salesCount;

    public long getId() { return id; }
    public String getName() { return name; }
    public String getBrandName() { return brandName; }
    public int getPrice() { return price; }
    public String getImageUrl() { return imageUrl; }
    public String getDetailImageUrl() { return detailImageUrl; }
    public int getStock() { return stock; }
    public int getCategory() { return category; }
    public String getDescription() { return description; }
    public int getSalesCount() { return salesCount; }
}
