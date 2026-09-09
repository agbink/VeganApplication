package com.vegan.api.order;

import com.vegan.api.order.dto.OrderCreateRequest;
import com.vegan.api.order.dto.OrderItemRequest;
import com.vegan.api.product.Product;
import com.vegan.api.product.ProductRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    // 주문 시 재고/판매량이 바뀌므로 상품 목록 캐시 무효화
    @CacheEvict(value = "products", allEntries = true)
    @Transactional
    public Orders createOrder(Long userId, OrderCreateRequest request) {
        Orders order = new Orders(
                userId,
                request.getUserName(),
                request.getPhone(),
                request.getAddress()
        );

        for (OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "상품을 찾을 수 없습니다. id=" + itemRequest.getProductId()));

            if (product.getStock() < itemRequest.getQuantity()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "재고가 부족합니다: " + product.getName());
            }

            OrderItem orderItem = new OrderItem(product, itemRequest.getQuantity());
            order.addItem(orderItem);

            product.decreaseStock(itemRequest.getQuantity());
            product.increaseSalesCount(itemRequest.getQuantity());
        }

        return orderRepository.save(order);
    }

    public List<Orders> getOrderHistory(Long userId) {
        return orderRepository.findByUserIdWithItems(userId);
    }

    public Orders getOrderDetail(Long orderId) {
        return orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다. id=" + orderId));
    }
}
