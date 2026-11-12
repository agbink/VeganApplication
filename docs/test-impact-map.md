# 변경 영향도 및 테스트 매핑

AI가 제안하거나 생성한 변경을 바로 반영하지 않고, 변경 범위에 맞는 테스트와 추가 검증을 선택하기 위한 기준이다.

## 검증 원칙

1. `git diff`로 변경 파일과 핵심 메서드를 확인한다.
2. 아래 표에서 영향 기능과 우선 테스트를 선택한다.
3. 우선 테스트가 통과하면 일반 회귀 테스트를 실행한다.
4. 동시성 정합성은 `concurrencyTest`로 검증하고, 성능·인덱스 측정은 별도 `performanceTest`로 검증한다.
5. 테스트 결과와 채택·기각 근거를 PR에 남긴 뒤 사람이 최종 승인한다.

## 변경 영향도 표

| 변경 영역 | 주요 영향 | 우선 실행 테스트 | 추가 검증 |
| --- | --- | --- | --- |
| `backend/**/order/OrderService.java` | 주문 생성, 수량 검증, 상품 잠금 순서, 재고 차감 | `OrderServiceTest`, `OrderPersistenceIntegrationTest` | 동시성 변경 시 `concurrencyTest` 실행 |
| `backend/**/order/OrderRepository.java`, `Orders.java`, `OrderItem.java` | 주문 조회, 소유자 검증, 연관관계 로딩, N+1 | `OrderPersistenceIntegrationTest` | `OrderApiPerformanceTest`로 HTTP 응답과 결과 동일성 확인 |
| `backend/**/product/Product.java`, `ProductRepository.java` | 재고 잠금, 상품 조회·검색, 주문과의 연계 | `OrderServiceTest`, `OrderPersistenceIntegrationTest` | 검색 변경 시 `ProductSearchIndexPerformanceTest`와 `EXPLAIN ANALYZE` 확인 |
| `backend/**/product/ProductService.java`, `ProductController.java` | 상품 목록, 검색, 페이지네이션, 응답 형식 | 관련 일반 테스트와 전체 회귀 테스트 | `ProductPaginationApiPerformanceTest`로 전체 조회와 20건 페이지 비교 |
| Redis·Cache 설정 및 상품 캐시 코드 | Cache Miss/Hit, DB 조회 여부, 응답 동일성 | 전체 백엔드 회귀 테스트 | `ProductCacheApiPerformanceTest`로 Hit 시 SQL 0회와 응답 동일성 확인 |
| 주문 재고 잠금·트랜잭션 코드 | 초과 판매, 락 충돌, 데드락 | `OrderServiceTest`, `OrderPersistenceIntegrationTest` | `OrderConcurrencyIntegrationTest`, `MultiProductOrderConcurrencyTest` |
| 주문·상품 DTO 또는 Controller | 요청 검증, HTTP 상태, JSON 계약 | 관련 일반 테스트와 전체 백엔드 회귀 테스트 | Android API 모델·호출부 영향 확인 |
| `android/**` API 모델·화면 코드 | 상품 조회, 검색, 주문 화면과 서버 계약 | Android 단위 테스트 | 주요 사용자 흐름 수동 확인 |
| 공통 설정·의존성·보안 코드 | 애플리케이션 전반 | 백엔드 전체 회귀 테스트 | Android 전체 단위 테스트 및 애플리케이션 기동 확인 |

## 실행 명령

### 일반 회귀 테스트

```bash
cd backend
./gradlew test
```

일반 테스트는 `concurrency`, `performance` 태그를 모두 제외하며 기능·검증·영속성 회귀를 확인한다. PR의 GitHub Actions에서도 동일한 명령을 실행한다.

### 동시성 정합성 테스트

```bash
cd backend
./gradlew concurrencyTest

# 변경 범위에 따라 개별 실행
./gradlew concurrencyTest --tests "com.vegan.api.order.OrderConcurrencyIntegrationTest"
./gradlew concurrencyTest --tests "com.vegan.api.order.MultiProductOrderConcurrencyTest"
```

`concurrency` 태그만 실행한다. MySQL·Redis Testcontainers가 필요하며, PR의 GitHub Actions에서 일반 테스트와 별도 job으로 두 클래스를 모두 실행한다.

- `OrderConcurrencyIntegrationTest`: 무잠금 기준 로직과 비관적 락 적용 로직을 비교하여 주문 수·재고·판매량 정합성을 검증한다.
- `MultiProductOrderConcurrencyTest`: 역순 잠금 기준 로직의 데드락을 재현하고, 상품 ID 순으로 잠그는 실제 주문 로직의 정합성을 검증한다.

각 테스트의 기존 30회 반복과 assertion을 유지한다. 실행시간 로그가 있어도 목적은 응답속도 측정이 아닌 정합성 검증이다.

일반·동시성 회귀를 함께 검증하려면 `./gradlew test concurrencyTest`를 실행한다. `test`만 실행하면 동시성 검증은 포함되지 않는다.

### Android 단위 테스트

```bash
cd android
./gradlew testDebugUnitTest
```

### 관련 성능 테스트만 실행

```bash
cd backend

./gradlew performanceTest --tests "com.vegan.api.order.OrderApiPerformanceTest"
./gradlew performanceTest --tests "com.vegan.api.product.ProductCacheApiPerformanceTest"
./gradlew performanceTest --tests "com.vegan.api.product.ProductPaginationApiPerformanceTest"
./gradlew performanceTest --tests "com.vegan.api.product.ProductSearchIndexPerformanceTest"
```

`performance` 태그만 실행하며 HTTP 응답시간·SQL 수·쿼리 실행시간 등 성능 비교를 수행한다. 일반 CI에서는 실행하지 않고 관련 변경 시 선택 실행한다. 기존 측정 결과는 보존하며, 새 측정은 환경과 조건을 함께 기록한다.

| 구분 | 태그 선택 | 실행 명령 | PR CI |
| --- | --- | --- | --- |
| 일반 회귀 | `concurrency`, `performance` 제외 | `./gradlew test` | 자동 실행 |
| 동시성 정합성 | `concurrency` 포함, `performance` 제외 | `./gradlew concurrencyTest` | 별도 job으로 자동 실행 |
| 성능 측정 | `performance` 포함, `concurrency` 제외 | `./gradlew performanceTest` | 선택 실행 |

## PR 기록 양식

```markdown
### AI 제안 및 변경
- AI가 제안한 내용:
- 실제 변경 파일과 메서드:

### 영향도 판단
- 영향받는 기능:
- 선택한 테스트:
- 선택 이유:

### 검증 결과
- 관련 테스트:
- 일반 회귀 테스트:
- 동시성 정합성 테스트:
- 성능·실행계획:
- 기존 응답과의 동일성:

### 최종 판단
- 채택/기각:
- 근거:
- 사람이 확인한 내용:
```

## 현재 자동화 범위

- PR에서 백엔드 일반 회귀 테스트, 동시성 정합성 테스트, Android 단위 테스트를 자동 실행한다.
- 동시성 job은 실패 여부와 관계없이 생성된 테스트 보고서를 artifact로 보관한다.
- 성능 측정은 `performanceTest`로 분리해 필요한 경우 선택 실행한다.
- 변경 파일 기반 테스트 자동 선별과 AI의 자동 재수정 루프는 아직 구현하지 않았다.
