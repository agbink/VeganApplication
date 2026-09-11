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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "주문 상품이 필요합니다.");
        }

        for (OrderItemRequest itemRequest : request.getItems()) {
            if (itemRequest.getProductId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "상품 ID가 필요합니다.");
            }
            if (itemRequest.getQuantity() < 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "주문 수량은 1개 이상이어야 합니다.");
            }
        }

        // 여러 상품을 주문해도 모든 트랜잭션이 같은 순서로 잠그도록 ID 오름차순 정렬
        List<OrderItemRequest> sortedItems = new ArrayList<>(request.getItems());
        sortedItems.sort(Comparator.comparing(OrderItemRequest::getProductId));

        Map<Long, Product> lockedProducts = new HashMap<>();
        for (OrderItemRequest itemRequest : sortedItems) {
            if (!lockedProducts.containsKey(itemRequest.getProductId())) {
                Product product = productRepository.findByIdForUpdate(itemRequest.getProductId())
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "상품을 찾을 수 없습니다. id=" + itemRequest.getProductId()));
                lockedProducts.put(itemRequest.getProductId(), product);
            }
        }

        Orders order = new Orders(
                userId,
                request.getUserName(),
                request.getPhone(),
                request.getAddress()
        );

        // 주문 내 표시 순서는 사용자가 보낸 순서를 유지합니다.
        for (OrderItemRequest itemRequest : request.getItems()) {
            Product product = lockedProducts.get(itemRequest.getProductId());

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

    public Orders getOrderDetail(Long userId, Long orderId) {
        return orderRepository.findByIdAndUserIdWithItems(orderId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다. id=" + orderId));
    }
}
