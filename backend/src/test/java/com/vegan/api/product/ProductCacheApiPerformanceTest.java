package com.vegan.api.product;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("performance")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductCacheApiPerformanceTest {

    private static final int PRODUCT_COUNT = 1_000;
    private static final int WARMUP_COUNT = 5;
    private static final int MEASUREMENT_COUNT = 50;

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("vegan_cache_performance_test")
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
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.sql.init.mode", () -> "never");
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.cache.type", () -> "redis");
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
    CacheManager cacheManager;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Test
    void measuresCacheMissAndHitForTheSameHttpResponse() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> createProducts());

        Cache productsCache = cacheManager.getCache("products");
        assertNotNull(productsCache);

        for (int i = 0; i < WARMUP_COUNT; i++) {
            productsCache.clear();
            requestProducts();
            requestProducts();
        }

        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        List<Double> missSamples = new ArrayList<>();
        List<Double> hitSamples = new ArrayList<>();
        long missSqlCount = 0;
        long hitSqlCount = 0;

        for (int i = 0; i < MEASUREMENT_COUNT; i++) {
            productsCache.clear();

            long beforeMissSql = statistics.getPrepareStatementCount();
            TimedResponse miss = measureRequest();
            long afterMissSql = statistics.getPrepareStatementCount();

            TimedResponse hit = measureRequest();
            long afterHitSql = statistics.getPrepareStatementCount();

            assertFalse(miss.body().isBlank());
            assertEquals(miss.body(), hit.body(), "Cache Miss와 Hit의 JSON 응답은 동일해야 합니다.");

            missSamples.add(miss.elapsedMs());
            hitSamples.add(hit.elapsedMs());
            missSqlCount += afterMissSql - beforeMissSql;
            hitSqlCount += afterHitSql - afterMissSql;
        }

        System.out.printf(
                "Redis API timing: products=%d, runs=%d, " +
                        "missAvgMs=%.3f, missP95Ms=%.3f, missSql=%d, " +
                        "hitAvgMs=%.3f, hitP95Ms=%.3f, hitSql=%d%n",
                PRODUCT_COUNT, MEASUREMENT_COUNT,
                average(missSamples), percentile95(missSamples), missSqlCount,
                average(hitSamples), percentile95(hitSamples), hitSqlCount);

        assertEquals(MEASUREMENT_COUNT, missSqlCount,
                "Cache Miss마다 상품 목록 SQL이 한 번 실행되어야 합니다.");
        assertEquals(0, hitSqlCount, "Cache Hit에서는 상품 목록 SQL이 실행되면 안 됩니다.");
    }

    private void createProducts() {
        List<Product> products = new ArrayList<>();
        for (int index = 0; index < PRODUCT_COUNT; index++) {
            products.add(new Product(
                    "캐시측정상품-" + index,
                    "Vegan", 1_000 + index, "image-" + index, "detail-" + index,
                    100, 101 + index % 3, "Redis 캐시 API 응답시간 측정 상품"));
        }
        productRepository.saveAll(products);
    }

    private TimedResponse measureRequest() {
        long startedAt = System.nanoTime();
        String body = requestProducts();
        return new TimedResponse(body, (System.nanoTime() - startedAt) / 1_000_000.0);
    }

    private String requestProducts() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/products", String.class);
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

    record TimedResponse(String body, double elapsedMs) {
    }
}
