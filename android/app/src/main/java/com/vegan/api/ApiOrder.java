package com.vegan.api;

import java.util.List;

public class ApiOrder {
    private long id;
    private long userId;
    private String userName;
    private String phone;
    private String address;
    private int totalPrice;
    private String orderState;
    private String orderDate;
    private List<ApiOrderItem> items;

    public long getId() { return id; }
    public long getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public int getTotalPrice() { return totalPrice; }
    public String getOrderState() { return orderState; }
    public String getOrderDate() { return orderDate; }
    public List<ApiOrderItem> getItems() { return items; }
}
