package com.vegan.api;

import java.io.Serializable;

public class ApiCartItem implements Serializable {
    private long id;
    private long productId;
    private String productName;
    private String productBrand;
    private int productPrice;
    private String productImg;
    private int productStock;
    private int selectedQuantity;

    public long getId() { return id; }
    public long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getProductBrand() { return productBrand; }
    public int getProductPrice() { return productPrice; }
    public String getProductImg() { return productImg; }
    public int getProductStock() { return productStock; }
    public int getSelectedQuantity() { return selectedQuantity; }
    public void setSelectedQuantity(int selectedQuantity) { this.selectedQuantity = selectedQuantity; }
}
