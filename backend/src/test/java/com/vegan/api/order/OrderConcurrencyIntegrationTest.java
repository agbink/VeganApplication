package com.vegan.api.order;

import com.vegan.api.order.dto.OrderCreateRequest;
import com.vegan.api.order.dto.OrderItemRequest;
import com.vegan.api.product.Product;
import com.vegan.api.product.ProductRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("performance")
@Testcontainers
@SpringBootTest
class OrderConcurrencyIntegrationTest {

    private static final int ROUNDS = 30;
    private static final int INITIAL_STOCK = 10;
    private static final int CONCURRENT_REQUESTS = 20;

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("vegan_concurrency_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "25");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.sql.init.mode", () -> "never");
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("jwt.secret", () -> "concurrency-test-secret-key-at-least-256-bits-long");
        registry.add("google.client-id", () -> "concurrency-test-client-id");
        registry.add("file.upload-dir", () -> "build/test-uploads");
        registry.add("file.base-url", () -> "http://localhost");
    }

    @Autowired
    OrderService orderService;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void comparesNoLockWithPessimisticLockUnderConcurrentOrders() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_REQUESTS);
        int noLockInconsistentRounds = 0;
        int lockedInconsistentRounds = 0;
        int lockedSuccessTotal = 0;
        int lockedRejectedTotal = 0;
        long startedAt = System.nanoTime();

        try {
            for (int round = 1; round <= ROUNDS; round++) {
                RoundResult noLock = runNoLockRound(executor, round);
                RoundResult locked = runLockedRound(executor, round);

                if (!noLock.isConsistent()) {
                    noLockInconsistentRounds++;
                }
                if (!locked.isConsistent()) {
                    lockedInconsistentRounds++;
                }

                lockedSuccessTotal += locked.successCount();
                lockedRejectedTotal += locked.rejectedCount();

                assertEquals(INITIAL_STOCK, locked.successCount(),
                        "비관적 락 적용 후 성공 주문 수가 초기 재고와 같아야 합니다. round=" + round);
                assertEquals(CONCURRENT_REQUESTS - INITIAL_STOCK, locked.rejectedCount(),
                        "재고를 초과한 주문은 거절되어야 합니다. round=" + round);
                assertEquals(0, locked.finalStock(),
                        "성공 주문 후 최종 재고는 0이어야 합니다. round=" + round);
                assertEquals(INITIAL_STOCK, locked.salesCount(),
                        "판매량과 성공 주문 수가 같아야 합니다. round=" + round);
                assertEquals(INITIAL_STOCK, locked.persistedOrderCount(),
                        "DB 주문 수와 성공 주문 수가 같아야 합니다. round=" + round);
            }
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }

        double elapsedMs = (System.nanoTime() - startedAt) / 1_000_000.0;
        System.out.printf(
                "Order concurrency comparison: rounds=%d, requestsPerRound=%d, initialStock=%d, " +
                        "noLockInconsistentRounds=%d, lockedInconsistentRounds=%d, " +
                        "lockedSuccessTotal=%d, lockedRejectedTotal=%d, elapsedMs=%.3f%n",
                ROUNDS, CONCURRENT_REQUESTS, INITIAL_STOCK,
                noLockInconsistentRounds, lockedInconsistentRounds,
                lockedSuccessTotal, lockedRejectedTotal, elapsedMs);

        assertTrue(noLockInconsistentRounds > 0,
                "락이 없는 기준 로직에서 동시성 정합성 문제가 한 번 이상 재현되어야 합니다.");
        assertEquals(0, lockedInconsistentRounds,
                "비관적 락 적용 후 모든 회차에서 주문과 재고 정합성이 유지되어야 합니다.");
    }

    private RoundResult runNoLockRound(ExecutorService executor, int round) throws Exception {
        Product product = createProduct("무잠금-" + round);
        CyclicBarrier afterReadBarrier = new CyclicBarrier(CONCURRENT_REQUESTS);
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        List<Future<AttemptResult>> futures = new ArrayList<>();

        for (int requestIndex = 0; requestIndex < CONCURRENT_REQUESTS; requestIndex++) {
            long userId = 1_000_000L + round * 100L + requestIndex;
            futures.add(executor.submit(() -> {
                try {
                    return transactionTemplate.execute(status -> createWithoutLock(
                            userId, product.getId(), afterReadBarrier));
                } catch (Exception exception) {
                    return AttemptResult.unexpected(exception);
                }
            }));
        }

        return collectRoundResult(product.getId(), futures);
    }

    private AttemptResult createWithoutLock(long userId, long productId, CyclicBarrier afterReadBarrier) {
        Product product = productRepository.findById(productId).orElseThrow();
        awaitBarrier(afterReadBarrier);

        if (product.getStock() < 1) {
            return AttemptResult.rejectedAttempt();
        }

        Orders order = new Orders(userId, "무잠금 주문자", "010-0000-0000", "서울시");
        order.addItem(new OrderItem(product, 1));
        product.decreaseStock(1);
        product.increaseSalesCount(1);
        orderRepository.save(order);
        return AttemptResult.succeeded();
    }

    private RoundResult runLockedRound(ExecutorService executor, int round) throws Exception {
        Product product = createProduct("비관적락-" + round);
        CyclicBarrier startBarrier = new CyclicBarrier(CONCURRENT_REQUESTS);
        List<Future<AttemptResult>> futures = new ArrayList<>();

        for (int requestIndex = 0; requestIndex < CONCURRENT_REQUESTS; requestIndex++) {
            long userId = 2_000_000L + round * 100L + requestIndex;
            futures.add(executor.submit(() -> {
                try {
                    awaitBarrier(startBarrier);
                    orderService.createOrder(userId, orderRequest(product.getId()));
                    return AttemptResult.succeeded();
                } catch (ResponseStatusException exception) {
                    if (exception.getStatusCode().value() == 400) {
                        return AttemptResult.rejectedAttempt();
                    }
                    return AttemptResult.unexpected(exception);
                } catch (Exception exception) {
                    return AttemptResult.unexpected(exception);
                }
            }));
        }

        return collectRoundResult(product.getId(), futures);
    }

    private RoundResult collectRoundResult(long productId,
                                           List<Future<AttemptResult>> futures) throws Exception {
        int successCount = 0;
        int rejectedCount = 0;
        List<Throwable> unexpectedErrors = new ArrayList<>();

        for (Future<AttemptResult> future : futures) {
            AttemptResult result = future.get(30, TimeUnit.SECONDS);
            if (result.success()) {
                successCount++;
            } else if (result.rejected()) {
                rejectedCount++;
            } else {
                unexpectedErrors.add(result.error());
            }
        }

        assertTrue(unexpectedErrors.isEmpty(),
                () -> "예상하지 못한 동시 주문 오류: " + unexpectedErrors);

        Product savedProduct = productRepository.findById(productId).orElseThrow();
        Integer persistedOrderCount = jdbcTemplate.queryForObject(
                "select count(distinct order_id) from order_item where product_id = ?",
                Integer.class, productId);

        return new RoundResult(
                successCount,
                rejectedCount,
                savedProduct.getStock(),
                savedProduct.getSalesCount(),
                persistedOrderCount == null ? 0 : persistedOrderCount);
    }

    private Product createProduct(String roundName) {
        return productRepository.save(new Product(
                "동시성측정상품-" + roundName,
                "Vegan", 1_000, "image", "detail",
                INITIAL_STOCK, 101, "주문 동시성 측정 상품"));
    }

    private OrderCreateRequest orderRequest(long productId) {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(productId);
        item.setQuantity(1);

        OrderCreateRequest request = new OrderCreateRequest();
        request.setUserName("동시 주문자");
        request.setPhone("010-0000-0000");
        request.setAddress("서울시");
        request.setItems(List.of(item));
        return request;
    }

    private void awaitBarrier(CyclicBarrier barrier) {
        try {
            barrier.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("동시 주문 대기 중 스레드가 중단됐습니다.", exception);
        } catch (BrokenBarrierException | TimeoutException exception) {
            throw new IllegalStateException("동시 주문 스레드가 제한 시간 안에 모이지 못했습니다.", exception);
        }
    }

    record AttemptResult(boolean success, boolean rejected, Throwable error) {
        static AttemptResult succeeded() {
            return new AttemptResult(true, false, null);
        }

        static AttemptResult rejectedAttempt() {
            return new AttemptResult(false, true, null);
        }

        static AttemptResult unexpected(Throwable error) {
            return new AttemptResult(false, false, error);
        }
    }

    record RoundResult(int successCount, int rejectedCount, int finalStock,
                       int salesCount, int persistedOrderCount) {
        boolean isConsistent() {
            return successCount == INITIAL_STOCK
                    && rejectedCount == CONCURRENT_REQUESTS - INITIAL_STOCK
                    && finalStock == 0
                    && salesCount == successCount
                    && persistedOrderCount == successCount;
        }
    }
}
