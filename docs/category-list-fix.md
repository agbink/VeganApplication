# 카테고리 목록 오류 수정 기록

작성일: 2026-11-13

## 증상

- 카테고리 화면에서 **전체** 탭을 누르면 상품이 하나도 나오지 않음
- 탭은 열리지만 스크롤이 끝없이 내려감
- 같은 상품 한 종류만 이름 뒤에 식별자(UUID)가 붙은 채 반복해서 보임

## 원인

### 1. "전체" 조회에 쓸 인덱스가 없음

- "전체" 탭은 `ORDER BY created_at DESC LIMIT 20`으로 조회함
- 기존 인덱스는 `(category, created_at)`이라 카테고리 조건이 없는 쿼리에는 쓰이지 않음
- 실행 계획: `Table scan on product (rows=1.74e+6)` → `Sort`. 요청마다 183만 건을 전부 정렬함
- 게다가 응답이 `Page`라서 요청마다 `COUNT(*)`도 실행됨
- 결과적으로 응답이 앱의 `readTimeout(10s)`을 넘기고, 실패 콜백에서는 로그만 남기므로 빈 화면이 됨

### 2. 탭을 오가면 페이지 상태가 초기화되지 않음

- `FragmentPagerAdapter`는 탭을 벗어나면 View만 파괴하고 Fragment 객체는 재사용함
- `onCreateView`에서 목록은 새로 만들지만 `currentPage`/`isLastPage`는 이전 값이 남음
- 그래서 다시 들어오면 0페이지가 아니라 중간 페이지부터 요청함. 데이터가 적으면 빈 화면, 많으면 앞부분이 빠진 목록이 나옴

### 3. DB 데이터가 원본 7건의 복제본

| 항목 | 값 |
|---|---|
| 전체 건수 | 1,835,071 |
| 서로 다른 상품 | 7종 (`data.sql` 원본) |
| 상품당 복제 수 | 262,153건 (7 × 2¹⁸ + 63) |
| 이름 | `라카 소울 비건 립밤_a3a2cd21-77b5-...` 형태로 복제할 때마다 `_UUID`가 붙음 |
| `created_at` | 원본 값을 그대로 복사함 → 상품마다 26만 건이 같은 시각을 가짐 |

- 자기 테이블을 `INSERT ... SELECT CONCAT(name, '_', UUID()) ...`로 두 배씩 불린 패턴
- 레포 코드에는 이런 쿼리가 없음. 상품 관련 테스트는 모두 Testcontainers 임시 DB에서만 돌기 때문에 로컬 `vegan_db`에 들어가지 않음
- 가장 최신 시각을 가진 "라카 소울 비건 립밤" 26만 건이 최신순 목록 앞부분을 전부 차지해서 한 종류만 반복되고, 무한 스크롤이 끝나지 않음
- 정렬 값이 같은 행이 수십만 건이라 `ORDER BY created_at`만으로는 페이지 경계가 고정되지 않음 (페이지 간 중복·누락 가능)
- 같은 구성의 인덱스가 두 개 있었음 (`idx_product_category_created_at`, `idx_product_category_created`)

### 4. 메인·검색 화면이 상품 전체를 한 번에 요청

- `MainActivity`: 최신 상품 6개를 보여주려고 `GET /api/products`(페이지 없음)로 **전체**를 받아옴
- `SearchActivity`: 검색어가 비면 전체 목록을, 검색어가 있으면 매칭 결과 전부를 받아옴
- 상품이 수십만 건 이상이면 서버 메모리 부족이나 앱 멈춤으로 이어지는 구조

## 수정 내용

### 백엔드

| 파일 | 변경 |
|---|---|
| `backend/src/main/java/com/vegan/api/product/Product.java` | 인덱스를 `id`를 포함한 3개로 재구성: `idx_product_category_created_id (category, created_at DESC, id DESC)`, `idx_product_created_id (created_at DESC, id DESC)`, `idx_product_sales_id (sales_count DESC, id DESC)` |
| `backend/src/main/java/com/vegan/api/product/ProductRepository.java` | 목록·카테고리·베스트 페이지 조회를 `Page` → `Slice`로 바꿔 `COUNT(*)` 제거. 정렬에 `id DESC`를 2차 기준으로 추가 (`...OrderByCreatedAtDescIdDesc`, `...OrderBySalesCountDescIdDesc`) |
| `backend/src/main/java/com/vegan/api/product/ProductService.java` | 위 Repository 변경에 맞춰 반환 타입과 호출 메서드 변경 |

