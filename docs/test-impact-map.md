# 변경 영향도 및 테스트 매핑

AI가 제안하거나 생성한 변경을 바로 반영하지 않고, 변경 범위에 맞는 테스트와 추가 검증을 선택하기 위한 기준이다.

## 검증 원칙

1. `git diff`로 변경 파일과 핵심 메서드를 확인한다.
2. 아래 표에서 영향 기능과 우선 테스트를 선택한다.
3. 우선 테스트가 통과하면 일반 회귀 테스트를 실행한다.
4. 성능·동시성·인덱스 변경은 별도 `performanceTest`로 검증한다.
5. 테스트 결과와 채택·기각 근거를 PR에 남긴 뒤 사람이 최종 승인한다.

## 변경 영향도 표

| 변경 영역 | 주요 영향 | 우선 실행 테스트 | 추가 검증 |
| --- | --- | --- | --- |
| `backend/**/order/OrderService.java` | 주문 생성, 수량 검증, 상품 잠금 순서, 재고 차감 | `OrderServiceTest`, `OrderPersistenceIntegrationTest` | 동시성 변경 시 아래 성능 태그 테스트 실행 |
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

일반 테스트는 `performance` 태그를 제외한다. PR의 GitHub Actions에서도 동일한 명령을 실행한다.

### Android 단위 테스트

```bash
cd android
./gradlew testDebugUnitTest
```

### 관련 성능 테스트만 실행

```bash
cd backend

./gradlew performanceTest --tests "com.vegan.api.order.OrderApiPerformanceTest"
./gradlew performanceTest --tests "com.vegan.api.order.OrderConcurrencyIntegrationTest"
./gradlew performanceTest --tests "com.vegan.api.order.MultiProductOrderConcurrencyTest"
./gradlew performanceTest --tests "com.vegan.api.product.ProductCacheApiPerformanceTest"
./gradlew performanceTest --tests "com.vegan.api.product.ProductPaginationApiPerformanceTest"
./gradlew performanceTest --tests "com.vegan.api.product.ProductSearchIndexPerformanceTest"
```

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
- 전체 회귀 테스트:
- 성능·실행계획:
- 기존 응답과의 동일성:

### 최종 판단
- 채택/기각:
- 근거:
- 사람이 확인한 내용:
```

## 현재 자동화 범위

- PR에서 백엔드 일반 회귀 테스트와 Android 단위 테스트를 자동 실행한다.
- 성능·동시성 테스트는 비용과 실행시간을 고려해 `performanceTest`로 분리해 필요한 경우 선택 실행한다.
- 변경 파일 기반 테스트 자동 선별과 AI의 자동 재수정 루프는 아직 구현하지 않았다.
