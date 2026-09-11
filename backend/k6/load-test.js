// k6 부하 테스트: 상품 조회 API
// 실행: k6 run k6/load-test.js
// (Redis 캐시 효과를 보려면: 캐시 켠 상태 vs spring.cache.type=none 상태로 각각 돌려서 비교)
import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  // 단계별 부하: 30초 동안 20명까지 증가 -> 1분 유지 -> 30초 동안 50명까지 -> 종료
  stages: [
    { duration: "30s", target: 20 },
    { duration: "1m", target: 20 },
    { duration: "30s", target: 50 },
    { duration: "30s", target: 0 },
  ],
  // 성능 기준(threshold): 이걸 못 지키면 테스트 실패로 표시
  thresholds: {
    http_req_duration: ["p(95)<500"], // 요청의 95%가 500ms 이내
    http_req_failed: ["rate<0.01"],   // 실패율 1% 미만
  },
};

const BASE = "http://localhost:8080";

export default function () {
  // 시나리오: 사용자가 메인 -> 카테고리 -> 베스트 -> 검색을 둘러봄

  let res = http.get(`${BASE}/api/products`);
  check(res, { "전체 상품 200": (r) => r.status === 200 });

  res = http.get(`${BASE}/api/products?category=101`);
  check(res, { "카테고리 200": (r) => r.status === 200 });

  res = http.get(`${BASE}/api/products/best`);
  check(res, { "베스트셀러 200": (r) => r.status === 200 });

  res = http.get(`${BASE}/api/products?page=0&size=20`);
  check(res, { "페이지 조회 200": (r) => r.status === 200 });

  sleep(1); // 사용자가 화면을 보는 시간 흉내
}
