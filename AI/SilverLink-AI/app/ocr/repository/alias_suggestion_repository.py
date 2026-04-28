"""사용자 피드백 기반 alias 제안 저장/관리 레포지토리."""
from __future__ import annotations

from datetime import datetime
from typing import Dict, List, Optional

import pymysql
from loguru import logger

from app.core.config import configs


class AliasSuggestionRepository:
    """medication_alias_suggestions 테이블 CRUD.

    사용자 확인 시 생성된 alias 제안을 PENDING 상태로 저장.
    관리자 승인 전까지 LocalDrugIndex 검색 대상에 포함되지 않음.
    """

    def _get_connection(self):
        return pymysql.connect(
            host=configs.RDS_HOST,
            port=configs.RDS_PORT,
            user=configs.RDS_USER,
            password=configs.RDS_PASSWORD,
            database=configs.RDS_DATABASE,
            charset="utf8mb4",
            cursorclass=pymysql.cursors.DictCursor,
        )

    # ──────────────────────────────────────────────
    # 제안 저장
    # ──────────────────────────────────────────────

    def save_suggestion(
        self,
        item_seq: str,
        alias_name: str,
        alias_normalized: str,
        source_request_id: str,
        source: str = "user_feedback",
        suggestion_type: str = "error_alias",
    ) -> bool:
        """alias 제안을 PENDING 상태로 저장.

        이미 동일한 (item_seq, alias_normalized) 제안이 있으면 frequency를 증가시킨다.

        Args:
            item_seq: 품목기준코드
            alias_name: 제안된 별칭 원문
            alias_normalized: 정규화된 별칭
            source_request_id: 원본 OCR request_id
            source: 출처 (user_feedback/system/ocr_learning)
            suggestion_type: 제안 유형 ('alias' 또는 'error_alias')
        """
        connection = None
        try:
            connection = self._get_connection()
            with connection.cursor() as cursor:
                # UPSERT: 중복 시 frequency 증가
                # UNIQUE KEY uk_suggestion_norm (item_seq, alias_normalized)
                sql = """
                    INSERT INTO medication_alias_suggestions (
                        item_seq, alias_name, alias_normalized,
                        suggestion_type, source, source_request_id,
                        review_status, frequency, is_active
                    ) VALUES (
                        %s, %s, %s, %s, %s, %s, 'PENDING', 1, 0
                    )
                    ON DUPLICATE KEY UPDATE
                        frequency = frequency + 1,
                        source_request_id = VALUES(source_request_id),
                        updated_at = CURRENT_TIMESTAMP
                """
                cursor.execute(sql, (
                    item_seq,
                    alias_name,
                    alias_normalized,
                    suggestion_type,
                    source,
                    source_request_id,
                ))
                connection.commit()
                logger.info(
                    "Alias suggestion saved: item_seq={}, alias='{}', type={}, request_id={}",
                    item_seq,
                    alias_name,
                    suggestion_type,
                    source_request_id,
                )
                return True
        except Exception as e:
            logger.error(
                "Alias suggestion save failed: item_seq={}, alias='{}', error={}",
                item_seq,
                alias_name,
                e,
            )
            return False
        finally:
            if connection:
                connection.close()

    # ──────────────────────────────────────────────
    # 관리자 조회 (페이징)
    # ──────────────────────────────────────────────

    def get_pending_suggestions(self, limit: int = 50) -> List[dict]:
        """PENDING 상태의 alias 제안 목록 조회 (관리자용)."""
        connection = None
        try:
            connection = self._get_connection()
            with connection.cursor() as cursor:
                cursor.execute(
                    """
                    SELECT s.*, m.item_name
                    FROM medication_alias_suggestions s
                    LEFT JOIN medications_master m ON m.item_seq = s.item_seq
                    WHERE s.review_status = 'PENDING'
                    ORDER BY s.frequency DESC, s.created_at DESC
                    LIMIT %s
                    """,
                    (limit,),
                )
                return list(cursor.fetchall())
        except Exception as e:
            logger.error(f"get_pending_suggestions failed: {e}")
            return []
        finally:
            if connection:
                connection.close()

    def get_pending_suggestions_paged(
        self,
        page: int = 1,
        size: int = 20,
        review_status: Optional[str] = "PENDING",
    ) -> Dict:
        """페이징 지원 alias 제안 목록 조회 (관리자 UI용).

        Returns:
            {"items": [...], "total": int, "page": int, "size": int}
        """
        connection = None
        try:
            connection = self._get_connection()
            with connection.cursor() as cursor:
                # 필터 조건
                where_clause = ""
                params: list = []
                if review_status:
                    where_clause = "WHERE s.review_status = %s"
                    params.append(review_status)

                # 전체 건수
                cursor.execute(
                    f"""
                    SELECT COUNT(*) as total
                    FROM medication_alias_suggestions s
                    {where_clause}
                    """,
                    tuple(params),
                )
                total = cursor.fetchone()["total"]

                # 페이징 데이터
                offset = (page - 1) * size
                cursor.execute(
                    f"""
                    SELECT s.*, m.item_name
                    FROM medication_alias_suggestions s
                    LEFT JOIN medications_master m ON m.item_seq = s.item_seq
                    {where_clause}
                    ORDER BY s.frequency DESC, s.created_at DESC
                    LIMIT %s OFFSET %s
                    """,
                    tuple(params + [size, offset]),
                )
                items = list(cursor.fetchall())

                # datetime → str 변환
                for item in items:
                    for key in ("created_at", "updated_at", "reviewed_at"):
                        if isinstance(item.get(key), datetime):
                            item[key] = item[key].isoformat()

                return {
                    "items": items,
                    "total": total,
                    "page": page,
                    "size": size,
                }
        except Exception as e:
            logger.error(f"get_pending_suggestions_paged failed: {e}")
            return {"items": [], "total": 0, "page": page, "size": size}
        finally:
            if connection:
                connection.close()

    # ──────────────────────────────────────────────
    # 관리자 승인/거부
    # ──────────────────────────────────────────────

    def approve_suggestion(
        self, suggestion_id: int, reviewed_by: str = "admin"
    ) -> Dict:
        """제안 승인: suggestion_type에 따라 alias 또는 error_alias 테이블에 등록.

        트랜잭션으로 묶어서 원자적으로 처리한다.

        Returns:
            {"success": bool, "message": str, "target_table": str | None}
        """
        connection = None
        try:
            connection = self._get_connection()
            connection.begin()

            with connection.cursor() as cursor:
                # 1. 제안 조회
                cursor.execute(
                    """
                    SELECT * FROM medication_alias_suggestions
                    WHERE id = %s AND review_status = 'PENDING'
                    FOR UPDATE
                    """,
                    (suggestion_id,),
                )
                suggestion = cursor.fetchone()

                if not suggestion:
                    return {
                        "success": False,
                        "message": f"PENDING 상태의 제안을 찾을 수 없습니다 (id={suggestion_id})",
                        "target_table": None,
                    }

                item_seq = suggestion["item_seq"]
                alias_name = suggestion["alias_name"]
                alias_normalized = suggestion["alias_normalized"] or alias_name
                stype = suggestion.get("suggestion_type", "error_alias")

                # 2. suggestion_type에 따라 대상 테이블에 INSERT
                if stype == "alias":
                    target_table = "medication_aliases"
                    cursor.execute(
                        """
                        INSERT INTO medication_aliases
                            (item_seq, alias_name, alias_normalized, source, is_active)
                        VALUES (%s, %s, %s, 'user', 1)
                        ON DUPLICATE KEY UPDATE
                            is_active = 1,
                            updated_at = CURRENT_TIMESTAMP
                        """,
                        (item_seq, alias_name, alias_normalized),
                    )
                else:
                    # error_alias (기본값)
                    target_table = "medication_error_aliases"
                    cursor.execute(
                        """
                        INSERT INTO medication_error_aliases
                            (item_seq, error_text, normalized_error_text, source, is_active)
                        VALUES (%s, %s, %s, 'user', 1)
                        ON DUPLICATE KEY UPDATE
                            is_active = 1,
                            updated_at = CURRENT_TIMESTAMP
                        """,
                        (item_seq, alias_name, alias_normalized),
                    )

                # 3. 제안 상태 업데이트
                cursor.execute(
                    """
                    UPDATE medication_alias_suggestions
                    SET review_status = 'APPROVED',
                        is_active = 1,
                        reviewed_by = %s,
                        reviewed_at = NOW()
                    WHERE id = %s
                    """,
                    (reviewed_by, suggestion_id),
                )

            connection.commit()
            logger.info(
                "Alias suggestion approved: id={}, type={}, alias='{}' → {}",
                suggestion_id,
                stype,
                alias_name,
                target_table,
            )
            return {
                "success": True,
                "message": f"승인 완료: '{alias_name}' → {target_table}에 등록",
                "target_table": target_table,
            }

        except Exception as e:
            if connection:
                connection.rollback()
            logger.error(f"approve_suggestion failed: id={suggestion_id}, error={e}")
            return {
                "success": False,
                "message": f"승인 처리 실패: {e}",
                "target_table": None,
            }
        finally:
            if connection:
                connection.close()

    def reject_suggestion(
        self, suggestion_id: int, reviewed_by: str = "admin"
    ) -> Dict:
        """제안 거부.

        Returns:
            {"success": bool, "message": str}
        """
        connection = None
        try:
            connection = self._get_connection()
            with connection.cursor() as cursor:
                cursor.execute(
                    """
                    UPDATE medication_alias_suggestions
                    SET review_status = 'REJECTED',
                        reviewed_by = %s,
                        reviewed_at = NOW()
                    WHERE id = %s AND review_status = 'PENDING'
                    """,
                    (reviewed_by, suggestion_id),
                )
                connection.commit()

                if cursor.rowcount == 0:
                    return {
                        "success": False,
                        "message": f"PENDING 상태의 제안을 찾을 수 없습니다 (id={suggestion_id})",
                    }

                logger.info(
                    "Alias suggestion rejected: id={}, by={}",
                    suggestion_id,
                    reviewed_by,
                )
                return {"success": True, "message": "거부 완료"}

        except Exception as e:
            logger.error(f"reject_suggestion failed: id={suggestion_id}, error={e}")
            return {"success": False, "message": f"거부 처리 실패: {e}"}
        finally:
            if connection:
                connection.close()

    # ──────────────────────────────────────────────
    # 테이블 생성 (fallback)
    # ──────────────────────────────────────────────

    def create_table_if_not_exists(self):
        """medication_alias_suggestions 테이블 생성 (schema.sql 기준)."""
        connection = None
        try:
            connection = self._get_connection()
            with connection.cursor() as cursor:
                cursor.execute("""
                    CREATE TABLE IF NOT EXISTS medication_alias_suggestions (
                        id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
                        item_seq            VARCHAR(20) NOT NULL COMMENT '품목기준코드',
                        alias_name          VARCHAR(200) NOT NULL COMMENT '제안된 별칭',
                        alias_normalized    VARCHAR(200) NOT NULL COMMENT '정규화된 별칭',
                        suggestion_type     VARCHAR(30) NOT NULL DEFAULT 'error_alias'
                                            COMMENT 'alias 또는 error_alias',
                        source              VARCHAR(50) DEFAULT 'user_feedback'
                                            COMMENT 'user_feedback/system/ocr_learning',
                        source_request_id   VARCHAR(100) NULL COMMENT '원본 OCR request_id',
                        review_status       VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                                            COMMENT 'PENDING/APPROVED/REJECTED',
                        frequency           INT NOT NULL DEFAULT 1
                                            COMMENT '동일 제안 횟수 (자동 증가)',
                        is_active           TINYINT(1) NOT NULL DEFAULT 0
                                            COMMENT '관리자 승인 전 기본 0',
                        reviewed_by         VARCHAR(100) NULL COMMENT '검토자',
                        reviewed_at         DATETIME NULL COMMENT '검토 시점',
                        created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
                        updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP
                                            ON UPDATE CURRENT_TIMESTAMP,

                        UNIQUE KEY uk_suggestion_norm (item_seq, alias_normalized),
                        INDEX idx_review_status (review_status),
                        INDEX idx_source_request_id (source_request_id),
                        INDEX idx_frequency (frequency DESC),
                        INDEX idx_suggestion_active (is_active),
                        CONSTRAINT fk_suggestion_item_seq
                            FOREIGN KEY (item_seq) REFERENCES medications_master (item_seq)
                            ON UPDATE CASCADE ON DELETE CASCADE
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                      COMMENT='사용자 확인 기반 alias 개선 후보'
                """)
                connection.commit()
                logger.info("medication_alias_suggestions 테이블 생성/확인 완료")
        except Exception as e:
            logger.error(f"medication_alias_suggestions 테이블 생성 실패: {e}")
            raise
        finally:
            if connection:
                connection.close()
