package com.vegan.api.order;

import com.vegan.api.order.dto.OrderCreateRequest;
import com.vegan.api.order.dto.OrderItemRequest;
import com.vegan.api.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

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
}
