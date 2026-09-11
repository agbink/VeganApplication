package com.vegan.api;

public class OrderItemRequestBody {
    private long productId;
    private int quantity;

    public OrderItemRequestBody(long productId, int quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }
}
