import pymysql
import os
import argparse
from dotenv import load_dotenv

load_dotenv(".env")

parser = argparse.ArgumentParser(description="Seed a PENDING alias suggestion for admin E2E testing.")
parser.add_argument("--alias-name", default="테스트_티이레놀_e2e", help="Alias text to insert")
parser.add_argument("--suggestion-type", default="error_alias", choices=["alias", "error_alias"])
parser.add_argument("--source-request-id", default="test_e2e_request_id")
parser.add_argument("--item-name-like", default="타이레놀", help="Preferred active medication name pattern")
args = parser.parse_args()

connection = pymysql.connect(
    host=os.getenv("RDS_HOST", "localhost"),
    port=int(os.getenv("RDS_PORT", 3306)),
    user=os.getenv("RDS_USER", "silverlink"),
    password=os.getenv("RDS_PASSWORD", "silverlink123"),
    database=os.getenv("RDS_DATABASE", "silverlink"),
    charset="utf8mb4"
)

try:
    with connection.cursor(pymysql.cursors.DictCursor) as cursor:
        cursor.execute(
            """
            SELECT item_seq, item_name
            FROM medications_master
            WHERE is_active = 1 AND item_name LIKE %s
            LIMIT 1
            """,
            (f"%{args.item_name_like}%",),
        )
        row = cursor.fetchone()

        if not row:
            cursor.execute(
                """
                SELECT item_seq, item_name
                FROM medications_master
                WHERE is_active = 1
                LIMIT 1
                """
            )
            row = cursor.fetchone()

        if not row:
            raise RuntimeError("No active medication found in medications_master.")

        item_seq = row["item_seq"]
        item_name = row["item_name"]

        cursor.execute("""
            INSERT INTO medication_alias_suggestions (
                item_seq, alias_name, alias_normalized,
                suggestion_type, source, source_request_id,
                review_status, frequency, is_active
            ) VALUES (
                %s, %s, %s,
                %s, 'user_feedback', %s,
                'PENDING', 5, 0
            )
            ON DUPLICATE KEY UPDATE
                review_status = 'PENDING',
                is_active = 0,
                frequency = 5,
                reviewed_by = NULL,
                reviewed_at = NULL
        """, (
            item_seq,
            args.alias_name,
            args.alias_name,
            args.suggestion_type,
            args.source_request_id,
        ))

        connection.commit()
        print("Admin alias E2E fixture seeded")
        print(f"  alias_name={args.alias_name}")
        print(f"  suggestion_type={args.suggestion_type}")
        print(f"  item_seq={item_seq}")
        print(f"  item_name={item_name}")

finally:
    connection.close()
