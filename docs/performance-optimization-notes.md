# Vegan 프로젝트 개선 - 변경 전후 비교 노트

적용한 개선 5가지: ① DB 인덱스 ② 페이지네이션 ③ 무한 스크롤(앱) ④ N+1 해결 ⑤ 설정 분리

---

## 1. Product 인덱스 추가 (백엔드)

**왜?** 인덱스가 없으면 MySQL이 조건에 맞는 행을 찾을 때 테이블 전체를 훑는다(풀 스캔).
인덱스는 책의 "찾아보기"처럼 특정 컬럼 기준으로 미리 정렬된 목록을 만들어 바로 찾아가게 해준다.

### Before — `Product.java`

```java
@Entity
@Table(name = "product")
public class Product {
```

### After

```java
@Entity
@Table(name = "product",
        indexes = {
                @Index(name = "idx_product_category_created", columnList = "category, createdAt DESC"),
                @Index(name = "idx_product_sales_count", columnList = "salesCount DESC")
        })
public class Product {
```

**포인트**

- `category, createdAt DESC` 복합 인덱스 → `findByCategoryOrderByCreatedAtDesc` 쿼리가 "카테고리 필터 + 최신순 정렬"을 인덱스만으로 처리
- `salesCount DESC` → 베스트셀러 정렬용
- `ddl-auto=update`라서 앱 재시작 시 자동으로 DB에 인덱스가 생성됨
- Orders, Review 엔티티에는 이미 인덱스가 있었고, `cart_item.user_id`는 외래키(FK)라 MySQL이 자동으로 인덱스를 만들어줌

---

## 2. 페이지네이션 (백엔드)

**왜?** 기존에는 상품이 1만 개여도 전부 조회해서 응답했다. 페이지 단위(20개씩)로 잘라 주면
응답 속도, 메모리, 네트워크 모두 절약된다.

### Before — `ProductRepository.java`

```java
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findAllByOrderByCreatedAtDesc();
    List<Product> findByCategoryOrderByCreatedAtDesc(int category);
    List<Product> findAllByOrderBySalesCountDesc();
    List<Product> findByNameContainingIgnoreCaseOrBrandNameContainingIgnoreCase(String name, String brandName);
}
```

### After — 기존 메서드는 유지하고 `Pageable` 버전 추가

```java
public interface ProductRepository extends JpaRepository<Product, Long> {
    // ... 기존 List 버전 그대로 ...

    // 페이지네이션 버전
    Page<Product> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<Product> findByCategoryOrderByCreatedAtDesc(int category, Pageable pageable);
    Page<Product> findAllByOrderBySalesCountDesc(Pageable pageable);
    Page<Product> findByNameContainingIgnoreCaseOrBrandNameContainingIgnoreCase(String name, String brandName, Pageable pageable);
}
```

**포인트**

- Spring Data JPA는 파라미터에 `Pageable`만 붙이면 자동으로 `LIMIT ... OFFSET ...` 쿼리를 만들어줌
- 반환 타입 `Page<T>`에는 데이터(`content`) 외에 전체 페이지 수, 전체 개수, 마지막 페이지 여부가 담김

### Before — `ProductController.java`

```java
@GetMapping
public List<Product> getProducts(@RequestParam(required = false) Integer category) {
    if (category != null) return productService.getByCategory(category);
    return productService.getAll();
}
```

### After — 하위호환: `page` 파라미터가 있을 때만 페이지 응답

```java
@GetMapping
public Object getProducts(@RequestParam(required = false) Integer category,
                          @RequestParam(required = false) Integer page,
                          @RequestParam(defaultValue = "20") int size) {
    if (page != null) {
        Pageable pageable = PageRequest.of(page, size);
        if (category != null) return productService.getByCategory(category, pageable);
        return productService.getAll(pageable);
    }
    if (category != null) return productService.getByCategory(category);
    return productService.getAll();
}
```

**포인트**

- `GET /api/products` → 기존처럼 배열 반환 (옛 앱 버전도 안 깨짐)
- `GET /api/products?page=0&size=20` → `{content: [...], totalPages, totalElements, last, ...}` 반환
- 반환 타입을 `Object`로 두면 Jackson이 실제 객체(List든 Page든)를 보고 알아서 JSON으로 변환
- `/best`, `/search`도 같은 방식으로 변경

---

## 3. 무한 스크롤 (안드로이드 앱)

