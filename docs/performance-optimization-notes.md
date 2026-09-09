# Vegan 프로젝트 개선 - 변경 전후 비교 노트

적용한 개선 4가지: ① DB 인덱스 ② 페이지네이션 ③ 무한 스크롤(앱) ④ N+1 해결 ⑤ 설정 분리

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

## 남은 과제 (나중에)

- **검색 최적화**: `LIKE '%키워드%'`는 인덱스를 못 탐 → 상품 수천 개 이상이면 MySQL FULLTEXT + ngram 파서 도입
- **Flyway**: `ddl-auto=update` 대신 SQL 마이그레이션 파일로 스키마 관리 → 배포 직전에 도입
- **SearchActivity 페이징**: 타이핑마다 검색하는 구조라 디바운스(입력 멈춤 감지)와 함께 적용해야 함
