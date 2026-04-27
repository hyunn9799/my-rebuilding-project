"""
SQS Worker Entry Point

워커 프로세스를 독립 실행하기 위한 메인 스크립트.
FastAPI 서버와 별도로 실행됩니다.

Usage:
    python worker_main.py
    
Environment Variables:
    AWS_REGION: AWS 리전 (default: ap-northeast-2)
    AWS_ACCESS_KEY_ID: AWS Access Key
    AWS_SECRET_ACCESS_KEY: AWS Secret Key
    SQS_QUEUE_URL: 메인 SQS 큐 URL
    SQS_DLQ_URL: Dead Letter Queue URL
"""
import asyncio
import logging
import sys
from dotenv import load_dotenv

# 환경 변수 로드
load_dotenv()

# 로깅 설정
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(sys.stdout),
        logging.FileHandler('logs/worker.log', encoding='utf-8')
    ]
)
logger = logging.getLogger(__name__)


def create_worker():
    """DI 컨테이너를 통해 워커 인스턴스 생성"""
    from app.core.container import Container
    
    container = Container()
    worker = container.sqs_worker()
    
    return worker


async def main():
    """워커 메인 함수"""
    logger.info("=" * 50)
    logger.info("🚀 SilverLink SQS Worker Starting...")
    logger.info("=" * 50)
    
    # 로그 디렉토리 생성
    import os
    os.makedirs("logs", exist_ok=True)
    
    try:
        worker = create_worker()
        await worker.start(poll_interval=1)
    except KeyboardInterrupt:
        logger.info("Worker interrupted by user")
    except Exception as e:
        logger.error(f"Worker failed: {e}")
        raise
    finally:
        logger.info("Worker shutdown complete")


if __name__ == "__main__":
    asyncio.run(main())