- 검색 API(`/api/products/search?page=`)는 `ProductPaginationApiPerformanceTest`가 `totalElements`를 검증하므로 `Page`로 유지함
- `Slice` 응답에는 `totalPages`/`totalElements`가 없음. 앱은 `last`만 쓰므로 영향 없음

### 앱

| 파일 | 변경 |
|---|---|
| `android/app/src/main/java/com/vegan/category/Category1~4Fragment.java` | `onCreateView`에서 `currentPage`/`isLoading`/`isLastPage` 초기화. 로드 실패 시 토스트 표시 |
| `android/app/src/main/java/com/vegan/main/MainActivity.java` | 최신 상품을 `getProductsPaged(null, 0, 6)`로 6개만 요청 |
| `android/app/src/main/java/com/vegan/search/SearchActivity.java` | 빈 검색어는 `getProductsPaged(null, 0, 20)`, 검색은 `searchProductsPaged(keyword, 0, 20)`로 20건만 요청 |
| `android/app/src/main/java/com/vegan/api/PageResponse.java` | `Slice` 응답이면 total 값이 0이라는 주석 추가 |

### DB 데이터 재생성

로컬 MySQL `vegan_db.product`에 실행한 SQL 스크립트로 진행함 (레포에는 포함하지 않음).

1. 보조 인덱스를 모두 제거함 (대량 삭제·삽입 속도 확보)
2. 원본 7건(id 1~7)과 `order_item`/`cart_item`이 참조하는 상품만 남기고 5만 건 단위로 삭제
3. 참조 때문에 남긴 복제본은 이름 뒤 UUID만 제거
4. 상품 1,300,000건을 새로 생성 (CRC32 해시로 속성마다 독립적으로 분산)
   - 카테고리: 푸드(101) 40% / 패션(102) 30% / 뷰티(103) 30%
   - 이름: `수식어 + 품목 + 옵션` (예: `크리스피 두부스틱 (400g)`, `오버핏 비건 레더 자켓 [블랙]`, `시카 수딩 젤 100ml`)
   - 브랜드: 카테고리별 8~10개
   - 가격: 푸드 1,500~30,000원 / 패션 18,900~297,900원 / 뷰티 5,000~60,000원
   - 재고 0~200, 판매량은 소수 인기 상품에 몰리는 분포
   - 등록일: 2024-01-01 ~ 2026-10-31에 고르게 분산
5. `id`를 포함한 인덱스 3개 생성 후 `ANALYZE TABLE`

### DB 재생성 결과 (2026-11-13, 약 8분 소요)

| 항목 | 값 |
|---|---|
| 재생성 전 | 1,835,071건 |
| 삭제 후 남은 건수 | 833건 (원본 7 + 주문 참조 84 + 장바구니 참조 743, 중복 제외) |
| 재생성 후 | **1,300,833건** |
| 서로 다른 상품명 | 1,608개 |
| 카테고리 | 푸드 520,512 / 패션 389,960 / 뷰티 390,361 |
| 등록일 | 2024-01-01 ~ 2026-10-31 |

"전체" 탭 쿼리 실행 계획:

```
Before: Sort: product.created_at DESC  →  Table scan on product (rows=1.74e+6)
After : Index scan on product using idx_product_created_id (rows=20)
```

카테고리 쿼리: `Index lookup on product using idx_product_category_created_id (category = 101)`. LIMIT 20에서 멈춤.

## 2차 수정: 검색 속도 개선 · 페이지 없는 목록 API 제거

### 문제

- 검색: 글자를 입력할 때마다 요청이 나가고, 요청마다 `LIKE '%키워드%'`(전체 스캔)와 `COUNT(*)`(전체 스캔)가 실행됨
- `GET /api/products`, `/best`, `/search`를 `page` 없이 호출하면 **조건에 맞는 상품 전체**를 반환함. 130만 건이면 서버 메모리 부족 위험이 있고, `getAll()`은 그 결과를 Redis에 통째로 캐시하려고 함
- k6 부하 테스트가 페이지 없는 API를 호출하고 있었음

