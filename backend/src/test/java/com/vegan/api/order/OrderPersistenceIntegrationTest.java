package com.vegan.api.order;

import com.vegan.api.order.dto.OrderCreateRequest;
import com.vegan.api.order.dto.OrderItemRequest;
import com.vegan.api.product.Product;
import com.vegan.api.product.ProductRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@Import(OrderService.class)
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrderPersistenceIntegrationTest {

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("vegan_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.sql.init.mode", () -> "never");
    }

    @Autowired
    OrderService orderService;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    EntityManager entityManager;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Test
    void orderCreationUpdatesStockAndOnlyOwnerCanReadIt() {
        Product product = productRepository.saveAndFlush(new Product(
                "비건 쿠키", "Vegan", 3000,
                "https://example.com/cookie.jpg", "https://example.com/detail.jpg",
                10, 101, "통합 테스트 상품"));

        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(product.getId());
        item.setQuantity(3);

        OrderCreateRequest request = new OrderCreateRequest();
        request.setUserName("주문자");
        request.setPhone("010-0000-0000");
        request.setAddress("서울시");
        request.setItems(List.of(item));

        Orders created = orderService.createOrder(1L, request);
        entityManager.flush();
        entityManager.clear();

        Orders loaded = orderService.getOrderDetail(1L, created.getId());
        assertEquals(9000, loaded.getTotalPrice());
        assertEquals(1, loaded.getItems().size());
        assertEquals(7, productRepository.findById(product.getId()).orElseThrow().getStock());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> orderService.getOrderDetail(2L, created.getId()));
        assertEquals(404, exception.getStatusCode().value());
    }

    @Test
    void comparesNPlusOneWithFetchJoinUsingTheSameData() {
        long userId = 100L;
        int orderCount = 10;
        int itemsPerOrder = 3;

        for (int orderIndex = 0; orderIndex < orderCount; orderIndex++) {
            Orders order = new Orders(userId, "성능 측정 사용자", "010-0000-0000", "서울시");
            for (int itemIndex = 0; itemIndex < itemsPerOrder; itemIndex++) {
                Product product = productRepository.save(new Product(
                        "상품-" + orderIndex + "-" + itemIndex,
                        "Vegan", 1000, "image", "detail",
                        100, 101, "N+1 측정 상품"));
                order.addItem(new OrderItem(product, 1));
            }
            orderRepository.save(order);
        }
        entityManager.flush();
        entityManager.clear();

        Statistics statistics = entityManagerFactory
                .unwrap(SessionFactory.class)
                .getStatistics();
        statistics.setStatisticsEnabled(true);

        statistics.clear();
        List<Orders> normalOrders = orderRepository.findByUserIdOrderByOrderDateDesc(userId);
        accessAllProducts(normalOrders);
        long normalSqlCount = statistics.getPrepareStatementCount();

        entityManager.clear();
        statistics.clear();
        List<Orders> fetchJoinOrders = orderRepository.findByUserIdWithItems(userId);
        accessAllProducts(fetchJoinOrders);
        long fetchJoinSqlCount = statistics.getPrepareStatementCount();

        System.out.printf(
                "N+1 comparison: orders=%d, items=%d, normalSql=%d, fetchJoinSql=%d%n",
                orderCount, orderCount * itemsPerOrder, normalSqlCount, fetchJoinSqlCount);

        assertTrue(normalSqlCount > 1, "일반 조회에서 N+1 SQL이 재현되어야 합니다.");
        assertEquals(1, fetchJoinSqlCount, "fetch join은 연관 데이터를 SQL 한 번으로 조회해야 합니다.");
        assertTrue(fetchJoinSqlCount < normalSqlCount, "fetch join의 SQL 횟수가 더 적어야 합니다.");
    }

    @Test
    void measuresNormalQueryAndFetchJoinElapsedTime() {
        long userId = 200L;
        int orderCount = 100;
        int itemsPerOrder = 3;
        int warmupCount = 5;
        int measurementCount = 20;

        createOrders(userId, orderCount, itemsPerOrder);
        entityManager.flush();
        entityManager.clear();

        for (int i = 0; i < warmupCount; i++) {
            measureNormalQuery(userId);
            measureFetchJoinQuery(userId);
        }

        List<Double> normalSamples = new ArrayList<>();
        List<Double> fetchJoinSamples = new ArrayList<>();
        for (int i = 0; i < measurementCount; i++) {
            if (i % 2 == 0) {
                normalSamples.add(measureNormalQuery(userId));
                fetchJoinSamples.add(measureFetchJoinQuery(userId));
            } else {
                fetchJoinSamples.add(measureFetchJoinQuery(userId));
                normalSamples.add(measureNormalQuery(userId));
            }
        }

        double normalAverage = average(normalSamples);
        double fetchJoinAverage = average(fetchJoinSamples);
        double normalP95 = percentile95(normalSamples);
        double fetchJoinP95 = percentile95(fetchJoinSamples);

        System.out.printf(
                "N+1 timing: orders=%d, items=%d, runs=%d, " +
                        "normalAvgMs=%.3f, normalP95Ms=%.3f, " +
                        "fetchJoinAvgMs=%.3f, fetchJoinP95Ms=%.3f%n",
                orderCount, orderCount * itemsPerOrder, measurementCount,
                normalAverage, normalP95, fetchJoinAverage, fetchJoinP95);

        assertEquals(measurementCount, normalSamples.size());
        assertEquals(measurementCount, fetchJoinSamples.size());
    }

    private void createOrders(long userId, int orderCount, int itemsPerOrder) {
        for (int orderIndex = 0; orderIndex < orderCount; orderIndex++) {
            Orders order = new Orders(userId, "성능 측정 사용자", "010-0000-0000", "서울시");
            for (int itemIndex = 0; itemIndex < itemsPerOrder; itemIndex++) {
                Product product = productRepository.save(new Product(
                        "시간측정상품-" + orderIndex + "-" + itemIndex,
                        "Vegan", 1000, "image", "detail",
                        100, 101, "N+1 시간 측정 상품"));
                order.addItem(new OrderItem(product, 1));
            }
            orderRepository.save(order);
        }
    }

    private double measureNormalQuery(long userId) {
        entityManager.clear();
        long startedAt = System.nanoTime();
        accessAllProducts(orderRepository.findByUserIdOrderByOrderDateDesc(userId));
        return (System.nanoTime() - startedAt) / 1_000_000.0;
    }

    private double measureFetchJoinQuery(long userId) {
        entityManager.clear();
        long startedAt = System.nanoTime();
        accessAllProducts(orderRepository.findByUserIdWithItems(userId));
        return (System.nanoTime() - startedAt) / 1_000_000.0;
    }

    private double average(List<Double> samples) {
        return samples.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
    }

    private double percentile95(List<Double> samples) {
        List<Double> sorted = new ArrayList<>(samples);
        Collections.sort(sorted);
        int index = (int) Math.ceil(sorted.size() * 0.95) - 1;
        return sorted.get(index);
    }

    private void accessAllProducts(List<Orders> orders) {
        orders.forEach(order -> order.getItems().forEach(item -> item.getProduct().getName()));
    }
}
