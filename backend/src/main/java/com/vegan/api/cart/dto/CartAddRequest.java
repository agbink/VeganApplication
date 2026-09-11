package com.vegan.api.cart.dto;

public class CartAddRequest {
    private Long productId;
    private String productName;
    private String productBrand;
    private int productPrice;
    private String productImg;
    private int productStock;
    private int selectedQuantity;

    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getProductBrand() { return productBrand; }
    public int getProductPrice() { return productPrice; }
    public String getProductImg() { return productImg; }
    public int getProductStock() { return productStock; }
    public int getSelectedQuantity() { return selectedQuantity; }
}
