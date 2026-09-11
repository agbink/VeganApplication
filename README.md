# VeganApplication

Android Studio와 Firebase로 개발한 비건 쇼핑 앱을 Spring Boot REST API와 MySQL 기반 구조로 전환한 프로젝트입니다.

## 저장소 구조

```text
VeganApplication/
├── android/   # Android 앱(Java/XML, Retrofit)
├── backend/   # Spring Boot API(JPA, MySQL, Redis)
└── docs/      # 전환 및 성능 개선 기록
```

저장소 루트는 Gradle composite build로 구성되어 있습니다. IntelliJ IDEA에서 이 루트 폴더 하나만 열면 Android와 Backend 빌드가 함께 연결됩니다. 두 애플리케이션은 배포와 실행 주기가 다르므로 빌드는 독립적으로 유지합니다.

## 주요 기능

- 이메일·Google·Naver 로그인과 JWT 인증
- 상품 목록·카테고리·검색·베스트셀러 조회
- 장바구니 추가·수량 변경·삭제
- 주문 생성과 재고·판매량 갱신
- 리뷰 작성·조회·삭제
- 회원·주문·상품·리뷰 관리 기능
- 이미지 업로드
- Redis 상품 조회 캐시와 TTL/무효화
- Fetch Join을 통한 주문 조회 N+1 방지
- 인덱스·페이지네이션·Android 무한 스크롤
- 비관적 락과 상품 ID 기준 잠금 순서 통일로 단일·다중 상품 주문 정합성 보호
- k6, Actuator, Prometheus, Grafana 기반 관측 환경

## 기술 스택

| 영역 | 기술 |
|---|---|
| Android | Java, XML, Retrofit, OkHttp, Glide |
| Backend | Java 17, Spring Boot 3.2.5, Spring Data JPA |
| Data | MySQL, Redis |
| Auth | JWT, BCrypt, Google/Naver OAuth |
| Observability | k6, Actuator, Prometheus, Grafana |

## 검증된 개선 결과

- 상품 10,000건 전체 조회를 20건 페이지 조회로 변경해 평균 응답시간 **82.0% 감소**, JSON 응답 크기 **99.8% 감소**
- 주문 10건·주문상품 30건 조회에서 Fetch Join으로 SQL 실행 횟수 **41회 → 1회** 감소
- 상품 1,000건 Redis Cache Hit에서 평균 응답시간 **38.9% 감소**, SQL 실행 **50회 → 0회** 확인
- 재고 10개 상품에 20개 동시 주문을 30회 재현하고, 비관적 락 적용 후 정합성 실패와 DB 락 충돌 **0건** 확인
- 다중 상품 A→B/B→A 교차 주문에서 잠금 순서를 상품 ID 기준으로 통일해 데드락 **30건 → 0건**
- `%keyword%` 검색은 B-tree 인덱스 적용 후에도 전체 스캔임을 `EXPLAIN ANALYZE`로 확인해 불필요한 인덱스 도입 보류

상세한 측정 조건과 한계는 [성능 최적화 노트](docs/performance-optimization-notes.md)에 기록했습니다.

## 로컬 실행

### Backend

1. `backend/src/main/resources/application.example.properties`를 `application.properties`로 복사합니다.
2. DB와 JWT 환경값을 로컬 환경에 맞게 설정합니다.
3. 관리자 계정으로 사용할 이메일을 `ADMIN_EMAIL`에 지정합니다. 비워두면 관리자 API는 모두 차단됩니다.
3. 다음 명령으로 실행합니다.

```bash
./backend/gradlew -p backend bootRun
```

### Android

1. 로컬 SDK 경로가 담긴 `android/local.properties`를 준비합니다.
2. 에뮬레이터는 기본 Backend 주소 `http://10.0.2.2:8080/`을 사용합니다. 실제 기기에서는 로컬 `android/gradle.properties`에 `VEGAN_API_BASE_URL=http://내-PC-IP:8080/`을 설정합니다.
3. 다음 명령으로 빌드합니다.

```bash
./android/gradlew -p android assembleDebug
```

## 설정 파일 관리

다음 파일은 Git에 커밋하지 않습니다.

- `android/local.properties`
- `android/app/google-services.json`
- `backend/src/main/resources/application.properties`
- `.idea/`, `.gradle/`, `build/`

Firebase 기반 초기 구현과 Spring Boot 전환 과정은 기존 Git 이력과 `docs/` 문서에서 확인할 수 있습니다.