**왜?** 페이지네이션의 모바일 UI 버전. 스크롤이 목록 끝에 가까워지면 다음 페이지를 자동 요청.

### 새 파일 — `PageResponse.java` (백엔드 Page 응답을 받는 그릇)

```java
public class PageResponse<T> {
    private List<T> content;      // 실제 상품 목록
    private int totalPages;       // 전체 페이지 수
    private long totalElements;   // 전체 상품 수
    private int number;           // 현재 페이지 번호 (0부터)
    private boolean last;         // 마지막 페이지 여부
    // getter들...
}
```

### Before — `ProductApiService.java`

```java
@GET("/api/products")
Call<List<ApiProduct>> getProducts(@Query("category") Integer category);
```

### After — 페이지 버전 추가

```java
@GET("/api/products")
Call<PageResponse<ApiProduct>> getProductsPaged(@Query("category") Integer category,
                                                @Query("page") int page,
                                                @Query("size") int size);
```

### Before — `Category1Fragment.java` (한 번에 전체 로드)

```java
RetrofitClient.getProductApi().getProducts(null).enqueue(new Callback<List<ApiProduct>>() {
    @Override
    public void onResponse(Call<List<ApiProduct>> call, Response<List<ApiProduct>> response) {
        if (response.isSuccessful() && response.body() != null) {
            arrayList.clear();
            arrayList.addAll(response.body());
            adapter.notifyDataSetChanged();   // 목록 전체를 다시 그림
        }
    }
    ...
});
```

### After — 상태 3개 + 스크롤 리스너 + 페이지 단위 로드

```java
private int currentPage = 0;          // 다음에 요청할 페이지
private boolean isLoading = false;    // 요청 중이면 중복 요청 방지
private boolean isLastPage = false;   // 마지막 페이지면 더 요청 안 함

// 무한 스크롤: 목록 끝 5개 이내로 스크롤되면 다음 페이지 로드
recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
    @Override
    public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
        if (dy <= 0 || isLoading || isLastPage) return;   // 위로 스크롤/로딩 중/끝이면 무시
        if (layoutManager.findLastVisibleItemPosition() >= arrayList.size() - 5) {
            loadNextPage();
        }
    }
});

private void loadNextPage() {
    isLoading = true;
    RetrofitClient.getProductApi().getProductsPaged(null, currentPage, PAGE_SIZE)
            .enqueue(new Callback<PageResponse<ApiProduct>>() {
                @Override
                public void onResponse(Call<PageResponse<ApiProduct>> call,
                                       Response<PageResponse<ApiProduct>> response) {
                    isLoading = false;
                    if (response.isSuccessful() && response.body() != null) {
                        int start = arrayList.size();
                        arrayList.addAll(response.body().getContent());   // 뒤에 이어붙임
                        adapter.notifyItemRangeInserted(start, response.body().getContent().size());
                        isLastPage = response.body().isLast();
                        currentPage++;
                    }
                }
                ...
            });
}
```

**포인트**

- `clear() + notifyDataSetChanged()` (전체 교체) → `addAll() + notifyItemRangeInserted()` (뒤에 추가). 후자가 성능도 좋고 스크롤 위치도 유지됨
- `isLoading` 플래그: 응답이 오기 전에 스크롤 이벤트가 수십 번 발생해도 요청은 1번만
- `arrayList.size() - 5`: 끝에 딱 닿기 전에 미리 로드해서 사용자가 끊김을 못 느끼게 함
- Category1~4Fragment, ProductListActivity 동일 패턴 (카테고리 번호만 다름: null/101/102/103)

---

## 4. N+1 문제 해결 (백엔드)

**왜?** 주문 목록을 조회하면 JPA가 주문 1번 + 각 주문의 아이템 N번 + 각 아이템의 상품 N번…
쿼리를 뻥튀기해서 날리는 현상. 주문 10개면 쿼리 21번이 나갈 수 있다.

### Before — `OrderRepository.java`

```java
public interface OrderRepository extends JpaRepository<Orders, Long> {
    List<Orders> findByUserIdOrderByOrderDateDesc(Long userId);
    // items, product는 나중에 접근할 때마다 추가 쿼리 발생 (N+1)
}
```

### After — fetch join으로 한 방에 조회

