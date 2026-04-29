"""
공공데이터포털 의약품 허가정보 API 클라이언트
DrugPrdtPrmsnInfoService07 / getDrugPrdtPrmsnInq07
"""
import httpx
from typing import Optional, List, Dict, Any
from loguru import logger

from app.core.config import configs

def get_field(item: dict, *keys, default=None):
    for key in keys:
        value = item.get(key)
        if value not in (None, ""):
            return value
    return default


class DrugApiClient:
    """식품의약품안전처 의약품 제품 허가 정보 API (DrugPrdtPrmsnInfoService07)"""

    def __init__(self, service_key: Optional[str] = None, endpoint: Optional[str] = None):
        self.service_key = service_key or configs.DRUG_API_SERVICE_KEY
        # endpoint 끝에 / 가 없이 operation이 시작되도록 처리
        base_ep = endpoint or configs.DRUG_API_ENDPOINT
        self.endpoint = base_ep.rstrip('/')
        operation = configs.DRUG_API_OPERATION
        if not operation.startswith('/'):
            operation = f"/{operation}"
        self.BASE_URL = f"{self.endpoint}{operation}"

        if not self.service_key:
            raise ValueError("DRUG_API_SERVICE_KEY 환경변수가 설정되지 않았습니다.")

    async def search_by_name(self, item_name: str, page_no: int = 1, num_of_rows: int = 10) -> Dict[str, Any]:
        """약품명으로 조회"""
        params = {
            "pageNo": str(page_no),
            "numOfRows": str(num_of_rows),
            "type": configs.DRUG_API_RESPONSE_TYPE,
        }
        if item_name:
            params["item_name"] = item_name
        return await self._request(params)

    async def search_by_seq(self, item_seq: str) -> Dict[str, Any]:
        """품목기준코드로 조회"""
        params = {
            "item_seq": item_seq,
            "type": configs.DRUG_API_RESPONSE_TYPE,
        }
        return await self._request(params)

    async def search_by_entp(self, entp_name: str, page_no: int = 1, num_of_rows: int = 10) -> Dict[str, Any]:
        """업체명으로 조회"""
        params = {
            "entp_name": entp_name,
            "pageNo": str(page_no),
            "numOfRows": str(num_of_rows),
            "type": configs.DRUG_API_RESPONSE_TYPE,
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
                "pageNo": str(page_no),
                "numOfRows": str(num_of_rows),
                "type": configs.DRUG_API_RESPONSE_TYPE,
            }
            if item_name:
                params["item_name"] = item_name

            data = await self._request(params)
            items = self._extract_items(data)

            if not items:
                logger.info(f"허가정보 API: 페이지 {page_no}에서 데이터 없음, 수집 종료")
                break

            all_items.extend(items)
            logger.info(f"허가정보 API: 페이지 {page_no} - {len(items)}건 수집 (누적 {len(all_items)}건)")

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
        # serviceKey URL 인코딩 문제 방어: httpx가 이중 인코딩하지 않게 쿼리 문자열 직접 구성 시 유의하거나
        # params dict 처리가 안전한지 확인. 기본적으로 params 전달은 안전함.
        # 공공 API에서 이중 인코딩 막기 위한 편법: 문자열 쿼리로 직접 결합 (선택 사항)
        # 하지만 최신 환경에서 service_key가 unquote 상태라면 param으로 맡기는게 안전함.
        
        # httpx Custom Query Params handling for serviceKey
        # Some go.kr APIs get broken with double encoding, passing as str helps:
        # However, to be clean, let's keep it in params dict, but pass serviceKey carefully.
        query_params = params.copy()
        
        try:
            async with httpx.AsyncClient(timeout=30.0) as client:
                query_string = "&".join(f"{k}={v}" for k, v in query_params.items())
                # serviceKey를 별도로 붙여 이중인코딩 회피 (공공데이터포털 종특)
                if self.service_key:
                    final_url = f"{self.BASE_URL}?serviceKey={self.service_key}&{query_string}"
                else:
                    final_url = f"{self.BASE_URL}?{query_string}"
                    
                response = await client.get(final_url)
                response.raise_for_status()

                # XML 에러 응답 체크 (API 키 오류 등)
                content_type = response.headers.get("content-type", "")
                if "xml" in content_type:
                    logger.error(f"허가정보 API XML 응답 (인증키 오류 가능): {response.text[:500]}")
                    return {}

                return response.json()
        except httpx.HTTPStatusError as e:
            logger.error(f"허가정보 API HTTP 에러: {e.response.status_code} - {e}")
            return {}
        except Exception as e:
            logger.error(f"허가정보 API 요청 실패: {e}")
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
