"""사용자 피드백 기반 alias 제안 저장/관리 레포지토리."""
from __future__ import annotations

from datetime import datetime
from typing import List, Optional

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

    def save_suggestion(
        self,
        item_seq: str,
        alias_name: str,
        alias_normalized: str,
        source_request_id: str,
        source: str = "user_feedback",
    ) -> bool:
        """alias 제안을 PENDING 상태로 저장.

        이미 동일한 (item_seq, alias_name) 제안이 있으면 frequency를 증가시킨다.
        """
        connection = None
        try:
            connection = self._get_connection()
            with connection.cursor() as cursor:
                # UPSERT: 중복 시 frequency 증가
                sql = """
                    INSERT INTO medication_alias_suggestions (
                        item_seq, alias_name, alias_normalized,
                        source, source_request_id,
                        review_status, frequency
                    ) VALUES (
                        %s, %s, %s, %s, %s, 'PENDING', 1
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
                    source,
                    source_request_id,
                ))
                connection.commit()
                logger.info(
                    "Alias suggestion saved: item_seq={}, alias='{}', request_id={}",
                    item_seq,
                    alias_name,
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

    def create_table_if_not_exists(self):
        """medication_alias_suggestions 테이블 생성."""
        connection = None
        try:
            connection = self._get_connection()
            with connection.cursor() as cursor:
                cursor.execute("""
                    CREATE TABLE IF NOT EXISTS medication_alias_suggestions (
                        id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
                        item_seq            VARCHAR(20) NOT NULL COMMENT '품목기준코드',
                        alias_name          VARCHAR(200) NOT NULL COMMENT '제안된 별칭',
                        alias_normalized    VARCHAR(200) NULL COMMENT '정규화된 별칭',
                        source              VARCHAR(50) DEFAULT 'user_feedback'
                                            COMMENT 'user_feedback/system/ocr_learning',
                        source_request_id   VARCHAR(36) NULL COMMENT '원본 OCR request_id',
                        review_status       VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                                            COMMENT 'PENDING/APPROVED/REJECTED',
                        frequency           INT NOT NULL DEFAULT 1
                                            COMMENT '동일 제안 횟수 (자동 증가)',
                        reviewed_by         VARCHAR(100) NULL COMMENT '검토자',
                        reviewed_at         DATETIME NULL COMMENT '검토 시점',
                        created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
                        updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP
                                            ON UPDATE CURRENT_TIMESTAMP,

                        UNIQUE KEY uk_suggestion (item_seq, alias_name),
                        INDEX idx_review_status (review_status),
                        INDEX idx_source_request_id (source_request_id),
                        INDEX idx_frequency (frequency DESC)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """)
                connection.commit()
                logger.info("medication_alias_suggestions 테이블 생성/확인 완료")
        except Exception as e:
            logger.error(f"medication_alias_suggestions 테이블 생성 실패: {e}")
            raise
        finally:
            if connection:
                connection.close()
