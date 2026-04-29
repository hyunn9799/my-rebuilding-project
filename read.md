# Codex Harness 구성 요청서

이 레포의 Codex CLI 토큰 사용량을 줄이기 위한 하네스 구조를 구성해줘.

## 목표

전체 코드를 무작정 읽지 않고, 필요한 파일만 좁혀서 작업할 수 있도록 다음 파일들을 생성한다.

- `AGENTS.md`
- `docs/architecture-map.md`
- `.agents/skills/ocr-drug-matching/SKILL.md`
- `.agents/skills/spring-api-debug/SKILL.md`
- `.agents/skills/react-admin-ui/SKILL.md`
- `.agents/skills/aws-ecs-architecture/SKILL.md`

---

## 작업 조건

1. `AGENTS.md`는 100줄 이하로 짧게 작성한다.
2. 전체 코드를 다 읽지 않는다.
3. 구조 파악은 다음 명령 중심으로만 수행한다.
   - `tree`
   - `find`
   - `rg`
   - `ls`
   - package/config 파일 확인
4. 다음 디렉터리와 파일은 사용자가 명시하지 않으면 읽지 않는다.
   - `node_modules`
   - `dist`
   - `build`
   - `.env`
   - `.venv`
   - `venv`
   - `target`
   - `.git`
   - `__pycache__`
   - `.pytest_cache`
   - `.mypy_cache`
5. 긴 설명은 `AGENTS.md`에 넣지 않고 `docs/architecture-map.md` 또는 `SKILL.md`로 분리한다.
6. 각 `SKILL.md`는 `description`을 명확하게 작성해서 필요한 경우에만 읽히게 구성한다.
7. 변경 후 반드시 `git diff`로 변경 내용을 요약한다.

---

## 생성할 파일 구조

```txt
.
├── AGENTS.md
├── docs/
│   └── architecture-map.md
└── .agents/
    └── skills/
        ├── ocr-drug-matching/
        │   └── SKILL.md
        ├── spring-api-debug/
        │   └── SKILL.md
        ├── react-admin-ui/
        │   └── SKILL.md
        └── aws-ecs-architecture/
            └── SKILL.md