package com.vegan.api.order;

import com.vegan.api.order.dto.OrderCreateRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // POST /api/orders -> 주문 생성 (JWT 필요)
    @PostMapping
    public Orders createOrder(@RequestAttribute(required = false) Long userId,
                              @Valid @RequestBody OrderCreateRequest request) {
        if (userId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        return orderService.createOrder(userId, request);
    }

    // GET /api/orders -> 내 주문 목록 (JWT 필요, 최신순)
    @GetMapping
    public List<Orders> getOrders(@RequestAttribute(required = false) Long userId) {
        if (userId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        return orderService.getOrderHistory(userId);
    }

    // GET /api/orders/{id} -> 주문 상세
    @GetMapping("/{id}")
    public Orders getOrder(@RequestAttribute(required = false) Long userId,
                           @PathVariable Long id) {
        if (userId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        return orderService.getOrderDetail(userId, id);
    }
}
