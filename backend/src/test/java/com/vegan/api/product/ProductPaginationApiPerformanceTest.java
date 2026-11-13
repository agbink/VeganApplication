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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("performance")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductPaginationApiPerformanceTest {

    private static final String KEYWORD = "page-test-product";
    private static final int PRODUCT_COUNT = 10_000;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
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
    void listApiAlwaysReturnsBoundedSliceWithoutCountQuery() throws Exception {
        createProductsWithBatchInsert();

        String basePath = "/api/products/search?keyword=" + KEYWORD;

        // page를 생략해도 전체가 아니라 첫 페이지(20건)만 반환한다.
        JsonNode defaultPage = objectMapper.readTree(request(basePath));
        assertEquals(DEFAULT_PAGE_SIZE, defaultPage.get("content").size(), "page 생략 시 기본 20건만 반환해야 합니다.");
        assertTrue(defaultPage.get("first").asBoolean());
        assertFalse(defaultPage.get("last").asBoolean());
        assertNull(defaultPage.get("totalElements"), "무한 스크롤 응답에는 전체 개수가 없어야 합니다.");

        // size는 최대 100건으로 제한한다.
        JsonNode capped = objectMapper.readTree(request(basePath + "&page=0&size=1000"));
        assertEquals(MAX_PAGE_SIZE, capped.get("content").size(), "size는 최대 100건으로 제한해야 합니다.");

        // 마지막 페이지와 범위를 벗어난 페이지
        int lastPage = PRODUCT_COUNT / MAX_PAGE_SIZE - 1;
        JsonNode last = objectMapper.readTree(request(basePath + "&page=" + lastPage + "&size=" + MAX_PAGE_SIZE));
        assertEquals(MAX_PAGE_SIZE, last.get("content").size());
        assertTrue(last.get("last").asBoolean(), "마지막 페이지는 last=true여야 합니다.");
        JsonNode beyond = objectMapper.readTree(request(basePath + "&page=" + (lastPage + 1) + "&size=" + MAX_PAGE_SIZE));
        assertEquals(0, beyond.get("content").size());
        assertTrue(beyond.get("last").asBoolean());

        for (int i = 0; i < WARMUP_COUNT; i++) {
            request(basePath);
        }

        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        List<Double> samples = new ArrayList<>();
        long sqlCount = 0;
        int bytes = 0;
        for (int i = 0; i < MEASUREMENT_COUNT; i++) {
            SqlTimedResponse response = measureWithSqlCount(basePath, statistics);
            samples.add(response.elapsedMs());
            sqlCount += response.sqlCount();
            bytes = response.bytes();
        }

        System.out.printf(
                "Slice API timing: products=%d, pageSize=%d, runs=%d, avgMs=%.3f, p95Ms=%.3f, bytes=%d, sql=%d%n",
                PRODUCT_COUNT, DEFAULT_PAGE_SIZE, MEASUREMENT_COUNT,
                average(samples), percentile95(samples), bytes, sqlCount);

        assertEquals(MEASUREMENT_COUNT, sqlCount,
                "Slice 조회는 COUNT 없이 요청마다 데이터 조회 SQL 한 번만 실행해야 합니다.");
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

    private SqlTimedResponse measureWithSqlCount(String path, Statistics statistics) {
        long sqlBefore = statistics.getPrepareStatementCount();
        long startedAt = System.nanoTime();
        String body = request(path);
        double elapsedMs = (System.nanoTime() - startedAt) / 1_000_000.0;
        long sqlCount = statistics.getPrepareStatementCount() - sqlBefore;
        return new SqlTimedResponse(elapsedMs, sqlCount, body.getBytes(StandardCharsets.UTF_8).length);
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

    record SqlTimedResponse(double elapsedMs, long sqlCount, int bytes) {
    }
}