### 변경

| 파일 | 변경 |
|---|---|
| `ProductController.java` | 모든 목록 API가 항상 페이지 응답. `page` 기본값 0, `size` 기본값 20, 최대 100으로 제한, 음수 page는 0으로 보정 |
| `ProductSliceResponse.java` (신규) | `content`, `number`, `size`, `first`, `last`만 담은 응답 객체. `SliceImpl`은 Redis JSON 캐시에서 역직렬화가 안 돼서 별도로 만듦 |
| `ProductService.java` | 페이지 없는 메서드 제거. 목록·카테고리·베스트 캐시를 **페이지 단위**(`all:{page}:{size}` 등)로 변경. 검색은 2글자 이상이면 FULLTEXT, 1글자이거나 연산자 문자(`+ - < > ( ) ~ * " @`)가 섞이면 LIKE로 처리. 두 경우 모두 COUNT 없이 `size + 1`건으로 다음 페이지 판단 |
| `ProductRepository.java` | List 반환 메서드 제거. ngram FULLTEXT 구문 검색 네이티브 쿼리 `searchByFullText` 추가. LIKE 검색도 `Slice` + `ORDER BY id DESC` |
| `ProductFullTextIndexInitializer.java` (신규) | 기동 시 `ft_product_name_brand (name, brand_name) WITH PARSER ngram`이 없으면 생성. JPA `@Index`로는 FULLTEXT를 선언할 수 없어서 추가 |
| `android/.../SearchActivity.java` | 300ms 디바운스. 새 검색을 보내기 전에 이전 요청을 취소해서 늦게 온 응답이 최신 결과를 덮어쓰지 않게 함 |
| `android/.../ProductApiService.java` | 페이지 없는 메서드(`getProducts`, `getBestSellers`, `searchProducts`) 제거 |
| `backend/k6/load-test.js` | 모든 호출을 페이지 API로 변경. 검색 시나리오 추가 |
| `ProductPaginationApiPerformanceTest.java` | "전체 vs 20건" 비교 대상인 전체 조회 API가 없어져서 새 API 계약 검증으로 재작성: page 생략 시 20건, size 상한 100, 마지막 페이지 `last=true`, 범위 밖 페이지는 빈 결과, 요청당 SQL 1회 |
| `ProductSearchIndexPerformanceTest.java` | 기존 LIKE(인덱스 없음 / B-tree) 비교에 FULLTEXT ngram 측정 추가 |

### 테스트 결과 (2026-11-13)

| 테스트 | 결과 |
|---|---|
| 일반 회귀 (`AdminAuthorizationServiceTest`, `OrderServiceTest`, `OrderPersistenceIntegrationTest`) | 8/8 통과 |
| `ProductPaginationApiPerformanceTest` | 통과. 상품 1만 건, 20건 응답 5,701 bytes, 평균 22.6ms, **20회 요청에 SQL 20회 (COUNT 없음)** |
| `ProductCacheApiPerformanceTest` | 통과. 페이지 단위 캐시로 바꾼 뒤에도 Miss 50회 SQL 50회, **Hit SQL 0회**, Miss/Hit 응답 동일. 평균 31.6ms → 15.8ms |
| `ProductSearchIndexPerformanceTest` | 통과. 결과 건수 동일(100건), FULLTEXT 인덱스 사용 확인 |

검색 `EXPLAIN ANALYZE` (상품 1만 건 중 100건 매칭):

| 방식 | 읽은 행 | DB 실행 시간 |
|---|---|---|
| LIKE, 인덱스 없음 | Table scan 10,000행 | 6.79ms |
| LIKE, B-tree 인덱스 | Table scan 10,000행 | 14ms |
| **FULLTEXT ngram** | Full-text index search 100행 | **0.28ms** |

