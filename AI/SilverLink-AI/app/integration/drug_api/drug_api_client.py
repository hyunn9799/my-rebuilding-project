"""
공공데이터포털 e약은요 API 클라이언트
https://www.data.go.kr/data/15075057/openapi.do
"""
import httpx
from typing import Optional, List, Dict, Any
from loguru import logger

from app.core.config import configs


class DrugApiClient:
    """식품의약품안전처 의약품개요정보(e약은요) API"""

    BASE_URL = "http://apis.data.go.kr/1471000/DrbEasyDrugInfoService/getDrbEasyDrugList"

    def __init__(self, service_key: Optional[str] = None):
        self.service_key = service_key or configs.DRUG_API_SERVICE_KEY
        if not self.service_key:
            raise ValueError("DRUG_API_SERVICE_KEY 환경변수가 설정되지 않았습니다.")

    async def search_by_name(self, item_name: str, page_no: int = 1, num_of_rows: int = 10) -> Dict[str, Any]:
        """약품명으로 조회"""
        params = {
            "serviceKey": self.service_key,
            "itemName": item_name,
            "pageNo": str(page_no),
            "numOfRows": str(num_of_rows),
            "type": "json",
        }
        return await self._request(params)

    async def search_by_seq(self, item_seq: str) -> Dict[str, Any]:
        """품목기준코드로 조회"""
        params = {
            "serviceKey": self.service_key,
            "itemSeq": item_seq,
            "type": "json",
        }
        return await self._request(params)

    async def search_by_entp(self, entp_name: str, page_no: int = 1, num_of_rows: int = 10) -> Dict[str, Any]:
        """업체명으로 조회"""
        params = {
            "serviceKey": self.service_key,
            "entpName": entp_name,
            "pageNo": str(page_no),
            "numOfRows": str(num_of_rows),
            "type": "json",
        }
        return await self._request(params)

    async def fetch_all_pages(
        self,
        item_name: Optional[str] = None,
        num_of_rows: int = 100,
        max_pages: Optional[int] = None,
    ) -> List[Dict[str, Any]]:
        """전체 페이지 순회하여 모든 약품 데이터 수집"""
        all_items = []
        page_no = 1

        while True:
            params = {
                "serviceKey": self.service_key,
                "pageNo": str(page_no),
                "numOfRows": str(num_of_rows),
                "type": "json",
            }
            if item_name:
                params["itemName"] = item_name

            data = await self._request(params)
            items = self._extract_items(data)

            if not items:
                logger.info(f"e약은요 API: 페이지 {page_no}에서 데이터 없음, 수집 종료")
                break

            all_items.extend(items)
            logger.info(f"e약은요 API: 페이지 {page_no} - {len(items)}건 수집 (누적 {len(all_items)}건)")

            # 총 건수 확인
            total_count = self._extract_total_count(data)
            if total_count and len(all_items) >= total_count:
                break

            if max_pages and page_no >= max_pages:
                logger.info(f"최대 페이지 수({max_pages}) 도달, 수집 종료")
                break

            page_no += 1

        return all_items

    async def _request(self, params: Dict[str, str]) -> Dict[str, Any]:
        """HTTP 요청 수행"""
        try:
            async with httpx.AsyncClient(timeout=30.0) as client:
                response = await client.get(self.BASE_URL, params=params)
                response.raise_for_status()

                # XML 에러 응답 체크 (API 키 오류 등)
                content_type = response.headers.get("content-type", "")
                if "xml" in content_type:
                    logger.error(f"e약은요 API XML 응답 (인증키 오류 가능): {response.text[:500]}")
                    return {}

                return response.json()
        except httpx.HTTPStatusError as e:
            logger.error(f"e약은요 API HTTP 에러: {e.response.status_code} - {e}")
            return {}
        except Exception as e:
            logger.error(f"e약은요 API 요청 실패: {e}")
            return {}

    def _extract_items(self, data: Dict[str, Any]) -> List[Dict[str, Any]]:
        """API 응답에서 약품 아이템 목록 추출"""
        try:
            body = data.get("body", {})
            items = body.get("items", [])
            if isinstance(items, list):
                return items
            # 단건 결과일 경우
            if isinstance(items, dict):
                return [items]
            return []
        except (KeyError, TypeError):
            return []

    def _extract_total_count(self, data: Dict[str, Any]) -> Optional[int]:
        """API 응답에서 총 건수 추출"""
        try:
            return int(data.get("body", {}).get("totalCount", 0))
        except (KeyError, TypeError, ValueError):
            return None
