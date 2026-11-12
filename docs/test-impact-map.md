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

## 변경 파일 기반 테스트 추천

`scripts/select-tests.py`는 Git 변경 파일을 읽어 우선 실행할 명령과 선택 이유를 출력한다. Git과 Python 3.9 이상이 필요하며 추가 Python 패키지는 필요 없다. 테스트나 출력된 명령을 자동 실행하지 않는다.

저장소 루트의 PowerShell에서:

```powershell
# 커밋 전 변경: 스테이징·미스테이징·추적되지 않은 파일 포함
python scripts/select-tests.py

# 브랜치에 커밋한 변경까지 확인 (먼저 비교 기준 갱신)
git fetch origin main
python scripts/select-tests.py --base origin/main

# 다른 OS에서도 Windows 명령 형식으로 출력
python scripts/select-tests.py --shell powershell

# 자동화 도구에서 읽을 JSON 출력
python scripts/select-tests.py --base origin/main --json
```

Windows에서 `python` 명령이 없고 Python Launcher가 설치되어 있다면 `py -3`로 대체한다. Linux/macOS에서는 `python3`로 실행할 수 있으며, 기본 출력은 Bash 명령이다. 출력된 명령은 저장소 루트에서 실행한다.

기본 비교는 `HEAD`와 현재 작업 파일 사이의 변경이다. `--base`는 지정한 ref와 `HEAD`의 공통 조상부터 현재 작업 파일까지 비교하여 브랜치 커밋과 커밋 전 변경을 함께 포함한다. 스크립트가 원격 정보를 가져오지는 않는다. 삭제된 파일도 포함하며 이름 변경은 이전·이후 경로를 모두 검사한다. Git이 무시하는 파일은 제외한다. 잘못된 비교 기준이나 Git 오류는 종료 코드 2로 알린다.

### 첫 버전의 선택 규칙

| 변경 파일 | 우선 추천 | 별도 성능 측정 후보 |
| --- | --- | --- |
| `OrderService.java` | `OrderServiceTest`, `OrderPersistenceIntegrationTest`, `concurrencyTest` | 없음 |
| `Product.java`, `ProductRepository.java` | 위 주문 테스트 2개 및 `concurrencyTest` | 검색 인덱스 측정 |
| `OrderRepository.java`, `Orders.java`, `OrderItem.java` | `OrderPersistenceIntegrationTest`, `concurrencyTest` | 주문 API 측정 |
| `ProductService.java`, `ProductController.java` | 백엔드 일반 회귀; Controller는 Android 검사도 포함 | 페이지네이션 측정 |
| `android/**` | Android 단위 테스트 | 없음 |
| 선택 스크립트 및 `scripts/tests/**` | Python 검증 테스트 | 없음 |
| `docs/**`, 루트 `README.md`·`LICENSE`·`LICENSE.md` | 없음 | 없음 |
| 설정·의존성·보안·나머지 매핑되지 않은 파일 | 백엔드 전체 일반·동시성 및 Android 검사 | 캐시·Redis 경로는 캐시 측정; 성능 테스트 파일은 해당 측정 |

여러 변경의 검사를 합치고 중복은 제거한다. 백엔드 전체 일반 회귀가 필요하면 개별 클래스 필터 대신 `test`를 추천한다. 성능 명령은 우선 회귀 명령과 구분해 출력한다.

이는 메서드 호출 관계를 분석하는 도구가 아니라 위 경로 규칙에 따른 추천이다. 잠금·트랜잭션 변경 여부를 경로만으로 확정할 수 없으므로 주문·재고 핵심 파일은 동시성 검사를 함께 추천한다. 실제 diff와 위 영향도 표를 확인해 추가 검증을 판단한다. Android 단위 테스트는 화면·실기기·서버 연동 검증을 대체하지 않는다.

이 추천은 개발 중 먼저 실행할 검사를 고르는 용도다. PR의 기존 일반·동시성·Android 검사는 추천 결과와 무관하게 모두 실행한다. 성능 수치는 이 도구에서 생성하지 않는다.

스크립트 자체 검증:

```powershell
python -m unittest discover -s scripts/tests -v
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
- 변경 파일 기반 우선 테스트 추천은 `scripts/select-tests.py`로 제공한다. 추천 명령 자동 실행과 AI의 자동 재수정 루프는 아직 구현하지 않았다.
- 추천 스크립트의 Python 검증 테스트를 별도 CI job에서 실행한다. 기존 필수 검사 3개의 실행 범위는 유지한다.