**측정 한계:** 같은 테스트에서 JDBC 왕복을 포함한 응답시간(5회 평균)은 LIKE 25.7ms, FULLTEXT 51.7ms로 오히려 FULLTEXT가 느리게 나왔다. 그런데 실행 계획이 완전히 같은 LIKE(인덱스 없음)와 LIKE(B-tree)도 25.7ms와 53.2ms로 두 배 차이가 났고, 표본 하나하나가 15~116ms로 크게 흔들렸다. 메모리가 빠듯한 노트북에서 5회만 측정한 탓에 잡음이 지배적이었던 것으로 보인다. 지금 근거로 쓸 수 있는 수치는 `EXPLAIN ANALYZE`의 DB 내부 실행 시간이고, 응답시간 개선은 측정 횟수를 늘리고 데이터를 키워 다시 재야 한다.

또 이 측정은 **매칭 건수가 적은 검색어(1%)** 기준이다. "비건"처럼 상품 상당수에 들어가는 흔한 검색어는 FULLTEXT가 매칭 전체를 모아 `ORDER BY id DESC`로 정렬해야 하므로 이득이 작거나 오히려 느릴 수 있다. 측정하지 않았다.

### 동작 변경 (호환성)

- `GET /api/products` 등을 `page` 없이 호출하면 이제 배열이 아니라 `{content, number, size, first, last}` 객체 1페이지를 반환함
- 검색은 2글자 이상이면 FULLTEXT 구문 검색. 결과 순서가 `id DESC`로 고정됨
- 응답에 `totalElements`/`totalPages`가 없음
- 실제 DB(130만 건)에서는 백엔드를 처음 기동할 때 FULLTEXT 인덱스를 만드느라 수 분이 걸리고, 그동안 검색 API는 실패할 수 있음

### 실제 DB(130만 건) 확인 결과 (2026-11-13)

로컬 `vegan_db`로 백엔드를 띄워 확인했다. FULLTEXT 인덱스 최초 생성에 **6분 44초**가 걸렸다.

**목록 API: 정상**

| 요청 | 결과 |
|---|---|
| `GET /api/products` (page 생략) | 20건, `first=true`, `last=false` |
| `?size=1000` | 100건 (상한 적용) |
| `?page=-3&size=0` | page 0, 1건 (보정) |
| `?category=101&page=0&size=20` | 20건, 수 ms |
| `/best?page=0&size=3` | 판매량 상위 3건 |
| 기동 직후 첫 요청 | 6.2초 (버퍼 풀이 비어 있는 콜드 상태) |
| `?category=102&page=500&size=20` (OFFSET 10,000) | 3.8초. 깊은 페이지일수록 느려짐 |

**검색 API: 흔한 검색어에서 FULLTEXT가 크게 느림 (개선 실패)**

| 검색어 | 처리 방식 | 매칭 규모 | 응답시간 (1회차 / 2회차) |
|---|---|---|---|
| 두부스틱 | FULLTEXT | 많음 | 10.1s / 12.4s |
| 비건 | FULLTEXT | 매우 많음 | 11.9s / **41.5s** |
| 라카 | FULLTEXT | 13건 | 0.54s / 0.07s |
| 없는상품 | FULLTEXT | 0건 | 0.02s / 0.02s |
| 두 (1글자) | LIKE | 많음 | 1.7s / 0.04s |
| SPF50+ | LIKE | 많음 | 0.05s / 0.02s |
| zzqx+ | LIKE | 0건 (전체 스캔) | 18.0s / 9.2s |

- FULLTEXT는 **매칭이 적을 때 빠르고 많을 때 느리다.** 매칭된 행 전체에 구문(phrase) 위치를 확인하고 `ORDER BY id DESC`로 정렬해야 하기 때문이다
- LIKE + `ORDER BY id DESC LIMIT 21`은 반대다. 매칭이 많으면 20건을 금방 찾고 멈추지만, 매칭이 적거나 없으면 130만 건을 끝까지 훑는다
- 더미 데이터의 상품명 조합이 1,608개뿐이라, 실제 검색어 대부분이 수만~수십만 건에 매칭된다. 비건 쇼핑몰에서 "비건"은 실제로도 대부분의 상품에 들어가는 단어다
- MySQL 기본 `innodb_buffer_pool_size`(128MB)에 테이블이 다 들어가지 않아서, 전체 스캔이 디스크 읽기가 된 영향도 있다

## 3차 수정: 검색을 LIKE로 되돌리고 데이터 10만 건으로 축소

FULLTEXT는 흔한 검색어에서 10~41초가 걸려 채택하지 않았다. 검색은 LIKE로 되돌렸고, COUNT 제거(Slice)와 앱 디바운스는 유지했다.

