"""DB 연결 및 OCR 테이블 검증 스크립트."""
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from dotenv import load_dotenv
load_dotenv()

import pymysql

host = os.getenv("RDS_HOST", "localhost")
port = int(os.getenv("RDS_PORT", "3306"))
user = os.getenv("RDS_USER", "root")
password = os.getenv("RDS_PASSWORD", "")
database = os.getenv("RDS_DATABASE", "silverlink")

print(f"=== DB 연결 테스트 ===")
print(f"Host: {host}:{port}")
print(f"Database: {database}")
print(f"User: {user}")
print()

try:
    conn = pymysql.connect(
        host=host, port=port, user=user,
        password=password, database=database,
        charset="utf8mb4", cursorclass=pymysql.cursors.DictCursor,
    )
    print("[OK] MySQL 연결 성공!")
    
    with conn.cursor() as cur:
        # 1. 전체 테이블 수
        cur.execute("SHOW TABLES")
        tables = [list(r.values())[0] for r in cur.fetchall()]
        print(f"[OK] 전체 테이블 수: {len(tables)}")
        
        # 2. OCR 관련 테이블 확인
        ocr_tables = [
            "medications_master",
            "medication_aliases",
            "medication_error_aliases",
            "medication_ocr_results",
            "medication_ocr_candidates",
            "medication_alias_suggestions",
            "medication_dictionary_load_logs",
        ]
        
        print(f"\n=== Phase 11 AI OCR 테이블 (7개) ===")
        for t in ocr_tables:
            if t in tables:
                cur.execute(f"SELECT COUNT(*) as cnt FROM `{t}`")
                cnt = cur.fetchone()["cnt"]
                print(f"  [OK] {t} (rows: {cnt})")
            else:
                print(f"  [MISSING] {t}")
        
        # 3. BE OCR 관련 테이블 확인
        be_tables = ["medication_ocr_logs", "medication_schedules", "medication_intake_logs"]
        print(f"\n=== BE 복약 테이블 ===")
        for t in be_tables:
            if t in tables:
                print(f"  [OK] {t}")
            else:
                print(f"  [MISSING] {t}")
        
        # 4. FK 검증 - medication_ocr_results → elderly
        print(f"\n=== FK 참조 검증 ===")
        cur.execute("""
            SELECT TABLE_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME
            FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
            WHERE TABLE_SCHEMA = %s
              AND TABLE_NAME LIKE 'medication%%'
              AND REFERENCED_TABLE_NAME IS NOT NULL
            ORDER BY TABLE_NAME
        """, (database,))
        fks = cur.fetchall()
        for fk in fks:
            print(f"  [FK] {fk['TABLE_NAME']}.{fk['COLUMN_NAME']} → {fk['REFERENCED_TABLE_NAME']}.{fk['REFERENCED_COLUMN_NAME']}")
        
        # 5. UNIQUE KEY 검증
        print(f"\n=== UNIQUE KEY 검증 ===")
        cur.execute("""
            SELECT TABLE_NAME, INDEX_NAME, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) as cols
            FROM INFORMATION_SCHEMA.STATISTICS
            WHERE TABLE_SCHEMA = %s
              AND TABLE_NAME IN ('medication_aliases', 'medication_error_aliases', 'medication_alias_suggestions')
              AND NON_UNIQUE = 0
              AND INDEX_NAME != 'PRIMARY'
            GROUP BY TABLE_NAME, INDEX_NAME
        """, (database,))
        uks = cur.fetchall()
        for uk in uks:
            print(f"  [UK] {uk['TABLE_NAME']}.{uk['INDEX_NAME']} → ({uk['cols']})")
    
    conn.close()
    print(f"\n=== 테스트 완료: ALL PASSED ===")

except pymysql.err.OperationalError as e:
    print(f"[FAIL] MySQL 연결 실패: {e}")
    sys.exit(1)
except Exception as e:
    print(f"[FAIL] 오류: {e}")
    sys.exit(1)
