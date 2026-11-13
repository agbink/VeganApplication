"""
상품 더미 데이터용 샘플 사진을 Wikimedia Commons에서 내려받는다.

- 품목(두부스틱, 립밤 등)마다 무료 라이선스 사진을 최대 2장 받는다.
- 허용 라이선스: CC0, Public domain, CC BY, CC BY-SA (출처 표기 필요)
- 받은 사진의 작가·라이선스·원본 주소를 manifest.json과 ATTRIBUTION.md로 남긴다.

사용법:
    python -I scripts/sample_images/download_sample_images.py backend/uploads/samples
    # 일부 품목만 다른 검색어로 다시 받기 (기존 사진·manifest는 유지)
    python -I scripts/sample_images/download_sample_images.py backend/uploads/samples cheese=cheese "serum=dropper bottle"

품목과 맞지 않는 사진은 파일을 지우고 rejected.json에 원본 제목을 넣으면 다시 받지 않는다.

검수를 마친 최종 목록(scripts/sample_images/manifest.json)대로 똑같이 받기:
    python -I scripts/sample_images/download_sample_images.py backend/uploads/samples --from-manifest scripts/sample_images/manifest.json
"""
import html
import json
import os
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

API = "https://commons.wikimedia.org/w/api.php"
USER_AGENT = "VeganApplicationSampleImages/1.0 (local development script)"
PER_ITEM = 2
THUMB_WIDTH = 640

# key: 파일 이름에 쓰는 품목 키, query: Commons 검색어
ITEMS = [
    # 푸드
    ("tofu", "fried tofu"), ("nuggets", "vegan nuggets"), ("dumplings", "dumplings"),
    ("oatmilk", "oat milk"), ("almondmilk", "almond milk"), ("ramen", "ramen noodles bowl"),
    ("tempeh", "tempeh"), ("curry", "chickpea curry"), ("cheese", "vegan cheese"),
    ("granola", "granola"), ("friedrice", "mushroom fried rice"), ("tteokbokki", "tteokbokki"),
    ("yogurt", "soy yogurt"), ("pasta", "creamy pasta"), ("sausage", "vegan sausage"),
    # 패션
    ("leatherjacket", "leather jacket"), ("tshirt", "t-shirt"), ("jeans", "jeans denim"),
    ("wallet", "leather wallet"), ("totebag", "leather tote bag"), ("linenshirt", "linen shirt"),
    ("dress", "dress fashion"), ("puffer", "down jacket"), ("sneakers", "canvas sneakers"),
    ("socks", "socks"), ("ecobag", "canvas tote bag"), ("knit", "knitted sweater"),
    # 뷰티
    ("aloe", "aloe vera gel"), ("lipbalm", "lip balm"), ("foundation", "foundation makeup"),
    ("eyeliner", "eyeliner pencil"), ("cream", "face cream jar"), ("cleansingoil", "cleansing oil"),
    ("sunscreen", "sunscreen"), ("bodywash", "shower gel"), ("shampoobar", "shampoo bar"),
    ("tonerpad", "cotton pads"), ("serum", "serum dropper bottle"), ("handcream", "hand cream"),
]

LICENSE_RANK = [
    (re.compile(r"^(cc0|public domain|pdm)", re.I), 0),
    (re.compile(r"^cc by \d", re.I), 1),
    (re.compile(r"^cc by-sa \d", re.I), 2),
]


def license_rank(name):
    for pattern, rank in LICENSE_RANK:
        if pattern.search(name or ""):
            return rank
    return None


def fetch(url, timeout):
    """429(요청 과다)·일시 오류면 점점 길게 기다렸다가 다시 시도한다."""
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    for attempt in range(6):
        try:
            with urllib.request.urlopen(request, timeout=timeout) as response:
                return response.read()
        except urllib.error.HTTPError as error:
            if error.code not in (429, 503) or attempt == 5:
                raise
            wait = int(error.headers.get("Retry-After") or 0) or 5 * (2 ** attempt)
            print(f"  rate limited, waiting {wait}s")
            time.sleep(wait)


def get_json(params):
    return json.loads(fetch(API + "?" + urllib.parse.urlencode(params), 30))


def strip_html(value):
    return html.unescape(re.sub(r"<[^>]+>", "", value or "")).strip()