| 변경 | 내용 |
|---|---|
| `ProductService.search` | 항상 `LIKE '%keyword%'` + `ORDER BY id DESC` + Slice |
| `ProductRepository` | `searchByFullText` 제거 |
| `ProductFullTextIndexInitializer` | 삭제 |
| `ProductSearchIndexPerformanceTest` | FULLTEXT 측정을 제거하고 원래 상태로 복원 |
| DB | `ft_product_name_brand` 인덱스 삭제 |

### 데이터 축소 (130만 → 10만)

- 생성 데이터 중 `id % 13 = 0`인 행만 남김. 상품 속성은 일련번호 해시로 정했고 `id`와는 관계가 없어서, 카테고리 비율과 등록일 분포가 유지되는 표본이 된다
- 원본 7건과 주문·장바구니 참조 상품은 유지
- 결과: **100,833건** (푸드 40,733 / 패션 29,992 / 뷰티 30,108), 인덱스 포함 **40.1MB**. MySQL 기본 버퍼 풀(128MB)에 모두 들어감

### 10만 건 실제 DB 확인 결과

| 요청 | 응답시간 |
|---|---|
| `GET /api/products` (기동 직후 첫 요청) | 2.06s |
| `?category=101&page=0&size=20` | 0.19s |
| `?category=102&page=500&size=20` (OFFSET 10,000) | 0.76s |
| `/best?page=0&size=20` | 0.03s |

| 검색어 | 130만 건 (이전) | 10만 건 (3회) |
|---|---|---|
| 비건 | 12~41s (FULLTEXT) | 0.20 / 0.03 / 0.02s |
| 두부스틱 | 10~12s (FULLTEXT) | 0.02 / 0.03 / 0.03s |
| 라카 (13건) | 0.07~0.5s (FULLTEXT) | 0.22 / 0.27 / 0.23s |
| 두 | 0.04~1.7s (LIKE) | 0.06 / 0.04 / 0.02s |
| zzqx (매칭 없음, 전체 스캔) | 9~18s (LIKE) | 0.26 / 0.22 / 0.18s |
| 없는상품 (매칭 없음) | - | 0.44 / 0.26 / 0.22s |

- 최악의 경우(매칭 없음 → 10만 건 전체 스캔)도 0.2~0.4초
- 데이터 규모가 커지면 LIKE 전체 스캔은 다시 느려짐. 대규모 검색은 별도 검색 엔진이 필요

## 4차: 상품 중복 제거 · 장바구니 정리 · 기본 이미지

### 상품 중복 제거 (브랜드 + 이름 기준)

- 10만 건이 서로 다른 상품이 아니라 상품명 1,608개 조합이 반복된 것이어서, 브랜드+이름이 같으면 `id`가 가장 작은 1건만 남김
- 원본(id 1~7)과 주문이 참조하는 상품은 유지
- 결과: 100,833건 → **8,383건** (서로 다른 브랜드+이름 8,376종 + 주문 참조 때문에 남은 중복 7건)
- 카테고리: 푸드 4,279 / 패션 1,763 / 뷰티 2,341

### 원본 상품 사본 합치기

- 검색 "라카 소울 비건 립밤"이 2건, "라카"가 13건(`상품_984772_144906` 같은 예전 더미 포함) 나오던 문제
- 원본(id 1~7)과 브랜드·카테고리·가격이 같고 이름이 같거나 `상품_숫자_숫자` 형태인 행을 사본으로 판단 → **744건**
- 사본을 가리키던 `order_item` 133건과 `review` 9건을 원본 id로 변경한 뒤 사본 삭제 (한 트랜잭션). 원본 재고는 그대로 둠
- 주문 항목은 상품명·가격·이미지 스냅샷을 따로 저장하므로 주문 내역 표시는 바뀌지 않음
- 결과: 상품 **7,639건**, 브랜드+이름 중복 0건, `상품_` 더미 0건, 상품이 없는 주문 항목 0건
- 검색 확인: "라카 소울 비건 립밤", "라카", "크리스피 두부스틱 (400gx1봉)", "벌룬 원피스", "코스노리" 모두 1건

### 리뷰 데이터 정리

