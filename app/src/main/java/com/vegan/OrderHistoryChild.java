package com.vegan;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

public class OrderHistoryChild {
    private String orderId;
    private int productId;
    private String productName;
    private String productPrice;
    private String totalQuantity;
    private  String orderImg;


    public OrderHistoryChild() { }


    public OrderHistoryChild(String orderId, int productId, String productName, String productPrice, String totalQuantity, String orderImg) {
        this.orderId = orderId;
        this.productId = productId;
        this.productName = productName;
        this.productPrice = productPrice;
        this.totalQuantity = totalQuantity;
        this.orderImg = orderImg;
    }

    public String getOrderId() { return orderId; }

    public void setOrderId(String orderId) { this.orderId = orderId; }


    public int getProductId() { return productId; }

    public void setProductId(int productId) { this.productId = productId; }

    public String getProductName() { return productName; }

    public void setProductName(String productName) { this.productName = productName; }

    public String getProductPrice() { return productPrice; }

    public void setProductPrice(String productPrice) { this.productPrice = productPrice; }

    public String getTotalQuantity() { return totalQuantity; }

    public void setTotalQuantity(String totalQuantity) { this.totalQuantity = totalQuantity; }

    public String getOrderImg() { return orderImg; }

    public void setOrderImg(String orderImg) { this.orderImg = orderImg; }
}