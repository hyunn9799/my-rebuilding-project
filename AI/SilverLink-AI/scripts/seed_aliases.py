"""
초기 alias seed 스크립트

medications_master에 이미 존재하는 약품명에서
자주 사용되는 줄임말, 괄호 내 별칭, 영문-한글 변환 형태를
medication_aliases / medication_error_aliases 에 초기 데이터로 삽입한다.

사용법:
    python -m scripts.seed_aliases
"""
import re
from typing import Dict, List, Set, Tuple

import pymysql
from loguru import logger

from app.core.config import configs


# ─────────────────────────────────────────────
# 흔한 제형 접미사 (alias 생성 시 제거 대상)
# ─────────────────────────────────────────────
FORM_SUFFIXES = [
    "정", "캡슐", "환", "산", "시럽",
    "현탁액", "액", "과립", "주", "크림",
    "연고", "겔", "패치", "필름코팅정", "서방정",
    "서방캡슐", "츄어블정", "장용정", "캡슐제",
]

# ─────────────────────────────────────────────
# OCR에서 흔히 발생하는 글자 혼동 맵
# ─────────────────────────────────────────────
OCR_ERROR_MAP: Dict[str, List[str]] = {
    "O": ["0"],        # 영문 O ↔ 숫자 0
    "0": ["O"],
    "l": ["1", "I"],   # 소문자 L ↔ 숫자 1 / 대문자 I
    "1": ["l", "I"],
    "I": ["l", "1"],
    "B": ["8"],
    "8": ["B"],
    "S": ["5"],
    "5": ["S"],
    "Z": ["2"],
    "2": ["Z"],
    "G": ["6"],
    "6": ["G"],
}


def _get_connection():
    return pymysql.connect(
        host=configs.RDS_HOST,
        port=configs.RDS_PORT,
        user=configs.RDS_USER,
        password=configs.RDS_PASSWORD,
        database=configs.RDS_DATABASE,
        charset="utf8mb4",
        cursorclass=pymysql.cursors.DictCursor,
    )


def _fetch_all_drugs(conn) -> List[dict]:
    """medications_master에서 전체 약품 조회."""
    with conn.cursor() as cur:
        cur.execute("SELECT item_seq, item_name, item_name_normalized, entp_name FROM medications_master")
        return cur.fetchall()


# ─────────────────────────────────────────────
# Alias 생성 규칙
# ─────────────────────────────────────────────

def generate_aliases(item_name: str) -> Set[str]:
    """
    약품명에서 가능한 alias 후보를 생성한다.

    규칙:
    1. 괄호 안 내용 제거한 이름 → alias  (예: "타이레놀정500mg(아세트아미노펜)" → "타이레놀정500mg")
    2. 제형+용량 제거한 브랜드명 → alias  (예: "타이레놀정500mg" → "타이레놀")
    3. 용량만 제거한 이름 → alias          (예: "타이레놀정500mg" → "타이레놀정")
    """
    aliases: Set[str] = set()
    name = item_name.strip()

    # 1. 괄호 안 내용 제거
    no_paren = re.sub(r"[\(（][^)）]*[\)）]", "", name).strip()
    if no_paren and no_paren != name and len(no_paren) >= 2:
        aliases.add(no_paren)

    # 2. 제형 + 용량 제거 → 브랜드명
    base = no_paren or name
    brand = re.sub(r"\d+\.?\d*\s*(mg|ml|mcg|g|iu|밀리그램|밀리리터|마이크로그램|그램)", "", base, flags=re.IGNORECASE).strip()
    for suffix in FORM_SUFFIXES:
        if brand.endswith(suffix):
            brand = brand[:-len(suffix)].strip()
            break
    if brand and brand != name and len(brand) >= 2:
        aliases.add(brand)

    # 3. 용량만 제거
    no_dose = re.sub(r"\d+\.?\d*\s*(mg|ml|mcg|g|iu|밀리그램|밀리리터|마이크로그램|그램)", "", base, flags=re.IGNORECASE).strip()
    if no_dose and no_dose != name and no_dose != brand and len(no_dose) >= 2:
        aliases.add(no_dose)

    # 원본과 동일한 것은 제거
    aliases.discard(name)
    return aliases


