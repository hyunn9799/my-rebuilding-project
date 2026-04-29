# Medication Data Pipeline (Phase 10)

본 문서는 의약품 식별을 위한 OCR 파이프라인의 핵심 데이터 적재 정책 및 가이드를 설명합니다.

## 1. 개요
* **Source of Truth**: 식품의약품안전처 - 의약품 제품 허가정보 API (DrugPrdtPrmsnInfoService07)
* **주요 변경점(Phase 10)**: 
  - 과거 'e약은요' API에서 허가정보 API로 전면 개편.
  - LLM 환각(Hallucination) 방지를 위해 효능 데이터 누락 시 임베딩/설명 스크립트에 안전 구문(Fallback) 추가.
  - 대량 데이터에 대한 단계적/안전한 Upsert 및 Vector Index 재구축 체계 마련.

## 2. 컴포넌트별 주요 역할
* `scripts/load_drug_data.py`
  - MySQL (`medications_master`) 전체 동기화 담당. (`--batch-size`, `--max-pages` 등 지원)
  - `is_active` 제어: `CANCEL_DATE` 항목 존재 시 비활성(0) 처리.
  - `--chromadb-only` 인자를 줘서 ChromaDB 벡터 추출 및 백그라운드 적재 분리 실행 가능.
* `scripts/seed_aliases.py`
  - 기존 약제 이름(줄임말, 기호 오류 등)에 대한 휴리스틱 알리어스 생성.
  - 덮어쓰기 방지 및 실수 예방을 위해 `--force` 옵션 필수 사용.

## 3. 실행 방법
전체 파이프라인 적재는 다음 일괄 실행 스크립트로 진행할 수 있습니다.

```bash
> .\run_phase10_pipeline.bat
```

해당 스크립트는 다음 세 단계를 순차적으로 진행합니다:
1. `load_drug_data.py`: MySQL의 `medications_master` 원본 병합 갱신.
2. `seed_aliases.py --force`: 식별된 오인식 단어 기초 시드 세팅.
3. `load_drug_data.py --chromadb-only`: 갱신 완료된 활성 리스트에 대해 OpenAI Embedding 후 ChromaDB Bulk Upsert 진행.

## 4. 백엔드 연동
적재가 끝난 뒤 서버를 구동(또는 관리자 엔드포인트 `/admin/reload-dictionary` 호출)하면 새로 구축된 MySQL/ChromaDB 최신 상태가 즉각 메모리에 로딩(Atomic Swap)되어 서비스 중단 없이 즉시 인식 파이프라인이 갱신됩니다.
