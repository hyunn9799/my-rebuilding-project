"""
약품 마스터 MySQL 레포지토리 (pymysql raw query)
medications_master 테이블 CRUD + 다중 매칭 검색
"""
import pymysql
from typing import Any, Dict, List, Optional, Tuple
from loguru import logger

from app.core.config import configs
from app.ocr.model.drug_model import DrugInfo


class DrugRepository:
    """약품 마스터 데이터 MySQL 레포지토리"""

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

    def _row_to_drug_info(self, row: dict) -> DrugInfo:
        """DB row를 DrugInfo 모델로 변환"""
        return DrugInfo(
            id=row.get("id"),
            item_seq=row.get("item_seq", ""),
            item_name=row.get("item_name", ""),
            item_name_normalized=row.get("item_name_normalized"),
            entp_name=row.get("entp_name"),
            efcy_qesitm=row.get("efcy_qesitm"),
            use_method_qesitm=row.get("use_method_qesitm"),
            atpn_qesitm=row.get("atpn_qesitm"),
            intrc_qesitm=row.get("intrc_qesitm"),
            se_qesitm=row.get("se_qesitm"),
            deposit_method_qesitm=row.get("deposit_method_qesitm"),
            item_image=row.get("item_image"),
        )

    # ────────────────────────────────────────────
    # 매칭 메서드 (score 0.0 ~ 1.0 반환)
    # ────────────────────────────────────────────

    def exact_match(self, name: str) -> List[Tuple[DrugInfo, float]]:
        """정확 일치 (score = 1.0)"""
        connection = None
        try:
            connection = self._get_connection()
            with connection.cursor() as cursor:
                sql = """
                    SELECT * FROM medications_master
                    WHERE item_name = %s OR item_name_normalized = %s
                    LIMIT 5
                """
                cursor.execute(sql, (name, name))
                rows = cursor.fetchall()
                return [(self._row_to_drug_info(r), 1.0) for r in rows]
        except Exception as e:
            logger.error(f"exact_match 실패: {e}")
            return []
        finally:
            if connection:
                connection.close()

    def alias_match(self, name: str) -> List[Tuple[DrugInfo, float, Dict[str, Any]]]:
        """사용자/운영 alias 정확 일치 검색."""
        connection = None
        try:
            connection = self._get_connection()
            with connection.cursor() as cursor:
                sql = """
                    SELECT m.*, a.alias_name, a.alias_normalized, a.source AS alias_source
                    FROM medication_aliases a
                    JOIN medications_master m ON m.item_seq = a.item_seq
                    WHERE a.is_active = 1
                      AND (a.alias_name = %s OR a.alias_normalized = %s)
                    ORDER BY a.updated_at DESC
                    LIMIT 10
                """
                cursor.execute(sql, (name, name))
                rows = cursor.fetchall()
                return [
                    (
                        self._row_to_drug_info(r),
                        0.98,
                        {
                            "alias_name": r.get("alias_name"),
                            "alias_normalized": r.get("alias_normalized"),
                            "alias_source": r.get("alias_source"),
                        },
                    )
                    for r in rows
                ]
        except Exception as e:
            logger.error(f"alias_match 실패: {e}")
            return []
        finally:
            if connection:
                connection.close()

    def error_alias_match(self, name: str) -> List[Tuple[DrugInfo, float, Dict[str, Any]]]:
        """OCR 오인식 alias 정확 일치 검색."""
        connection = None
        try:
            connection = self._get_connection()
            with connection.cursor() as cursor:
                sql = """
                    SELECT
                        m.*,
                        ea.error_text,
                        ea.normalized_error_text,
                        ea.correction_reason,
                        ea.confidence AS error_alias_confidence,
                        ea.source AS error_alias_source
                    FROM medication_error_aliases ea
                    JOIN medications_master m ON m.item_seq = ea.item_seq
                    WHERE ea.is_active = 1
                      AND (ea.error_text = %s OR ea.normalized_error_text = %s)
                    ORDER BY ea.confidence DESC, ea.updated_at DESC
                    LIMIT 10
                """
                cursor.execute(sql, (name, name))
                rows = cursor.fetchall()

                results = []
                for r in rows:
                    confidence = float(r.get("error_alias_confidence") or 0.9)
                    score = min(max(confidence, 0.5), 1.0) * 0.95
                    results.append((
                        self._row_to_drug_info(r),
                        round(score, 3),
                        {
                            "error_text": r.get("error_text"),
                            "normalized_error_text": r.get("normalized_error_text"),
                            "correction_reason": r.get("correction_reason"),
                            "error_alias_confidence": confidence,
                            "error_alias_source": r.get("error_alias_source"),
                        },
                    ))

                return results
        except Exception as e:
            logger.error(f"error_alias_match 실패: {e}")
            return []
        finally:
            if connection:
                connection.close()

    def prefix_match(self, name: str, min_prefix_len: int = 3) -> List[Tuple[DrugInfo, float]]:
        """
        전방 일치 (score = prefix 길이 비율)
        '타이레놀' → '타이레놀정500밀리그램' 매칭
        """
        if len(name) < min_prefix_len:
            return []

        connection = None
        try:
            connection = self._get_connection()
            with connection.cursor() as cursor:
                sql = """
                    SELECT * FROM medications_master
                    WHERE item_name LIKE %s OR item_name_normalized LIKE %s
                    LIMIT 10
                """
                pattern = f"{name}%"
                cursor.execute(sql, (pattern, pattern))
                rows = cursor.fetchall()

                results = []
                for r in rows:
                    drug = self._row_to_drug_info(r)
                    target = drug.item_name_normalized or drug.item_name
                    # 입력 대비 일치 비율
                    score = len(name) / max(len(target), 1)
                    score = min(score, 0.95)  # exact가 아니므로 0.95 상한
                    results.append((drug, round(score, 3)))

                return sorted(results, key=lambda x: x[1], reverse=True)
        except Exception as e:
            logger.error(f"prefix_match 실패: {e}")
            return []
        finally:
            if connection:
                connection.close()

    def ngram_match(self, name: str, ngram_size: int = 2) -> List[Tuple[DrugInfo, float]]:
        """
        N-gram 토큰 매칭 (FULLTEXT INDEX 활용)
        '타이놀정' → 2-gram ['타이', '이놀', '놀정'] → MATCH AGAINST
        """
        if len(name) < ngram_size:
            return []

        connection = None
        try:
            connection = self._get_connection()
            with connection.cursor() as cursor:
                # MySQL ngram FULLTEXT 검색
                sql = """
                    SELECT *, MATCH(item_name) AGAINST(%s IN BOOLEAN MODE) AS relevance
                    FROM medications_master
                    WHERE MATCH(item_name) AGAINST(%s IN BOOLEAN MODE)
                    ORDER BY relevance DESC
                    LIMIT 10
                """
                cursor.execute(sql, (name, name))
                rows = cursor.fetchall()

                if not rows:
                    return []

                # relevance 점수를 0~1로 정규화
                max_relevance = max(r["relevance"] for r in rows) if rows else 1.0
                results = []
                for r in rows:
                    drug = self._row_to_drug_info(r)
                    score = (r["relevance"] / max_relevance) * 0.85  # ngram 상한 0.85
                    results.append((drug, round(score, 3)))

                return results
        except Exception as e:
            logger.error(f"ngram_match 실패: {e}")
            return []
        finally:
            if connection:
                connection.close()

    def fetch_all_for_fuzzy(self, limit: int = 5000) -> List[DrugInfo]:
        """[DEPRECATED] fuzzy 매칭용 전체 약품명 목록 조회.

        경고: 이 메서드는 매 호출마다 최대 limit건을 DB에서 가져옵니다.
        LocalDrugIndex가 활성화된 환경에서는 호출되지 않아야 합니다.
        mysql_matcher의 _try_fuzzy는 이제 ngram_match 기반이므로
        이 메서드는 향후 제거 예정입니다.
        """
        import warnings
        warnings.warn(
            "fetch_all_for_fuzzy is deprecated; use ngram_match + fuzzy scoring instead",
            DeprecationWarning,
            stacklevel=2,
        )
        connection = None
        try:
            connection = self._get_connection()
            with connection.cursor() as cursor:
                sql = "SELECT * FROM medications_master LIMIT %s"
                cursor.execute(sql, (limit,))
                rows = cursor.fetchall()
                return [self._row_to_drug_info(r) for r in rows]
        except Exception as e:
            logger.error(f"fetch_all_for_fuzzy 실패: {e}")
            return []
        finally:
            if connection:
                connection.close()

    # ────────────────────────────────────────────
    # CRUD (적재 스크립트용)
    # ────────────────────────────────────────────

    def fetch_all_medications_for_index(self) -> List[DrugInfo]:
        """Local memory index 초기화를 위한 전체 약품 조회."""
        connection = None
        try:
            connection = self._get_connection()
            with connection.cursor() as cursor:
                cursor.execute("SELECT * FROM medications_master")
                rows = cursor.fetchall()
                return [self._row_to_drug_info(r) for r in rows]
        except Exception as e:
            logger.error(f"fetch_all_medications_for_index 실패: {e}")
            return []
        finally:
            if connection:
                connection.close()

    def fetch_all_aliases_for_index(self) -> List[Dict[str, Any]]:
        """Local memory index 초기화를 위한 alias 전체 조회."""
        connection = None
        try:
            connection = self._get_connection()
            with connection.cursor() as cursor:
                cursor.execute("""
                    SELECT item_seq, alias_name, alias_normalized, source AS alias_source
                    FROM medication_aliases
                    WHERE is_active = 1
                """)
                return list(cursor.fetchall())
        except Exception as e:
            logger.error(f"fetch_all_aliases_for_index 실패: {e}")
            return []
        finally:
            if connection:
                connection.close()

    def fetch_all_error_aliases_for_index(self) -> List[Dict[str, Any]]:
        """Local memory index 초기화를 위한 OCR error alias 전체 조회."""
        connection = None
        try:
            connection = self._get_connection()
            with connection.cursor() as cursor:
                cursor.execute("""
                    SELECT
                        item_seq,
                        error_text,
                        normalized_error_text,
                        correction_reason,
                        confidence,
                        source AS error_alias_source
                    FROM medication_error_aliases
                    WHERE is_active = 1
                """)
                return list(cursor.fetchall())
        except Exception as e:
            logger.error(f"fetch_all_error_aliases_for_index 실패: {e}")
            return []
        finally:
            if connection:
                connection.close()

    def upsert_drug(self, drug: DrugInfo) -> bool:
        """약품 정보 upsert (INSERT ON DUPLICATE KEY UPDATE)"""
        connection = None
        try:
            connection = self._get_connection()
            with connection.cursor() as cursor:
                sql = """
                    INSERT INTO medications_master
                    (item_seq, item_name, item_name_normalized, entp_name,
                     efcy_qesitm, use_method_qesitm, atpn_qesitm,
                     intrc_qesitm, se_qesitm, deposit_method_qesitm, item_image)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                    ON DUPLICATE KEY UPDATE
                        item_name = VALUES(item_name),
                        item_name_normalized = VALUES(item_name_normalized),
                        entp_name = VALUES(entp_name),
                        efcy_qesitm = VALUES(efcy_qesitm),
                        use_method_qesitm = VALUES(use_method_qesitm),
                        atpn_qesitm = VALUES(atpn_qesitm),
                        intrc_qesitm = VALUES(intrc_qesitm),
                        se_qesitm = VALUES(se_qesitm),
                        deposit_method_qesitm = VALUES(deposit_method_qesitm),
                        item_image = VALUES(item_image)
                """
                cursor.execute(sql, (
                    drug.item_seq, drug.item_name, drug.item_name_normalized,
                    drug.entp_name, drug.efcy_qesitm, drug.use_method_qesitm,
                    drug.atpn_qesitm, drug.intrc_qesitm, drug.se_qesitm,
                    drug.deposit_method_qesitm, drug.item_image,
                ))
                connection.commit()
                return True
        except Exception as e:
            logger.error(f"upsert_drug 실패: {e}")
            return False
        finally:
            if connection:
                connection.close()

    def bulk_upsert(self, drugs: List[DrugInfo]) -> int:
        """대량 upsert"""
        connection = None
        count = 0
        try:
            connection = self._get_connection()
            with connection.cursor() as cursor:
                sql = """
                    INSERT INTO medications_master
                    (item_seq, item_name, item_name_normalized, entp_name,
                     efcy_qesitm, use_method_qesitm, atpn_qesitm,
                     intrc_qesitm, se_qesitm, deposit_method_qesitm, item_image)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                    ON DUPLICATE KEY UPDATE
                        item_name = VALUES(item_name),
                        item_name_normalized = VALUES(item_name_normalized),
                        entp_name = VALUES(entp_name),
                        efcy_qesitm = VALUES(efcy_qesitm),
                        use_method_qesitm = VALUES(use_method_qesitm),
                        atpn_qesitm = VALUES(atpn_qesitm),
                        intrc_qesitm = VALUES(intrc_qesitm),
                        se_qesitm = VALUES(se_qesitm),
                        deposit_method_qesitm = VALUES(deposit_method_qesitm),
                        item_image = VALUES(item_image)
                """
                for drug in drugs:
                    try:
                        cursor.execute(sql, (
                            drug.item_seq, drug.item_name, drug.item_name_normalized,
                            drug.entp_name, drug.efcy_qesitm, drug.use_method_qesitm,
                            drug.atpn_qesitm, drug.intrc_qesitm, drug.se_qesitm,
                            drug.deposit_method_qesitm, drug.item_image,
                        ))
                        count += 1
                    except Exception as e:
                        logger.warning(f"개별 upsert 실패 (item_seq={drug.item_seq}): {e}")

                connection.commit()
                logger.info(f"bulk_upsert 완료: {count}/{len(drugs)}건")

            return count
        except Exception as e:
            logger.error(f"bulk_upsert 실패: {e}")
            return count
        finally:
            if connection:
                connection.close()

    def create_table_if_not_exists(self):
        """medications_master 테이블 생성 (없을 경우)"""
        connection = None
        try:
            connection = self._get_connection()
            with connection.cursor() as cursor:
                sql = """
                    CREATE TABLE IF NOT EXISTS medications_master (
                        id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
                        item_seq              VARCHAR(20) UNIQUE NOT NULL COMMENT '품목기준코드',
                        item_name             VARCHAR(200) NOT NULL COMMENT '약품명',
                        item_name_normalized  VARCHAR(200) COMMENT '정규화된 약품명',
                        entp_name             VARCHAR(200) COMMENT '업체명',
                        efcy_qesitm           TEXT COMMENT '효능효과',
                        use_method_qesitm     TEXT COMMENT '사용법',
                        atpn_qesitm           TEXT COMMENT '주의사항',
                        intrc_qesitm          TEXT COMMENT '상호작용',
                        se_qesitm             TEXT COMMENT '부작용',
                        deposit_method_qesitm TEXT COMMENT '보관법',
                        item_image            VARCHAR(500) COMMENT '이미지URL',
                        created_at            DATETIME DEFAULT CURRENT_TIMESTAMP,
                        updated_at            DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        INDEX idx_item_name (item_name),
                        INDEX idx_item_name_normalized (item_name_normalized),
                        FULLTEXT INDEX ft_item_name (item_name) WITH PARSER ngram
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """
                cursor.execute(sql)

                cursor.execute("""
                    CREATE TABLE IF NOT EXISTS medication_aliases (
                        id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
                        item_seq           VARCHAR(20) NOT NULL COMMENT '품목기준코드',
                        alias_name         VARCHAR(200) NOT NULL COMMENT '약품 별칭',
                        alias_normalized   VARCHAR(200) COMMENT '정규화된 약품 별칭',
                        source             VARCHAR(50) DEFAULT 'manual' COMMENT 'manual/user/system',
                        is_active          TINYINT(1) NOT NULL DEFAULT 1,
                        created_at         DATETIME DEFAULT CURRENT_TIMESTAMP,
                        updated_at         DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        UNIQUE KEY uk_medication_alias (item_seq, alias_name),
                        INDEX idx_alias_name (alias_name),
                        INDEX idx_alias_normalized (alias_normalized),
                        INDEX idx_alias_item_seq (item_seq),
                        CONSTRAINT fk_medication_alias_item_seq
                            FOREIGN KEY (item_seq) REFERENCES medications_master(item_seq)
                            ON DELETE CASCADE
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """)

                cursor.execute("""
                    CREATE TABLE IF NOT EXISTS medication_error_aliases (
                        id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
                        item_seq              VARCHAR(20) NOT NULL COMMENT '품목기준코드',
                        error_text            VARCHAR(200) NOT NULL COMMENT 'OCR 오인식 문자열',
                        normalized_error_text VARCHAR(200) COMMENT '정규화된 OCR 오인식 문자열',
                        correction_reason     VARCHAR(200) COMMENT '보정 사유',
                        confidence            DECIMAL(4,3) NOT NULL DEFAULT 0.900,
                        source                VARCHAR(50) DEFAULT 'manual' COMMENT 'manual/user/system',
                        is_active             TINYINT(1) NOT NULL DEFAULT 1,
                        created_at            DATETIME DEFAULT CURRENT_TIMESTAMP,
                        updated_at            DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        UNIQUE KEY uk_medication_error_alias (item_seq, error_text),
                        INDEX idx_error_text (error_text),
                        INDEX idx_normalized_error_text (normalized_error_text),
                        INDEX idx_error_alias_item_seq (item_seq),
                        CONSTRAINT fk_medication_error_alias_item_seq
                            FOREIGN KEY (item_seq) REFERENCES medications_master(item_seq)
                            ON DELETE CASCADE
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """)

                connection.commit()
                logger.info("medications_master 및 alias 테이블 생성/확인 완료")
        except Exception as e:
            logger.error(f"테이블 생성 실패: {e}")
            raise
        finally:
            if connection:
                connection.close()
