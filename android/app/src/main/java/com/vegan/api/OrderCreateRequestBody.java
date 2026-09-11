package com.vegan.api;

import java.util.List;

/**
 * POST /api/orders 로 보낼 요청 바디.
 * 서버의 OrderCreateRequest와 필드 이름을 똑같이 맞춰야 Gson이 정확히 변환합니다.
 */
public class OrderCreateRequestBody {
    private String userName;
    private String phone;
    private String address;
    private List<OrderItemRequestBody> items;

    public OrderCreateRequestBody(String userName, String phone,
                                   String address, List<OrderItemRequestBody> items) {
        this.userName = userName;
        this.phone = phone;
        this.address = address;
        this.items = items;
    }
}
