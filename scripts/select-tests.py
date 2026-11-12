#!/usr/bin/env python3
"""Print a test plan from Git changes; never execute tests or modify files."""

import argparse
import json
import os
from pathlib import Path
import subprocess
import sys


JAVA = "backend/src/main/java/com/vegan/api/"
ORDER = "com.vegan.api.order."
PRODUCT = "com.vegan.api.product."
PERFORMANCE = {
    "OrderApiPerformanceTest": ORDER,
    "ProductCacheApiPerformanceTest": PRODUCT,
    "ProductPaginationApiPerformanceTest": PRODUCT,
    "ProductSearchIndexPerformanceTest": PRODUCT,
}


def git(repo, *args):
    result = subprocess.run(
        ["git", "-C", str(repo), *args], capture_output=True, check=False
    )
    if result.returncode:
        raise ValueError(result.stderr.decode("utf-8", errors="replace").strip())
    return result.stdout


def changed_files(repo, base=None):
    # No rename detection: both old and new paths must participate in selection.
    start = "HEAD"
    if base:
        start = git(repo, "merge-base", "HEAD", base).decode().strip()
    tracked = git(repo, "diff", "--name-only", "--no-renames", "-z", start, "--")
    untracked = git(repo, "ls-files", "--others", "--exclude-standard", "-z")
    return sorted({os.fsdecode(p) for p in (tracked + untracked).split(b"\0") if p})


def select(paths):
    plan = {"changed_files": sorted(set(paths)), "reasons": [], "tests": [],
            "full_backend": False, "concurrency": False, "android": False,
            "selector_tests": False, "performance_tests": []}
    tests, performance = set(), set()
    for path in plan["changed_files"]:
        stem = Path(path).stem
        if path == "scripts/select-tests.py" or path.startswith("scripts/tests/"):
            plan["selector_tests"] = True
            reason = "테스트 선택 스크립트 변경: Python 검증"
        elif path.startswith("docs/") or path in {"README.md", "LICENSE", "LICENSE.md"}:
            reason = "문서 변경: 실행 테스트 추천 없음"
        elif path == JAVA + "order/OrderService.java" or path in {
            JAVA + "product/Product.java", JAVA + "product/ProductRepository.java"
        }:
            tests.update({ORDER + "OrderServiceTest", ORDER + "OrderPersistenceIntegrationTest"})
            plan["concurrency"] = True
            reason = "주문·재고 잠금 영향: 주문 단위·통합 및 동시성 검증"
            if path.startswith(JAVA + "product/"):
                performance.add(PRODUCT + "ProductSearchIndexPerformanceTest")
        elif path in {JAVA + "order/" + name for name in
                      ("OrderRepository.java", "Orders.java", "OrderItem.java")}:
            tests.add(ORDER + "OrderPersistenceIntegrationTest")
            plan["concurrency"] = True
            performance.add(ORDER + "OrderApiPerformanceTest")
            reason = "주문 영속성 영향: DB 저장·조회 및 동시 주문 검증"
        elif path.startswith("android/"):
            plan["android"] = True
            reason = "Android 변경: Android 단위 테스트 (화면·실기기 검증은 별도)"
        elif path in {JAVA + "product/ProductService.java", JAVA + "product/ProductController.java"}:
            plan["full_backend"] = True
            performance.add(PRODUCT + "ProductPaginationApiPerformanceTest")
            if stem.endswith("Controller"):
                plan["android"] = True
            reason = "상품 API 영향: 백엔드 일반 회귀 및 필요 시 페이지네이션 측정"
        else:
            # Unknown backend paths include settings/security/new code. Do not
            # claim that an unmapped file has no test impact.
            plan["full_backend"] = True
            plan["concurrency"] = True
            plan["android"] = True
            reason = "공통 설정 또는 개별 매핑이 없는 변경: 전체 일반·동시성·Android 검증"
        if path.startswith("backend/"):
            if "cache" in path.lower() or "redis" in path.lower():
                performance.add(PRODUCT + "ProductCacheApiPerformanceTest")
            if stem in PERFORMANCE:
                performance.add(PERFORMANCE[stem] + stem)
        plan["reasons"].append({"path": path, "reason": reason})
    plan["tests"] = sorted(tests) if not plan["full_backend"] else []
    plan["performance_tests"] = sorted(performance)
    return plan


def commands(plan, shell):
    def gradle(project, task, filters=()):
        prefix = (f".\\{project}\\gradlew.bat -p {project}" if shell == "powershell"
                  else f"bash ./{project}/gradlew -p {project}")
        return prefix + " " + task + "".join(f' --tests "{t}"' for t in filters)

    selected = []
    if plan["full_backend"] or plan["tests"]:
        selected.append(gradle("backend", "test", plan["tests"]))
    if plan["concurrency"]:
        selected.append(gradle("backend", "concurrencyTest"))
    if plan["android"]:
        selected.append(gradle("android", "testDebugUnitTest"))
    if plan["selector_tests"]:
        selected.append("python -m unittest discover -s scripts/tests -v")
    optional = [gradle("backend", "performanceTest", [t]) for t in plan["performance_tests"]]
    return selected, optional


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base", help="Include branch changes since merge-base with this ref; no fetch")
    parser.add_argument("--shell", choices=("powershell", "posix"),
                        default="powershell" if os.name == "nt" else "posix")
    parser.add_argument("--json", action="store_true", help="Print a machine-readable plan")
    args = parser.parse_args()
    try:
        repo = Path(__file__).resolve().parents[1]
        plan = select(changed_files(repo, args.base))
    except (ValueError, OSError) as exc:
        print(f"변경 파일 확인 실패: {exc}", file=sys.stderr)
        return 2
    plan["commands"], plan["optional_performance_commands"] = commands(plan, args.shell)
    if args.json:
        print(json.dumps(plan, ensure_ascii=False, indent=2))
    else:
        print("테스트 추천 (명령 출력만 수행, 테스트를 실행하지 않음)")
        print("명령은 저장소 루트에서 실행하세요.")
        if not plan["changed_files"]:
            print("비교 범위에 변경 파일이 없습니다. 커밋한 브랜치 변경은 --base origin/main을 사용하세요.")
        for item in plan["reasons"]:
            print(f"\n변경: {item['path']}\n이유: {item['reason']}")
        print("\n우선 실행할 명령:")
        print("\n".join(plan["commands"]) or "없음")
        if plan["optional_performance_commands"]:
            print("\n성능 관련 변경일 때 별도 선택 실행 (필수 회귀 검사와 구분):")
            print("\n".join(plan["optional_performance_commands"]))
        print("\n파일 경로 기반 추천입니다. 실제 변경 내용과 docs/test-impact-map.md도 확인하세요.")
        print("추천 명령은 PR의 필수 검사 3개를 대체하지 않습니다.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
