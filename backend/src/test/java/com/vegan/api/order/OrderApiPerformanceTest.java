package com.vegan.api.order;

import com.vegan.api.product.Product;
import com.vegan.api.product.ProductRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@Tag("performance")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({OrderApiPerformanceTest.BenchmarkController.class,
        OrderApiPerformanceTest.BenchmarkService.class})
class OrderApiPerformanceTest {

    private static final long USER_ID = 300L;
    private static final int ORDER_COUNT = 100;
    private static final int ITEMS_PER_ORDER = 3;
    private static final int WARMUP_COUNT = 5;
    private static final int MEASUREMENT_COUNT = 20;

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("vegan_api_performance_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.sql.init.mode", () -> "never");
        registry.add("jwt.secret", () -> "performance-test-secret-key-at-least-256-bits-long");
        registry.add("google.client-id", () -> "performance-test-client-id");
        registry.add("file.upload-dir", () -> "build/test-uploads");
        registry.add("file.base-url", () -> "http://localhost");
    }

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Test
    void measuresCompleteHttpResponseForNormalQueryAndFetchJoin() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> createOrders());

        verifySameResponse();
        for (int i = 0; i < WARMUP_COUNT; i++) {
            request("normal");
            request("fetch-join");
        }

        List<Double> normalSamples = new ArrayList<>();
        List<Double> fetchJoinSamples = new ArrayList<>();
        for (int i = 0; i < MEASUREMENT_COUNT; i++) {
            if (i % 2 == 0) {
                normalSamples.add(measure("normal"));
                fetchJoinSamples.add(measure("fetch-join"));
            } else {
                fetchJoinSamples.add(measure("fetch-join"));
                normalSamples.add(measure("normal"));
            }
        }

        System.out.printf(
                "N+1 API timing: orders=%d, items=%d, runs=%d, " +
                        "normalAvgMs=%.3f, normalP95Ms=%.3f, " +
                        "fetchJoinAvgMs=%.3f, fetchJoinP95Ms=%.3f%n",
                ORDER_COUNT, ORDER_COUNT * ITEMS_PER_ORDER, MEASUREMENT_COUNT,
                average(normalSamples), percentile95(normalSamples),
                average(fetchJoinSamples), percentile95(fetchJoinSamples));

        assertEquals(MEASUREMENT_COUNT, normalSamples.size());
        assertEquals(MEASUREMENT_COUNT, fetchJoinSamples.size());
    }

    private void createOrders() {
        for (int orderIndex = 0; orderIndex < ORDER_COUNT; orderIndex++) {
            Orders order = new Orders(USER_ID, "API 성능 측정 사용자", "010-0000-0000", "서울시");
            for (int itemIndex = 0; itemIndex < ITEMS_PER_ORDER; itemIndex++) {
                Product product = productRepository.save(new Product(
                        "API측정상품-" + orderIndex + "-" + itemIndex,
                        "Vegan", 1000, "image", "detail",
                        100, 101, "API 응답시간 측정 상품"));
                order.addItem(new OrderItem(product, 1));
            }
            orderRepository.save(order);
        }
    }

    private void verifySameResponse() {
        String normalBody = request("normal");
        String fetchJoinBody = request("fetch-join");
        assertFalse(normalBody.isBlank());
        assertEquals(normalBody, fetchJoinBody, "두 조회 방식은 동일한 JSON을 반환해야 합니다.");
    }

    private double measure(String path) {
        long startedAt = System.nanoTime();
        request(path);
        return (System.nanoTime() - startedAt) / 1_000_000.0;
    }

    private String request(String path) {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/benchmark/orders/" + path + "/" + USER_ID,
                String.class);
        assertEquals(200, response.getStatusCode().value());
        return response.getBody();
    }

    private double average(List<Double> samples) {
        return samples.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
    }

    private double percentile95(List<Double> samples) {
        List<Double> sorted = new ArrayList<>(samples);
        Collections.sort(sorted);
        return sorted.get((int) Math.ceil(sorted.size() * 0.95) - 1);
    }

    record OrderResponse(Long id, int totalPrice, String orderState,
                         List<OrderItemResponse> items) {
    }

    record OrderItemResponse(Long id, String productName, int price, int quantity) {
    }

    @RestController
    @RequestMapping("/benchmark/orders")
    static class BenchmarkController {

        private final BenchmarkService benchmarkService;

        BenchmarkController(BenchmarkService benchmarkService) {
            this.benchmarkService = benchmarkService;
        }

        @GetMapping("/normal/{userId}")
        List<OrderResponse> normal(@PathVariable Long userId) {
            return benchmarkService.findNormal(userId);
        }

        @GetMapping("/fetch-join/{userId}")
        List<OrderResponse> fetchJoin(@PathVariable Long userId) {
            return benchmarkService.findWithFetchJoin(userId);
        }
    }

    @Service
    static class BenchmarkService {

        private final OrderRepository orderRepository;

        BenchmarkService(OrderRepository orderRepository) {
            this.orderRepository = orderRepository;
        }

        @Transactional(readOnly = true)
        public List<OrderResponse> findNormal(Long userId) {
            return toResponse(orderRepository.findByUserIdOrderByOrderDateDesc(userId));
        }

        @Transactional(readOnly = true)
        public List<OrderResponse> findWithFetchJoin(Long userId) {
            return toResponse(orderRepository.findByUserIdWithItems(userId));
        }

        private List<OrderResponse> toResponse(List<Orders> orders) {
            return orders.stream()
                    .map(order -> new OrderResponse(
                            order.getId(), order.getTotalPrice(), order.getOrderState(),
                            order.getItems().stream()
                                    .sorted(Comparator.comparing(OrderItem::getId))
                                    .map(item -> new OrderItemResponse(
                                            item.getId(), item.getProduct().getName(),
                                            item.getPriceAtOrder(), item.getQuantity()))
                                    .toList()))
                    .toList();
        }
    }
}
