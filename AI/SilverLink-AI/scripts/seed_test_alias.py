import pymysql
import os
from dotenv import load_dotenv

load_dotenv(".env")

# item_seq of Tylenol 500mg (as an example)
# Looking up an active medication item_seq to use
# The test requirement said: "item_seq는 실제 medications_master에 존재하는 active 약품 사용"
connection = pymysql.connect(
    host=os.getenv("RDS_HOST", "localhost"),
    port=int(os.getenv("RDS_PORT", 3306)),
    user=os.getenv("RDS_USER", "silverlink"),
    password=os.getenv("RDS_PASSWORD", "silverlink123"),
    database=os.getenv("RDS_DATABASE", "silverlink_dev"),
    charset="utf8mb4"
)

try:
    with connection.cursor(pymysql.cursors.DictCursor) as cursor:
        # Get an active Tylenol or similar active medication from DB
        cursor.execute("SELECT item_seq FROM medications_master WHERE is_active = 1 AND item_name LIKE '%타이레놀%' LIMIT 1")
        row = cursor.fetchone()
        
        if not row:
            # Fallback to any active medication
            cursor.execute("SELECT item_seq FROM medications_master WHERE is_active = 1 LIMIT 1")
            row = cursor.fetchone()
            
        item_seq = row["item_seq"]
        alias_name = "테스트_티이레놀_0428"
        
        cursor.execute("""
            INSERT INTO medication_alias_suggestions (
                item_seq, alias_name, alias_normalized,
                suggestion_type, source, source_request_id,
                review_status, frequency, is_active
            ) VALUES (
                %s, %s, %s,
                'error_alias', 'user_feedback', 'test_e2e_request_id',
                'PENDING', 5, 0
            )
            ON DUPLICATE KEY UPDATE 
                review_status = 'PENDING',
                is_active = 0,
                frequency = 5
        """, (item_seq, alias_name, alias_name))
        
        connection.commit()
        print(f"Test fixture seeded: {alias_name} for item_seq {item_seq}")

finally:
    connection.close()
