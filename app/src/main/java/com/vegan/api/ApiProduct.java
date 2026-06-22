package com.vegan.api;

/**
 * Spring Boot의 /api/products 응답(JSON)을 그대로 받기 위한 DTO 클래스.
 * 기존 ItemsDomain과 필드 이름이 다르니, 화면에 쓸 때는 ItemsDomain으로 변환해서 사용하세요.
 */
public class ApiProduct {
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