- `review` 500건 모두 `리뷰 내용 N` 더미였고, 작성자 이름 없음, 작성 시각 전부 21:04:32
- 그중 480건은 이미 삭제된 상품에 달린 리뷰였음 (`review.product_id`에 외래키가 없어 상품 삭제 후에도 남음). 상품 화면에는 안 보이지만 "내 리뷰"·관리자 리뷰 화면에는 보였음 → **삭제**
- 남은 20건(원본 상품 1~5, 7): 상품과 별점에 맞는 문장으로 교체, 작성자 이름을 서로 다른 닉네임 20개로 변경(작성 계정 `user_id`는 유지), 작성 시각을 08:00~22:00으로 분산(날짜·별점 유지)
- 결과: 리뷰 20건, 더미 문구 0건, 상품 없는 리뷰 0건
- 남은 점: 같은 계정이 같은 상품에 리뷰를 여러 개 가진 상태(앱 규칙상 1인 1리뷰)라 화면에서는 닉네임이 달라도 DB상 작성자는 계정 2개뿐임

### 장바구니

- 정리 전 `cart_item` 973행(사용자 3명, 수량 합 1,950) → 전체 삭제
- 삭제 전 확인: **같은 사용자·같은 상품이 두 행 이상인 경우 0건**. 기존 데이터에서도 중복 행은 생기지 않았음
- `CartService.addItem`은 `(user_id, product_id)`로 기존 항목을 찾아 있으면 수량만 더함. 합계가 재고를 넘으면 400으로 거절
- 신규 테스트 `CartPersistenceIntegrationTest` (MySQL Testcontainers) **3/3 통과**
  - 같은 상품 2개 + 3개 담기 → 행 1개, 같은 항목 id, 수량 5
  - 재고 5개 상품에 4개 담은 뒤 2개 추가 → 거절되고 수량 4 유지
  - 다른 상품·다른 사용자는 각각 별도 행
- 남은 위험: `cart_item`에 `(user_id, product_id)` 유니크 제약이 없음. 같은 상품을 동시에 두 번 담는 요청(버튼 연타 등)이 겹치면 두 요청 모두 "기존 항목 없음"으로 판단해 행이 2개 생길 수 있고, 그 뒤 조회(`findByUser_IdAndProduct_Id`)가 결과 2건 예외로 실패할 수 있음. 동시 요청은 테스트하지 않았음

### 기본 이미지

- 원인: DB의 사진 주소가 모두 `https://example.com/products/N.jpg`(404)이고, 앱의 Glide 호출 17곳에 실패 시 이미지가 지정되지 않아 빈칸으로 보였음
- `res/drawable/img_placeholder.xml` 추가 (연한 배경 + 잎사귀)
- `com.vegan.util.ImageLoader.load(ImageView, url)` 추가: 로딩 중·실패·주소 없음 모두 기본 이미지 표시
- 앱의 Glide 호출 17곳을 모두 `ImageLoader.load`로 교체. 리뷰 사진처럼 주소가 없으면 숨기는 기존 처리는 유지
- 더미 데이터 사진 주소는 아직 `example.com` 그대로임 (매 이미지마다 404 요청이 나감)

## 5차: 샘플 상품 사진 · 장바구니 동시 담기

### 샘플 상품 사진 (무료 라이선스)

- Wikimedia Commons에서 품목별로 받은 사진 **65장** (CC0 21, Public domain 5, CC BY 14, CC BY-SA 25)
- 받은 뒤 한 장씩 미리보기로 검수해 품목과 맞지 않는 사진(그림·도자기·꽃·로켓 등), 욕설 문구가 있는 사진, 상표가 크게 보이는 사진 39장을 제외함
- 저장 위치: `backend/uploads/samples/` (`uploads/`는 gitignore 대상이라 사진 파일은 커밋되지 않음)
- 레포에 남긴 것: `scripts/sample_images/download_sample_images.py`, 최종 목록 `manifest.json`, 출처 `ATTRIBUTION.md`
  - 같은 사진 다시 받기: `python -I scripts/sample_images/download_sample_images.py backend/uploads/samples --from-manifest scripts/sample_images/manifest.json`