```java
public interface OrderRepository extends JpaRepository<Orders, Long> {

    // fetch join으로 주문 + 주문상품 + 상품을 한 번에 조회 (N+1 방지)
    @Query("select distinct o from Orders o " +
            "left join fetch o.items i " +
            "left join fetch i.product " +
            "where o.userId = :userId " +
            "order by o.orderDate desc")
    List<Orders> findByUserIdWithItems(@Param("userId") Long userId);

    @Query("select distinct o from Orders o " +
            "left join fetch o.items i " +
            "left join fetch i.product " +
            "where o.id = :orderId")
    Optional<Orders> findByIdWithItems(@Param("orderId") Long orderId);
}
```

### Before / After — `OrderItem.java`

```java
// Before: @ManyToOne 기본값은 EAGER(즉시 로딩) → 아이템 조회 때마다 상품 쿼리 발생
@ManyToOne
@JoinColumn(name = "product_id")
private Product product;

// After: LAZY로 바꾸고, 필요할 땐 위의 fetch join으로 함께 가져옴
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "product_id")
private Product product;
```

**포인트**

- `join fetch`: 연관 엔티티를 SQL JOIN으로 **한 번에** 끌어옴 (JPQL 문법)
- `distinct`: 컬렉션 join 시 주문이 아이템 수만큼 중복돼 나오는 걸 제거
- `@ManyToOne`의 기본 fetch는 EAGER, `@OneToMany`의 기본은 LAZY — 실무에선 전부 LAZY로 두고 필요할 때 fetch join 하는 게 정석
- 확인법: `spring.jpa.show-sql=true` 상태에서 주문 내역 API 호출 → 콘솔에 쿼리가 1번만 찍히면 성공

---

## 5. 민감 정보 환경변수 분리

**왜?** 비밀번호와 시크릿 키가 코드에 그대로 있으면, git에 올리는 순간 유출된다.

### Before — `application.properties`

```properties
spring.datasource.password=YOUR_LOCAL_DB_PASSWORD
jwt.secret=YOUR_LOCAL_JWT_SECRET
```

### After

```properties
# 환경변수가 있으면 그 값을, 없으면 콜론(:) 뒤의 기본값(로컬 개발용)을 사용
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/vegan_db?serverTimezone=Asia/Seoul&characterEncoding=UTF-8}
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD}
jwt.secret=${JWT_SECRET}
```

**포인트**

- `${환경변수명:기본값}` 문법 — Spring이 시작할 때 환경변수를 먼저 찾고, 없으면 기본값 사용
- 로컬과 배포 환경 모두 `DB_PASSWORD`, `JWT_SECRET` 환경변수를 설정해야 함
- 실제 값은 저장소에 기록하지 않고 로컬 환경변수나 비밀 관리 도구에서 관리

---

## 6. 성능·동시성 실측 결과

> 수치는 Windows 11, Docker Desktop, MySQL 8 Testcontainers 환경의 로컬 측정값이다.
> 절대 성능보다 동일 환경에서 변경 전후를 비교하는 근거로 사용한다.

### 6.1 상품 목록 페이지네이션

상품 10,000건을 모두 반환하는 기존 API와 20건만 반환하는 페이지 API를 비교했다.
워밍업 3회 후 각각 20회 측정했으며, 호출 순서에 따른 캐시 편향을 줄이기 위해 실행 순서를 교차했다.

| 지표 | 전체 조회 | 20건 페이지 조회 | 변화 |
| --- | ---: | ---: | ---: |
| 평균 응답시간 | 292.6ms | 52.5ms | **82.0% 감소** |
| p95 응답시간 | 549.3ms | 106.2ms | **80.7% 감소** |
| JSON 응답 크기 | 2.81MB | 5.74KB | **99.8% 감소** |
| SQL | 요청당 1회 | 요청당 2회 | 페이지 조회는 `COUNT` 포함 |

페이지 조회는 전체 개수를 구하는 `COUNT` 때문에 SQL이 한 번 늘었지만, 조회·직렬화·전송하는
데이터를 크게 줄여 전체 응답시간과 응답 크기를 개선했다.

### 6.2 단일 상품 동시 주문

재고 10개인 같은 상품에 20개 주문을 동시에 시작하는 시나리오를 30회 반복했다.

| 지표 | 무잠금 기준 로직 | 비관적 락 적용 |
| --- | ---: | ---: |
| 정합성 실패 회차 | 30/30 | **0/30** |
| DB 락 충돌 | 552건 | **0건** |
| 정상 성공 | 비교 로직 특성상 불안정 | 300건 |
| 재고 부족 거절 | 비교 로직 특성상 불안정 | 300건 |

