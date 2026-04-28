"""OCR 결과 저장/조회 MySQL 레포지토리."""
from __future__ import annotations

import json
from datetime import datetime
from typing import List, Optional

import pymysql
from loguru import logger

from app.core.config import configs
from app.ocr.model.ocr_result_model import OcrResultRecord


class OcrResultRepository:
    """medication_ocr_results 테이블 CRUD."""

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

    # ------------------------------------------------------------------
    # CREATE
    # ------------------------------------------------------------------

    def save_result(self, record: OcrResultRecord) -> str:
        """OCR 결과 저장. request_id를 반환한다.

        저장 실패 시 예외를 raise하지 않고 빈 문자열을 반환하며
        로그에 request_id와 오류 내용을 기록한다.
        """
        connection = None
        try:
            connection = self._get_connection()
            with connection.cursor() as cursor:
                sql = """
                    INSERT INTO medication_ocr_results (
                        request_id, elderly_user_id, raw_ocr_text,
                        normalized_names, candidates, pipeline_stages,
                        decision_status, match_confidence, decision_reasons,
                        best_drug_item_seq, best_drug_name,
                        llm_description, warnings, total_duration_ms
                    ) VALUES (
                        %s, %s, %s,
                        %s, %s, %s,
                        %s, %s, %s,
                        %s, %s,
                        %s, %s, %s
                    )
                """
                cursor.execute(sql, (
                    record.request_id,
                    record.elderly_user_id,
                    record.raw_ocr_text,
                    json.dumps(record.normalized_names, ensure_ascii=False),
                    json.dumps(record.candidates, ensure_ascii=False),
                    json.dumps(record.pipeline_stages, ensure_ascii=False),
                    record.decision_status,
                    record.match_confidence,
                    json.dumps(record.decision_reasons, ensure_ascii=False),
                    record.best_drug_item_seq,
                    record.best_drug_name,
                    record.llm_description,
                    json.dumps(record.warnings, ensure_ascii=False),
                    record.total_duration_ms,
                ))
                connection.commit()
                logger.info(
                    "OCR result saved: request_id={}, status={}",
                    record.request_id,
                    record.decision_status,
                )
                return record.request_id
        except Exception as e:
            logger.error(
                "OCR result save failed: request_id={}, error={}",
                record.request_id,
                e,
            )
            return ""
        finally:
            if connection:
                connection.close()

    # ------------------------------------------------------------------
    # READ
    # ------------------------------------------------------------------

    def get_result(self, request_id: str) -> Optional[OcrResultRecord]:
        """request_id로 결과 조회."""
        connection = None
        try:
            connection = self._get_connection()
            with connection.cursor() as cursor:
                cursor.execute(
                    "SELECT * FROM medication_ocr_results WHERE request_id = %s",
                    (request_id,),
                )
                row = cursor.fetchone()
                if not row:
                    return None
                return self._row_to_record(row)
        except Exception as e:
            logger.error("OCR result get failed: request_id={}, error={}", request_id, e)
            return None
        finally:
            if connection:
                connection.close()

    def get_pending_confirmations(
        self,
        elderly_user_id: int,
        limit: int = 20,
    ) -> List[OcrResultRecord]:
        """미확인 건 목록 조회 (user_confirmed IS NULL)."""
        connection = None
        try:
            connection = self._get_connection()
            with connection.cursor() as cursor:
                cursor.execute(
                    """
                    SELECT * FROM medication_ocr_results
                    WHERE elderly_user_id = %s
                      AND user_confirmed IS NULL
                      AND decision_status IN ('NEED_USER_CONFIRMATION', 'AMBIGUOUS')
                    ORDER BY created_at DESC
                    LIMIT %s
                    """,
                    (elderly_user_id, limit),
                )
                rows = cursor.fetchall()
                return [self._row_to_record(r) for r in rows]
        except Exception as e:
            logger.error(
                "get_pending_confirmations failed: elderly_user_id={}, error={}",
                elderly_user_id,
                e,
            )
            return []
        finally:
            if connection:
                connection.close()

    # ------------------------------------------------------------------
    # UPDATE
    # ------------------------------------------------------------------

    def update_user_confirmation(
        self,
        request_id: str,
        selected_item_seq: str,
        confirmed: bool,
    ) -> bool:
        """사용자 확인 결과 업데이트."""
        connection = None
        try:
            connection = self._get_connection()
            with connection.cursor() as cursor:
                affected = cursor.execute(
                    """
                    UPDATE medication_ocr_results
                    SET user_confirmed = %s,
                        user_selected_seq = %s,
                        user_confirmed_at = %s
                    WHERE request_id = %s
                      AND user_confirmed IS NULL
                    """,
                    (
                        1 if confirmed else 0,
                        selected_item_seq,
                        datetime.now(),
                        request_id,
                    ),
                )
                connection.commit()
                if affected == 0:
                    logger.warning(
                        "update_user_confirmation: no rows affected (request_id={}, "
                        "already confirmed?)",
                        request_id,
                    )
                    return False
                logger.info(
                    "User confirmation updated: request_id={}, selected={}, confirmed={}",
                    request_id,
                    selected_item_seq,
                    confirmed,
                )
                return True
        except Exception as e:
            logger.error(
                "update_user_confirmation failed: request_id={}, error={}",
                request_id,
                e,
            )
            return False
        finally:
            if connection:
                connection.close()

    # ------------------------------------------------------------------
    # TABLE
    # ------------------------------------------------------------------

    def create_table_if_not_exists(self):
        """medication_ocr_results 테이블 생성."""
        connection = None
        try:
            connection = self._get_connection()
            with connection.cursor() as cursor:
                cursor.execute("""
                    CREATE TABLE IF NOT EXISTS medication_ocr_results (
                        id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
                        request_id          VARCHAR(36) NOT NULL COMMENT 'UUID',
                        elderly_user_id     BIGINT NULL COMMENT '어르신 사용자 ID',

                        raw_ocr_text        TEXT NOT NULL COMMENT 'Luxia OCR 원문',

                        normalized_names    JSON NULL COMMENT '정규화된 약품명 후보',
                        candidates          JSON NULL COMMENT '매칭 후보 전체',
                        pipeline_stages     JSON NULL COMMENT '파이프라인 단계별 소요시간',

                        decision_status     VARCHAR(30) NOT NULL DEFAULT 'NOT_FOUND',
                        match_confidence    DECIMAL(5,3) NOT NULL DEFAULT 0.000,
                        decision_reasons    JSON NULL COMMENT '판정 사유',
                        best_drug_item_seq  VARCHAR(20) NULL,
                        best_drug_name      VARCHAR(200) NULL,

                        user_confirmed      TINYINT(1) NULL COMMENT 'NULL=미확인, 1=확정, 0=거부',
                        user_selected_seq   VARCHAR(20) NULL,
                        user_confirmed_at   DATETIME NULL,

                        llm_description     TEXT NULL,
                        warnings            JSON NULL,

                        total_duration_ms   DECIMAL(10,2) NULL,
                        created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
                        updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                        UNIQUE KEY uk_request_id (request_id),
                        INDEX idx_elderly_user_id (elderly_user_id),
                        INDEX idx_decision_status (decision_status),
                        INDEX idx_user_confirmed (user_confirmed),
                        INDEX idx_created_at (created_at)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """)
                connection.commit()
                logger.info("medication_ocr_results 테이블 생성/확인 완료")
        except Exception as e:
            logger.error(f"medication_ocr_results 테이블 생성 실패: {e}")
            raise
        finally:
            if connection:
                connection.close()

    # ------------------------------------------------------------------
    # HELPERS
    # ------------------------------------------------------------------

    @staticmethod
    def _row_to_record(row: dict) -> OcrResultRecord:
        """DB row → OcrResultRecord 변환."""

        def _parse_json(value, default=None):
            if value is None:
                return default if default is not None else []
            if isinstance(value, (list, dict)):
                return value
            try:
                return json.loads(value)
            except (json.JSONDecodeError, TypeError):
                return default if default is not None else []

        user_confirmed_raw = row.get("user_confirmed")
        user_confirmed = None
        if user_confirmed_raw is not None:
            user_confirmed = bool(user_confirmed_raw)

        return OcrResultRecord(
            id=row.get("id"),
            request_id=row.get("request_id", ""),
            elderly_user_id=row.get("elderly_user_id"),
            raw_ocr_text=row.get("raw_ocr_text", ""),
            normalized_names=_parse_json(row.get("normalized_names")),
            candidates=_parse_json(row.get("candidates")),
            pipeline_stages=_parse_json(row.get("pipeline_stages")),
            decision_status=row.get("decision_status", "NOT_FOUND"),
            match_confidence=float(row.get("match_confidence") or 0.0),
            decision_reasons=_parse_json(row.get("decision_reasons")),
            best_drug_item_seq=row.get("best_drug_item_seq"),
            best_drug_name=row.get("best_drug_name"),
            user_confirmed=user_confirmed,
            user_selected_seq=row.get("user_selected_seq"),
            user_confirmed_at=row.get("user_confirmed_at"),
            llm_description=row.get("llm_description"),
            warnings=_parse_json(row.get("warnings")),
            total_duration_ms=float(row.get("total_duration_ms") or 0.0),
            created_at=row.get("created_at"),
            updated_at=row.get("updated_at"),
        )
