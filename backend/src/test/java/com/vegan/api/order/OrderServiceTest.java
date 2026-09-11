package com.vegan.api.order;

import com.vegan.api.order.dto.OrderCreateRequest;
import com.vegan.api.order.dto.OrderItemRequest;
import com.vegan.api.product.Product;
import com.vegan.api.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;
import org.mockito.InOrder;

class OrderServiceTest {

    @Test
    void rejectsNonPositiveQuantity() {
        OrderRepository orderRepository = mock(OrderRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        OrderService service = new OrderService(orderRepository, productRepository);
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(1L);
        item.setQuantity(-1);
        OrderCreateRequest request = new OrderCreateRequest();
        request.setUserName("user");
        request.setPhone("010-0000-0000");
        request.setAddress("address");
        request.setItems(List.of(item));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.createOrder(1L, request));
        assertEquals(400, exception.getStatusCode().value());
    }

    @Test
    void locksProductsInAscendingIdOrder() {
        OrderRepository orderRepository = mock(OrderRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        OrderService service = new OrderService(orderRepository, productRepository);

        Product product10 = new Product("상품10", "Vegan", 1_000,
                "image", "detail", 10, 101, "잠금 순서 테스트");
        Product product20 = new Product("상품20", "Vegan", 1_000,
                "image", "detail", 10, 101, "잠금 순서 테스트");
        when(productRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(product10));
        when(productRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(product20));

        OrderItemRequest item20 = orderItem(20L);
        OrderItemRequest item10 = orderItem(10L);
        OrderCreateRequest request = orderRequest(List.of(item20, item10));

        service.createOrder(1L, request);

        InOrder lockOrder = inOrder(productRepository);
        lockOrder.verify(productRepository).findByIdForUpdate(10L);
        lockOrder.verify(productRepository).findByIdForUpdate(20L);
    }

    private OrderItemRequest orderItem(long productId) {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(productId);
        item.setQuantity(1);
        return item;
    }

    private OrderCreateRequest orderRequest(List<OrderItemRequest> items) {
        OrderCreateRequest request = new OrderCreateRequest();
        request.setUserName("user");
        request.setPhone("010-0000-0000");
        request.setAddress("address");
        request.setItems(items);
        return request;
    }
}
