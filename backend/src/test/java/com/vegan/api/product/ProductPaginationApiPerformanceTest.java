package com.vegan.api.product;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.persistence.EntityManagerFactory;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("performance")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductPaginationApiPerformanceTest {

    private static final String KEYWORD = "page-test-product";
    private static final int PRODUCT_COUNT = 10_000;
    private static final int PAGE_SIZE = 20;
    private static final int WARMUP_COUNT = 3;
    private static final int MEASUREMENT_COUNT = 20;

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("vegan_pagination_performance_test")
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
    JdbcTemplate jdbcTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Test
    void comparesFullResponseWithTwentyItemPage() throws Exception {
        createProductsWithBatchInsert();

        String fullPath = "/api/products/search?keyword=" + KEYWORD;
        String pagePath = fullPath + "&page=0&size=" + PAGE_SIZE;

        String fullBody = request(fullPath);
        String pageBody = request(pagePath);
        verifyResponses(fullBody, pageBody);

        for (int i = 0; i < WARMUP_COUNT; i++) {
            request(fullPath);
            request(pagePath);
        }

        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        List<Double> fullSamples = new ArrayList<>();
        List<Double> pageSamples = new ArrayList<>();
        long fullSqlCount = 0;
        long pageSqlCount = 0;

        for (int i = 0; i < MEASUREMENT_COUNT; i++) {
            if (i % 2 == 0) {
                SqlTimedResponse full = measureWithSqlCount(fullPath, statistics);
                SqlTimedResponse page = measureWithSqlCount(pagePath, statistics);
                fullSamples.add(full.elapsedMs());
                pageSamples.add(page.elapsedMs());
                fullSqlCount += full.sqlCount();
                pageSqlCount += page.sqlCount();
            } else {
                SqlTimedResponse page = measureWithSqlCount(pagePath, statistics);
                SqlTimedResponse full = measureWithSqlCount(fullPath, statistics);
                pageSamples.add(page.elapsedMs());
                fullSamples.add(full.elapsedMs());
                pageSqlCount += page.sqlCount();
                fullSqlCount += full.sqlCount();
            }
        }

        int fullBytes = fullBody.getBytes(StandardCharsets.UTF_8).length;
        int pageBytes = pageBody.getBytes(StandardCharsets.UTF_8).length;

        System.out.printf(
                "Pagination API timing: products=%d, pageSize=%d, runs=%d, " +
                        "fullAvgMs=%.3f, fullP95Ms=%.3f, fullBytes=%d, fullSql=%d, " +
                        "pageAvgMs=%.3f, pageP95Ms=%.3f, pageBytes=%d, pageSql=%d%n",
                PRODUCT_COUNT, PAGE_SIZE, MEASUREMENT_COUNT,
                average(fullSamples), percentile95(fullSamples), fullBytes, fullSqlCount,
                average(pageSamples), percentile95(pageSamples), pageBytes, pageSqlCount);

        assertEquals(MEASUREMENT_COUNT, fullSqlCount,
                "전체 조회는 요청마다 SQL 한 번을 실행해야 합니다.");
        assertEquals(MEASUREMENT_COUNT * 2L, pageSqlCount,
                "Page 조회는 데이터 조회와 COUNT SQL을 각각 실행해야 합니다.");
    }

    private void createProductsWithBatchInsert() {
        List<Integer> indexes = new ArrayList<>(PRODUCT_COUNT);
        for (int index = 0; index < PRODUCT_COUNT; index++) {
            indexes.add(index);
        }

        jdbcTemplate.batchUpdate(
                "insert into product " +
                        "(name, brand_name, price, image_url, detail_image_url, stock, category, " +
                        "description, sales_count, created_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                indexes,
                500,
                (statement, index) -> {
                    statement.setString(1, KEYWORD + "-" + index);
                    statement.setString(2, "Vegan");
                    statement.setInt(3, 1_000 + index);
                    statement.setString(4, "image-" + index);
                    statement.setString(5, "detail-" + index);
                    statement.setInt(6, 100);
                    statement.setInt(7, 101 + index % 3);
                    statement.setString(8, "페이지네이션 API 응답시간 측정 상품");
                    statement.setInt(9, 0);
                    statement.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now().plusNanos(index)));
                });
    }

    private void verifyResponses(String fullBody, String pageBody) throws Exception {
        JsonNode full = objectMapper.readTree(fullBody);
        JsonNode page = objectMapper.readTree(pageBody);

        assertEquals(PRODUCT_COUNT, full.size(), "전체 조회는 모든 상품을 반환해야 합니다.");
        assertNotNull(page.get("content"));
        assertEquals(PAGE_SIZE, page.get("content").size(), "페이지 조회는 20건만 반환해야 합니다.");
        assertEquals(PRODUCT_COUNT, page.get("totalElements").asInt());
    }

    private SqlTimedResponse measureWithSqlCount(String path, Statistics statistics) {
        long sqlBefore = statistics.getPrepareStatementCount();
        long startedAt = System.nanoTime();
        request(path);
        double elapsedMs = (System.nanoTime() - startedAt) / 1_000_000.0;
        long sqlCount = statistics.getPrepareStatementCount() - sqlBefore;
        return new SqlTimedResponse(elapsedMs, sqlCount);
    }

    private String request(String path) {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + path, String.class);
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

    record SqlTimedResponse(double elapsedMs, long sqlCount) {
    }
}
