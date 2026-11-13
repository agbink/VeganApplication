package com.vegan.api.cart;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vegan.api.cart.dto.CartAddRequest;
import com.vegan.api.product.Product;
import com.vegan.api.product.ProductRepository;
import com.vegan.api.user.User;
import com.vegan.api.user.UserRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 같은 사용자가 같은 상품을 동시에 여러 번 담아도 장바구니 행은 하나이고 수량이 모두 더해지는지 검증한다.
 * (버튼 연타처럼 요청이 겹치는 상황)
 */
@Tag("concurrency")
@DataJpaTest
@Import(CartService.class)
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED) // 각 요청이 실제로 커밋되어야 동시성 재현 가능
class CartConcurrencyIntegrationTest {

    private static final int CONCURRENT_REQUESTS = 10;
    private static final int ROUNDS = 10;

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("vegan_cart_concurrency_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.sql.init.mode", () -> "never");
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> String.valueOf(CONCURRENT_REQUESTS + 2));
    }

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    CartService cartService;

    @Autowired
    CartItemRepository cartItemRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    UserRepository userRepository;

    @Test
    void concurrentAddsOfSameProductMergeIntoOneRow() throws Exception {
        for (int round = 1; round <= ROUNDS; round++) {
            User user = userRepository.save(new User(
                    "cart-concurrency-" + round + "@example.com", "encoded", "사용자", "010-0000-0000", "서울"));
            Product product = productRepository.save(new Product(
                    "동시담기 상품 " + round, "테스트브랜드", 5_000, "image", "detail", 100, 101, "동시성 테스트"));

            List<Throwable> errors = addConcurrently(user.getId(), product.getId());

            List<CartItem> cart = cartItemRepository.findByUser_Id(user.getId());
            assertTrue(errors.isEmpty(), "round " + round + ": 동시 담기 요청이 실패하면 안 됩니다: " + errors);
            assertEquals(1, cart.size(), "round " + round + ": 같은 상품은 한 줄이어야 합니다.");
            assertEquals(CONCURRENT_REQUESTS, cart.get(0).getSelectedQuantity(),
                    "round " + round + ": 수량은 요청 수만큼 더해져야 합니다.");
        }
    }

    private List<Throwable> addConcurrently(Long userId, Long productId) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_REQUESTS);
        CountDownLatch ready = new CountDownLatch(CONCURRENT_REQUESTS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    cartService.addItem(userId, OBJECT_MAPPER.convertValue(
                            Map.of("productId", productId, "selectedQuantity", 1), CartAddRequest.class));
                    return null;
                }));
            }
            ready.await(10, TimeUnit.SECONDS);
            start.countDown();

            List<Throwable> errors = new ArrayList<>();
            for (Future<?> future : futures) {
                try {
                    future.get(30, TimeUnit.SECONDS);
                } catch (Exception e) {
                    errors.add(e.getCause() != null ? e.getCause() : e);
                }
            }
            return errors;
        } finally {
            executor.shutdownNow();
        }
    }
}
