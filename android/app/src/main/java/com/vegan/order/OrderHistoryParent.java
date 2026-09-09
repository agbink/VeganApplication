package com.vegan.order;

import java.util.ArrayList;

public class OrderHistoryParent {
    private String orderDate;
    private ArrayList<OrderHistoryChild> orderHistoryChildList;

    public OrderHistoryParent() {}

    public String getOrderDate() { return orderDate; }

    public void setOrderDate(String orderDate) { this.orderDate = orderDate; }

    public ArrayList<OrderHistoryChild> getOrderHistoryChildList() {
        return orderHistoryChildList;
    }

    public void setOrderHistoryChildList(ArrayList<OrderHistoryChild> orderHistoryChildList) {
        this.orderHistoryChildList = orderHistoryChildList;
    }

}