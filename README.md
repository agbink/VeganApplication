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
- k6, Actuator, Prometheus, Grafana 기반 관측 환경

## 기술 스택

| 영역 | 기술 |
|---|---|
| Android | Java, XML, Retrofit, OkHttp, Glide |
| Backend | Java 17, Spring Boot 3.2.5, Spring Data JPA |
| Data | MySQL, Redis |
| Auth | JWT, BCrypt, Google/Naver OAuth |
| Observability | k6, Actuator, Prometheus, Grafana |

## 로컬 실행

### Backend

1. `backend/src/main/resources/application.example.properties`를 `application.properties`로 복사합니다.
2. DB와 JWT 환경값을 로컬 환경에 맞게 설정합니다.
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
