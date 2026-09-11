package com.vegan.api.order;

import com.vegan.api.order.dto.OrderCreateRequest;
import com.vegan.api.order.dto.OrderItemRequest;
import com.vegan.api.product.Product;
import com.vegan.api.product.ProductRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
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
class MultiProductOrderConcurrencyTest {

    private static final int ROUNDS = 30;

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("vegan_multi_product_lock_test")
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
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "10");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.sql.init.mode", () -> "never");
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("jwt.secret", () -> "multi-product-test-secret-key-at-least-256-bits-long");
        registry.add("google.client-id", () -> "multi-product-test-client-id");
        registry.add("file.upload-dir", () -> "build/test-uploads");
        registry.add("file.base-url", () -> "http://localhost");
    }

    @Autowired
    OrderService orderService;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void comparesOppositeLockOrderWithSortedProductionOrder() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        int unsortedDeadlockRounds = 0;
        int unsortedConflictTotal = 0;
        int sortedDeadlockRounds = 0;
        int sortedSuccessTotal = 0;

        try {
            for (int round = 1; round <= ROUNDS; round++) {
                int baselineConflicts = runOppositeLockBaseline(executor, round);
                if (baselineConflicts > 0) {
                    unsortedDeadlockRounds++;
                }
                unsortedConflictTotal += baselineConflicts;

                SortedRoundResult sorted = runSortedProductionOrders(executor, round);
                if (sorted.conflictCount() > 0) {
                    sortedDeadlockRounds++;
                }
                sortedSuccessTotal += sorted.successCount();

                assertEquals(0, sorted.conflictCount(),
                        "잠금 순서 정렬 후 데드락이 없어야 합니다. round=" + round);
                assertEquals(2, sorted.successCount(),
                        "A→B와 B→A 주문이 모두 성공해야 합니다. round=" + round);
                assertEquals(0, sorted.productAFinalStock(),
                        "상품 A의 최종 재고가 정확해야 합니다. round=" + round);
                assertEquals(0, sorted.productBFinalStock(),
                        "상품 B의 최종 재고가 정확해야 합니다. round=" + round);
                assertEquals(2, sorted.productASalesCount());
                assertEquals(2, sorted.productBSalesCount());
                assertEquals(2, sorted.persistedOrderCount());
            }
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }

        System.out.printf(
                "Multi-product lock ordering: rounds=%d, " +
                        "unsortedDeadlockRounds=%d, unsortedConflictTotal=%d, " +
                        "sortedDeadlockRounds=%d, sortedSuccessTotal=%d%n",
                ROUNDS, unsortedDeadlockRounds, unsortedConflictTotal,
                sortedDeadlockRounds, sortedSuccessTotal);

        assertTrue(unsortedDeadlockRounds > 0,
                "반대 순서로 잠그는 기준 로직에서 데드락이 재현되어야 합니다.");
        assertEquals(0, sortedDeadlockRounds,
                "상품 ID 순으로 잠근 실제 주문 로직에서는 데드락이 없어야 합니다.");
        assertEquals(ROUNDS * 2, sortedSuccessTotal,
                "정렬 적용 후 모든 주문이 성공해야 합니다.");
    }

    private int runOppositeLockBaseline(ExecutorService executor, int round) throws Exception {
        Product productA = createProduct("기준-A-" + round);
        Product productB = createProduct("기준-B-" + round);
        CyclicBarrier afterFirstLock = new CyclicBarrier(2);

        Future<LockAttempt> first = executor.submit(() -> lockInGivenOrder(
                productA.getId(), productB.getId(), afterFirstLock));
        Future<LockAttempt> second = executor.submit(() -> lockInGivenOrder(
                productB.getId(), productA.getId(), afterFirstLock));

        List<LockAttempt> attempts = List.of(
                first.get(30, TimeUnit.SECONDS),
                second.get(30, TimeUnit.SECONDS));

        List<Throwable> unexpected = attempts.stream()
                .map(LockAttempt::unexpectedError)
                .filter(error -> error != null)
                .toList();
        assertTrue(unexpected.isEmpty(), () -> "기준 로직의 예상하지 못한 오류: " + unexpected);

        return (int) attempts.stream().filter(LockAttempt::conflict).count();
    }

    private LockAttempt lockInGivenOrder(long firstProductId, long secondProductId,
                                         CyclicBarrier afterFirstLock) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        try {
            transactionTemplate.executeWithoutResult(status -> {
                productRepository.findByIdForUpdate(firstProductId).orElseThrow();
                awaitBarrier(afterFirstLock);
                productRepository.findByIdForUpdate(secondProductId).orElseThrow();
            });
            return LockAttempt.completed();
        } catch (CannotAcquireLockException exception) {
            return LockAttempt.conflicted();
        } catch (Exception exception) {
            return LockAttempt.unexpected(exception);
        }
    }

    private SortedRoundResult runSortedProductionOrders(ExecutorService executor, int round)
            throws Exception {
        Product productA = createProduct("정렬-A-" + round);
        Product productB = createProduct("정렬-B-" + round);
        CyclicBarrier startBarrier = new CyclicBarrier(2);

        Future<OrderAttempt> first = executor.submit(() -> createOrder(
                3_000_000L + round * 10L,
                List.of(productA.getId(), productB.getId()), startBarrier));
        Future<OrderAttempt> second = executor.submit(() -> createOrder(
                3_000_001L + round * 10L,
                List.of(productB.getId(), productA.getId()), startBarrier));

        List<OrderAttempt> attempts = List.of(
                first.get(30, TimeUnit.SECONDS),
                second.get(30, TimeUnit.SECONDS));
        List<Throwable> unexpected = attempts.stream()
                .map(OrderAttempt::unexpectedError)
                .filter(error -> error != null)
                .toList();
        assertTrue(unexpected.isEmpty(), () -> "정렬 적용 주문의 예상하지 못한 오류: " + unexpected);

        int successCount = (int) attempts.stream().filter(OrderAttempt::success).count();
        int conflictCount = (int) attempts.stream().filter(OrderAttempt::conflict).count();
        Product savedA = productRepository.findById(productA.getId()).orElseThrow();
        Product savedB = productRepository.findById(productB.getId()).orElseThrow();
        Integer persistedOrderCount = jdbcTemplate.queryForObject(
                "select count(distinct order_id) from order_item where product_id in (?, ?)",
                Integer.class, productA.getId(), productB.getId());

        return new SortedRoundResult(
                successCount,
                conflictCount,
                savedA.getStock(),
                savedB.getStock(),
                savedA.getSalesCount(),
                savedB.getSalesCount(),
                persistedOrderCount == null ? 0 : persistedOrderCount);
    }

    private OrderAttempt createOrder(long userId, List<Long> productIds,
                                     CyclicBarrier startBarrier) {
        try {
            awaitBarrier(startBarrier);
            orderService.createOrder(userId, orderRequest(productIds));
            return OrderAttempt.succeeded();
        } catch (CannotAcquireLockException exception) {
            return OrderAttempt.conflicted();
        } catch (Exception exception) {
            return OrderAttempt.unexpected(exception);
        }
    }

    private Product createProduct(String name) {
        return productRepository.save(new Product(
                "다중상품-" + name,
                "Vegan", 1_000, "image", "detail",
                2, 101, "다중 상품 잠금 순서 테스트"));
    }

    private OrderCreateRequest orderRequest(List<Long> productIds) {
        List<OrderItemRequest> items = new ArrayList<>();
        for (Long productId : productIds) {
            OrderItemRequest item = new OrderItemRequest();
            item.setProductId(productId);
            item.setQuantity(1);
            items.add(item);
        }

        OrderCreateRequest request = new OrderCreateRequest();
        request.setUserName("다중 상품 주문자");
        request.setPhone("010-0000-0000");
        request.setAddress("서울시");
        request.setItems(items);
        return request;
    }

    private void awaitBarrier(CyclicBarrier barrier) {
        try {
            barrier.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("동시 주문 대기 중 스레드가 중단됐습니다.", exception);
        } catch (BrokenBarrierException | TimeoutException exception) {
            throw new IllegalStateException("두 주문이 제한 시간 안에 모이지 못했습니다.", exception);
        }
    }

    record LockAttempt(boolean conflict, Throwable unexpectedError) {
        static LockAttempt completed() {
            return new LockAttempt(false, null);
        }

        static LockAttempt conflicted() {
            return new LockAttempt(true, null);
        }

        static LockAttempt unexpected(Throwable error) {
            return new LockAttempt(false, error);
        }
    }

    record OrderAttempt(boolean success, boolean conflict, Throwable unexpectedError) {
        static OrderAttempt succeeded() {
            return new OrderAttempt(true, false, null);
        }

        static OrderAttempt conflicted() {
            return new OrderAttempt(false, true, null);
        }

        static OrderAttempt unexpected(Throwable error) {
            return new OrderAttempt(false, false, error);
        }
    }

    record SortedRoundResult(int successCount, int conflictCount,
                             int productAFinalStock, int productBFinalStock,
                             int productASalesCount, int productBSalesCount,
                             int persistedOrderCount) {
    }
}