def generate_error_aliases(item_name: str) -> List[Tuple[str, str]]:
    """
    약품명에서 OCR 오인식 변형을 생성한다.

    Returns:
        List[(error_text, reason)]
    """
    errors: List[Tuple[str, str]] = []
    name = item_name.strip()

    for i, ch in enumerate(name):
        if ch in OCR_ERROR_MAP:
            for replacement in OCR_ERROR_MAP[ch]:
                error_text = name[:i] + replacement + name[i + 1:]
                if error_text != name:
                    reason = f"OCR confusion: '{ch}' → '{replacement}'"
                    errors.append((error_text, reason))

    # 숫자 사이 공백 삽입 (예: "500mg" → "5 0 0 mg")
    spaced = re.sub(r"(\d)", r"\1 ", name).strip()
    if spaced != name and len(spaced) > len(name):
        errors.append((spaced, "OCR spacing in digits"))

    return errors


# ─────────────────────────────────────────────
# Upsert helpers
# ─────────────────────────────────────────────

def _upsert_alias(conn, item_seq: str, alias_name: str, alias_normalized: str):
    with conn.cursor() as cur:
        cur.execute(
            """
            INSERT INTO medication_aliases (item_seq, alias_name, alias_normalized, source)
            VALUES (%s, %s, %s, 'seed')
            ON DUPLICATE KEY UPDATE
                alias_normalized = VALUES(alias_normalized),
                source = 'seed',
                updated_at = CURRENT_TIMESTAMP
            """,
            (item_seq, alias_name, alias_normalized),
        )


def _upsert_error_alias(conn, item_seq: str, error_text: str, normalized: str, reason: str):
    with conn.cursor() as cur:
        cur.execute(
            """
            INSERT INTO medication_error_aliases
                (item_seq, error_text, normalized_error_text, correction_reason, confidence, source)
            VALUES (%s, %s, %s, %s, 0.85, 'seed')
            ON DUPLICATE KEY UPDATE
                normalized_error_text = VALUES(normalized_error_text),
                correction_reason = VALUES(correction_reason),
                source = 'seed',
                updated_at = CURRENT_TIMESTAMP
            """,
            (item_seq, error_text, normalized, reason),
        )


# ─────────────────────────────────────────────
# Main
# ─────────────────────────────────────────────

def main():
    conn = _get_connection()
    try:
        drugs = _fetch_all_drugs(conn)
        logger.info(f"medications_master에서 {len(drugs)}개 약품 로드됨")

        alias_count = 0
        error_alias_count = 0

        for drug in drugs:
            item_seq = drug["item_seq"]
            item_name = drug["item_name"]
            normalized = drug.get("item_name_normalized") or item_name

            # --- alias ---
            aliases = generate_aliases(item_name)
            if normalized != item_name:
                aliases |= generate_aliases(normalized)
            for alias in aliases:
                alias_norm = alias.lower().replace(" ", "")
                _upsert_alias(conn, item_seq, alias, alias_norm)
                alias_count += 1

            # --- error alias ---
            error_aliases = generate_error_aliases(normalized or item_name)
            for error_text, reason in error_aliases:
                error_norm = error_text.lower().replace(" ", "")
                _upsert_error_alias(conn, item_seq, error_text, error_norm, reason)
                error_alias_count += 1

        conn.commit()
        logger.info(
            f"Seed 완료 — alias: {alias_count}건, error_alias: {error_alias_count}건 upsert"
        )
    except Exception as e:
        conn.rollback()
        logger.error(f"Seed 실패: {e}")
        raise
    finally:
        conn.close()


if __name__ == "__main__":
    main()
