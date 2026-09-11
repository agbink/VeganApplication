package com.vegan.api.product;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 이름/브랜드 부분 문자열 검색에 일반 B-tree 인덱스가 실제로 효과가 있는지 검증한다.
 *
 * ContainingIgnoreCase가 만드는 조건은 lower(column) like '%keyword%' 형태다.
 * 선행 와일드카드 때문에 일반 인덱스를 추가해도 range 탐색을 할 수 없다는 점을
 * EXPLAIN ANALYZE와 반복 실행시간으로 함께 확인한다.
 */
@Tag("performance")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ProductSearchIndexPerformanceTest {

    private static final int PRODUCT_COUNT = 100_000;
    private static final int MATCH_COUNT = 100;
    private static final int WARMUP_COUNT = 3;
    private static final int MEASUREMENT_COUNT = 20;
    private static final String KEYWORD = "needle";
    private static final String PATTERN = "%" + KEYWORD + "%";
    private static final String SEARCH_SQL = """
            select id, name, brand_name, price, stock, category, sales_count, created_at
            from product
            where lower(name) like ? or lower(brand_name) like ?
            """;

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("vegan_product_search_performance_test")
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

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void comparesContainingIgnoreCaseSearchBeforeAndAfterBtreeIndexes() {
        createProductsWithBatchInsert();

        QueryMeasurement withoutIndex = measure("withoutIndex");

        jdbcTemplate.execute("create index idx_product_name on product (name)");
        jdbcTemplate.execute("create index idx_product_brand_name on product (brand_name)");
        jdbcTemplate.execute("analyze table product");

        QueryMeasurement withIndex = measure("withBtreeIndex");

        System.out.printf(Locale.ROOT,
                "Product search index comparison: products=%d, matches=%d, runs=%d, " +
                        "withoutIndexAvgMs=%.3f, withoutIndexP95Ms=%.3f, " +
                        "withIndexAvgMs=%.3f, withIndexP95Ms=%.3f, improvementPct=%.2f%n",
                PRODUCT_COUNT, MATCH_COUNT, MEASUREMENT_COUNT,
                withoutIndex.averageMs(), withoutIndex.p95Ms(),
                withIndex.averageMs(), withIndex.p95Ms(),
                improvementPercent(withoutIndex.averageMs(), withIndex.averageMs()));
        System.out.println("Product search EXPLAIN ANALYZE without index: " + withoutIndex.explain());
        System.out.println("Product search EXPLAIN ANALYZE with B-tree index: " + withIndex.explain());

        assertEquals(MATCH_COUNT, withoutIndex.resultCount());
        assertEquals(MATCH_COUNT, withIndex.resultCount());
        assertTrue(withoutIndex.explain().toLowerCase(Locale.ROOT).contains("table scan"),
                "인덱스 적용 전에는 전체 테이블 스캔이어야 합니다: " + withoutIndex.explain());
        assertTrue(withIndex.explain().toLowerCase(Locale.ROOT).contains("table scan"),
                "선행 와일드카드 검색은 B-tree 인덱스를 추가해도 전체 스캔이어야 합니다: " + withIndex.explain());
    }

    private QueryMeasurement measure(String label) {
        for (int i = 0; i < WARMUP_COUNT; i++) {
            executeSearch();
        }

        List<Double> samples = new ArrayList<>(MEASUREMENT_COUNT);
        int resultCount = 0;
        for (int i = 0; i < MEASUREMENT_COUNT; i++) {
            long startedAt = System.nanoTime();
            resultCount = executeSearch();
            samples.add((System.nanoTime() - startedAt) / 1_000_000.0);
        }

        String explain = jdbcTemplate.queryForObject(
                "explain analyze " + SEARCH_SQL,
                String.class,
                PATTERN,
                PATTERN);
        System.out.printf(Locale.ROOT, "%s samplesMs=%s%n", label, samples);
        return new QueryMeasurement(average(samples), percentile95(samples), resultCount, explain);
    }

    private int executeSearch() {
        return jdbcTemplate.query(
                SEARCH_SQL,
                (resultSet, rowNumber) -> resultSet.getLong("id"),
                PATTERN,
                PATTERN).size();
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
                1_000,
                (statement, index) -> {
                    boolean matches = index < MATCH_COUNT;
                    statement.setString(1, matches ? "vegan-" + KEYWORD + "-product-" + index : "vegan-product-" + index);
                    statement.setString(2, matches ? "brand-" + KEYWORD + "-" + index : "brand-" + index);
                    statement.setInt(3, 1_000 + index % 100_000);
                    statement.setString(4, "image-" + index);
                    statement.setString(5, "detail-" + index);
                    statement.setInt(6, 100);
                    statement.setInt(7, 101 + index % 3);
                    statement.setString(8, "상품 검색 인덱스 성능 측정 데이터");
                    statement.setInt(9, index % 1_000);
                    statement.setTimestamp(10, Timestamp.valueOf(LocalDateTime.of(2026, 1, 1, 0, 0).plusSeconds(index)));
                });
    }

    private double average(List<Double> samples) {
        return samples.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
    }

    private double percentile95(List<Double> samples) {
        List<Double> sorted = new ArrayList<>(samples);
        Collections.sort(sorted);
        return sorted.get((int) Math.ceil(sorted.size() * 0.95) - 1);
    }

    private double improvementPercent(double beforeMs, double afterMs) {
        return (beforeMs - afterMs) / beforeMs * 100.0;
    }

    record QueryMeasurement(double averageMs, double p95Ms, int resultCount, String explain) {
    }
}
