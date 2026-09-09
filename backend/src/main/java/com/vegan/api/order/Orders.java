package com.vegan.api.order;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "orders",
        indexes = {
                @Index(name = "idx_orders_uid_date", columnList = "userId, orderDate DESC")
        }
)
public class Orders {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;   // JWT 기반 사용자 ID

    private String userName;
    private String phone;
    private String address;

    private int totalPrice;
    private String orderState;
    private LocalDateTime orderDate;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    protected Orders() {
    }

    public Orders(Long userId, String userName, String phone, String address) {
        this.userId = userId;
        this.userName = userName;
        this.phone = phone;
        this.address = address;
        this.orderState = "paid";
        this.orderDate = LocalDateTime.now();
        this.totalPrice = 0;
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.assignOrder(this);
        this.totalPrice += item.getPriceAtOrder() * item.getQuantity();
    }

    public void setOrderState(String orderState) {
        this.orderState = orderState;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public int getTotalPrice() {
        return totalPrice;
    }

    public String getOrderState() {
        return orderState;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public List<OrderItem> getItems() {
        return items;
    }
}
