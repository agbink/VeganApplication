package com.vegan.api;

/**
 * Spring Boot의 OrderItem 응답을 받기 위한 DTO.
 * 서버가 product 필드에 전체 Product 객체를 그대로 내려주기 때문에
 * ApiProduct를 그대로 재사용해서 받습니다.
 */
public class ApiOrderItem {
    private long id;
    private ApiProduct product;
    private String productNameSnapshot;
    private int priceAtOrder;
    private String imageUrlSnapshot;
    private int quantity;
    private boolean reviewed;

    public long getId() { return id; }
    public ApiProduct getProduct() { return product; }
    public String getProductNameSnapshot() { return productNameSnapshot; }
    public int getPriceAtOrder() { return priceAtOrder; }
    public String getImageUrlSnapshot() { return imageUrlSnapshot; }
    public int getQuantity() { return quantity; }
    public boolean isReviewed() { return reviewed; }
}