비관적 락 적용 후 총 600개 요청 중 재고 범위의 300개만 성공했고 나머지 300개는 정상적으로
거절됐다. `noLockConflictTotal=552`는 초과 판매 건수가 아니라 무잠금 비교 로직에서 발생한
데드락성 DB 락 충돌 건수다.

### 6.3 다중 상품 잠금 순서

두 주문이 상품을 `A → B`, `B → A` 순서로 요청하는 교차 잠금 시나리오를 30회 반복했다.
실제 주문 로직은 입력 순서와 관계없이 상품 ID 오름차순으로 잠금을 획득하되, 주문상품 표시
순서는 사용자의 입력 순서를 유지하도록 분리했다.

| 지표 | 정렬 전 기준 로직 | ID 순 잠금 적용 |
| --- | ---: | ---: |
| 데드락 발생 회차 | 30/30 | **0/30** |
| DB 락 충돌 | 30건 | **0건** |
| 적용 후 성공 | - | **60/60건** |

### 6.4 상품 검색 B-tree 인덱스 검증

상품 10,000건 중 100건이 일치하는 `LOWER(name) LIKE '%needle%' OR
LOWER(brand_name) LIKE '%needle%'` 검색을 워밍업 1회, 본 측정 5회 실행했다.

| 지표 | 인덱스 전 | B-tree 인덱스 후 |
| --- | ---: | ---: |
| 평균 | 26.915ms | 21.132ms |
| p95 | 37.551ms | 28.677ms |
| 실행계획 | 전체 테이블 스캔 | 전체 테이블 스캔 |

표면상 평균은 21.48% 줄었지만 실행계획상 인덱스를 사용하지 않았다. 인덱스 적용 후 측정이
나중에 실행되어 버퍼 캐시 효과가 섞일 수 있으므로 이를 인덱스 개선 효과로 해석하지 않는다.
선행 와일드카드 부분 문자열 검색에는 일반 B-tree 인덱스가 효과 없음을 확인하고 도입을
보류했다.

### 6.5 N+1 및 Redis 캐시 측정 기록

주문 10건과 주문상품 30건을 조회해 일반 조회와 Fetch Join을 비교했다.

| 지표 | 일반 조회 | Fetch Join | 변화 |
| --- | ---: | ---: | ---: |
| SQL 실행 횟수 | 41회 | 1회 | **40회 감소(약 97.6%)** |

일반 조회의 41회는 주문 목록 1회에 주문별 연관 데이터를 추가로 조회하면서 발생한 결과다.
Fetch Join은 주문·주문상품·상품을 한 SQL로 조회해 N+1을 제거했다. 이 결과는 SQL 횟수
측정이며, 별도의 HTTP 평균·p95 개선 수치로 표현하지 않는다.

상품 1,000건의 동일한 HTTP 응답에 대해 Cache Miss와 Hit를 50회 비교했다.

| 지표 | Cache Miss | Cache Hit | 변화 |
| --- | ---: | ---: | ---: |
| 평균 응답시간 | 77.414ms | 47.309ms | **38.9% 감소** |
| p95 응답시간 | 148.867ms | 75.395ms | **49.4% 감소** |
| SQL 실행 횟수(50회 합계) | 50회 | 0회 | **100% 제거** |

Cache Hit 응답은 Miss 응답과 동일한 JSON임을 함께 검증했다. 별도 측정에서는 평균
`81.558ms → 46.281ms`(43.3% 감소), p95 `124.966ms → 98.323ms`(21.3% 감소)가
관찰되어 실행별 편차가 있었다. 따라서 포트폴리오에는 측정 조건과 대표 실행값을 함께 밝힌다.

## 남은 과제

- **검색 최적화(선택)**: 데이터 증가 시 MySQL FULLTEXT + ngram을 적용하고 기존 `LIKE`와 검색 정확도·성능 비교
- **락 비용 측정(선택)**: 동시성 1·10·20·50에서 비관적 락의 평균·p95·처리량 비교
- **Flyway**: `ddl-auto=update` 대신 SQL 마이그레이션 파일로 스키마 관리
- **SearchActivity 검색 UX**: 디바운스, 검색 결과 페이지네이션, 이전 요청 취소 적용
- **Git/CI 정리**: 성능 테스트를 일반 단위 테스트와 분리한 상태로 커밋하고 필요 시 수동 성능 측정 잡에서 실행