- DB: 상품 이름의 품목에 맞춰 `image_url`, `detail_image_url`을 `http://localhost:8080/uploads/samples/{품목}_{n}.jpg`로 변경. 7,639건 전부 매칭(미매칭 0건). 수분 크림은 맞는 사진이 없어 알로에 사진을 같이 씀
- 서버에서 65개 주소 모두 200 응답 확인
- 주의: 주소가 `localhost`라 폰에서는 `adb reverse tcp:8080 tcp:8080`이 걸려 있어야 보임

### 장바구니 동시 담기

- `CartItem`에 `(user_id, product_id)` 유니크 제약 `uk_cart_item_user_product` 추가 (실제 DB에도 추가, 기존 중복 0건)
- `CartService.addItem`이 시작할 때 사용자 행을 `PESSIMISTIC_WRITE`로 잠가 같은 사용자의 담기 요청을 순서대로 처리 (`UserRepository.findByIdForUpdate`)
- 신규 `CartConcurrencyIntegrationTest` (`concurrency` 태그): 같은 사용자가 같은 상품을 10개 스레드로 동시에 담기 × 10라운드

| 조건 | 결과 |
|---|---|
| 유니크 제약만 (잠금 없음) | 실패: 동시 요청 일부가 `Duplicate entry ... uk_cart_item_user_product`로 거절됨 (중복 행은 막히지만 사용자에게는 담기 실패) |
| 유니크 제약 + 사용자 행 잠금 | 통과: 매 라운드 1행, 수량 10 |

### 문서 정리

- README의 "상품 10,000건 전체 조회 → 20건 페이지 조회 82.0% 감소" 삭제 (전체 조회 API가 없어짐)
- `docs/performance-optimization-notes.md`의 같은 측정 절(6.1) 삭제, 이후 절 번호 조정

## 검증

- [x] 백엔드 `compileJava compileTestJava` 통과
- [x] 앱 `:app:compileDebugJavaWithJavac` 통과
- [x] DB 재생성 결과 확인 (건수, 카테고리 분포, 실행 계획)
- [x] 일반 회귀 테스트 + 상품 성능 테스트 3개 통과
- [x] 백엔드 재시작 후 FULLTEXT 인덱스 생성 확인 (6분 44초)
- [x] 실제 DB로 목록·검색 API 확인 (검색 성능 문제 발견)
- [x] 앱 요청 재현(API): 전체·푸드·패션·뷰티·베스트 각 30페이지 스크롤 중복 0건, 카테고리 혼입 없음, 검색 끝 페이지 `last=true`
- [x] `concurrencyTest` 통과 (`OrderConcurrencyIntegrationTest`, `MultiProductOrderConcurrencyTest`, 7분 41초)
- [ ] 앱 화면 직접 확인: 에뮬레이터 32.1.11이 Hyper-V 환경에서 기동되지 않아 미확인 (Emulator 업데이트 필요)

## 남은 이슈

- **검색 확장성:** 10만 건에서는 최악 0.4초지만 LIKE는 전체 스캔이라 데이터가 늘면 선형으로 느려짐
- **검색 측정 신뢰도:** 위 응답시간은 curl 3회 측정값이라 참고용. 근거로 쓰려면 반복 측정 필요
- **"라카" 검색 결과:** 주문·장바구니 참조 때문에 남긴 예전 더미 13건(`상품_984772_144906` 형태)이 나옴. 해당 주문·장바구니를 정리하면 지울 수 있음
- **깊은 페이지:** OFFSET 방식이라 뒤쪽 페이지로 갈수록 느려짐 (커서 기반 페이지네이션 검토)
- **문서 갱신:** `docs/performance-optimization-notes.md`와 README의 "전체 조회 → 페이지 조회 82.0% 감소"는 이제 없어진 전체 조회 API 기준 수치임. 과거 측정이라고 명시하거나 새 기준으로 다시 정리 필요

## 참고: 이번 수정과 별개로 확인된 것

- `android/gradle.properties`에 `VEGAN_API_BASE_URL=http://localhost:8080/`이 추가돼 있음. 에뮬레이터에서는 `localhost`가 에뮬레이터 자신을 가리키므로 `http://10.0.2.2:8080/`을 써야 함
- `strings.xml`에 실제 소셜 로그인 키가 들어 있어 커밋하지 않도록 주의