def search(query):
    data = get_json({
        "action": "query", "format": "json", "generator": "search",
        "gsrnamespace": 6, "gsrsearch": f"{query} filetype:bitmap", "gsrlimit": 30,
        "prop": "imageinfo", "iiprop": "url|extmetadata|mime|size", "iiurlwidth": THUMB_WIDTH,
    })
    candidates = []
    for page in (data.get("query", {}).get("pages", {}) or {}).values():
        info = (page.get("imageinfo") or [{}])[0]
        meta = info.get("extmetadata", {})
        lic = meta.get("LicenseShortName", {}).get("value", "")
        rank = license_rank(lic)
        if rank is None or info.get("mime") != "image/jpeg":
            continue
        if info.get("width", 0) < 500 or info.get("height", 0) < 400:
            continue
        candidates.append({
            "rank": rank, "order": page.get("index", 999),
            "title": page["title"], "thumb": info.get("thumburl"),
            "page": info.get("descriptionurl"), "license": lic,
            "license_url": meta.get("LicenseUrl", {}).get("value", ""),
            "artist": strip_html(meta.get("Artist", {}).get("value", "")) or "Unknown",
        })
    # 검색 관련도 순서를 우선하고, 같은 수준이면 제약이 적은 라이선스를 앞에 둔다
    candidates.sort(key=lambda c: (c["order"] // 5, c["rank"], c["order"]))
    return candidates


def download(url, path):
    data = fetch(url, 60)
    if not data.startswith(b"\xff\xd8"):
        raise ValueError("JPEG가 아닌 응답")
    path.write_bytes(data)


def write_atomic(path, text):
    """임시 파일에 쓴 뒤 교체해, 쓰다가 실패해도 이전 내용이 남게 한다."""
    temp = path.with_name(path.name + ".tmp")
    temp.write_text(text, encoding="utf-8")
    os.replace(temp, path)


def save_metadata(out_dir, manifest):
    """manifest.json과 ATTRIBUTION.md를 현재 목록 기준으로 저장한다 (사진을 받을 때마다 호출)."""
    manifest = sorted(manifest, key=lambda m: m["file"])
    write_atomic(out_dir / "manifest.json", json.dumps(manifest, ensure_ascii=False, indent=2))
    lines = ["# 샘플 상품 사진 출처", "",
             "Wikimedia Commons에서 받은 무료 라이선스 사진입니다. 원본을 640px 너비로 축소한 것 외에는 수정하지 않았습니다.", "",
             "| 파일 | 원본 | 작가 | 라이선스 |", "|---|---|---|---|"]
    for m in manifest:
        lic = f"[{m['license']}]({m['license_url']})" if m["license_url"] else m["license"]
        lines.append(f"| {m['file']} | [{m['title']}]({m['page']}) | {m['artist'].replace('|', '/')} | {lic} |")
    write_atomic(out_dir / "ATTRIBUTION.md", "\n".join(lines) + "\n")


def restore(out_dir, manifest_file):
    """manifest에 적힌 원본 제목으로 같은 사진을 같은 파일 이름으로 다시 받는다."""
    manifest = json.loads(Path(manifest_file).read_text(encoding="utf-8"))
    for entry in manifest:
        data = get_json({"action": "query", "format": "json", "titles": entry["title"],
                         "prop": "imageinfo", "iiprop": "url", "iiurlwidth": THUMB_WIDTH})
        page = next(iter(data["query"]["pages"].values()))
        download(page["imageinfo"][0]["thumburl"], out_dir / entry["file"])
        print(entry["file"])
        time.sleep(2)
    save_metadata(out_dir, manifest)
    print(f"restored {len(manifest)} images -> {out_dir}")


def main():
    out_dir = Path(sys.argv[1] if len(sys.argv) > 1 else "backend/uploads/samples")
    out_dir.mkdir(parents=True, exist_ok=True)
    if len(sys.argv) > 3 and sys.argv[2] == "--from-manifest":
        restore(out_dir, sys.argv[3])
        return
    overrides = [tuple(arg.split("=", 1)) for arg in sys.argv[2:]]
    items = overrides or ITEMS

    manifest_path = out_dir / "manifest.json"
    rejected_path = out_dir / "rejected.json"
    rejected = set(json.loads(rejected_path.read_text(encoding="utf-8"))) if rejected_path.exists() else set()
    # 기본 실행이든 보충 실행이든 기존 목록을 이어서 쓴다.
    # (처음부터 다시 받으려면 폴더를 비우고 실행)
    manifest = json.loads(manifest_path.read_text(encoding="utf-8")) if manifest_path.exists() else []
    # 지워진 파일은 manifest에서도 뺀다
    manifest = [m for m in manifest if (out_dir / m["file"]).exists()]
    used_titles = {m["title"] for m in manifest} | rejected

    for key, query in items:
        saved = sum(1 for m in manifest if m["item"] == key)
        if saved >= PER_ITEM:
            print(f"{key:14s} {saved} image(s) (already)")
            continue
        for candidate in search(query):
            if saved >= PER_ITEM or not candidate["thumb"]:
                break
            if candidate["title"] in used_titles:
                continue
            index = 1
            while (out_dir / f"{key}_{index}.jpg").exists():
                index += 1
            file_name = f"{key}_{index}.jpg"
            try:
                download(candidate["thumb"], out_dir / file_name)
            except Exception as error:  # 한 장 실패해도 다음 후보로 진행
                print(f"  skip {candidate['title']}: {error}")
                continue
            manifest.append({"file": file_name, "item": key, "query": query, **{
                k: candidate[k] for k in ("title", "page", "artist", "license", "license_url")}})
            used_titles.add(candidate["title"])
            saved += 1
            # 중간에 실패해도 받은 사진의 출처가 남도록 한 장마다 저장
            save_metadata(out_dir, manifest)
            time.sleep(2)
        print(f"{key:14s} {saved} image(s)")
        time.sleep(2)

    save_metadata(out_dir, manifest)
    print(f"total {len(manifest)} images -> {out_dir}")


if __name__ == "__main__":
    main()
