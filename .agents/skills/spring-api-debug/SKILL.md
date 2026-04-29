---
name: spring-api-debug
description: Use for Spring Boot API debugging, controller/service/repository changes, auth/session/Redis behavior, JPA tests, and backend integration issues.
---

# Spring API Debug Skill

## Start Here

- Read `docs/architecture-map.md`.
- Locate the API owner with `rg -n "@RequestMapping"` before opening files.

## Primary Files

- App entry: `BE/SilverLink-BE/src/main/java/com/aicc/silverlink/SilverLinkApplication.java`
- Build: `BE/SilverLink-BE/build.gradle`
- Config: `src/main/resources/application*.yml`, `src/test/resources/application-ci.yml`
- Domain root: `BE/SilverLink-BE/src/main/java/com/aicc/silverlink/domain`
- Shared config/common: `global/config`, `global/common`
- External infra: `infra/external`, `infra/storage`, `infrastructure/callbot`
- Tests: `BE/SilverLink-BE/src/test/java/com/aicc/silverlink`

## Common Areas

- Auth/passkey/phone/session: `domain/auth`, `domain/session`, `global/config/auth`, Redis Lua under `src/main/resources/redis`.
- Member roles: `domain/user`, `domain/admin`, `domain/guardian`, `domain/counselor`, `domain/elderly`.
- Calls: `domain/call`, `domain/elderly`, `infrastructure/callbot`.
- Notices/files/notifications: `domain/notice`, `domain/file`, `domain/notification`, `infra/storage`.
- OCR/chatbot bridge: `domain/ocr`, `domain/chatbot`, `infra/external/ai`.

## Search Recipes

- `rg -n "@RequestMapping\\(\"/api/auth|class .*Controller|SecurityFilterChain|OncePerRequestFilter" BE/SilverLink-BE/src/main/java`
- `rg -n "Redis|Session|AuthSession|JWT|Passkey|WebAuthn" BE/SilverLink-BE/src/main/java BE/SilverLink-BE/src/test/java`
- `rg -n "Repository|@Transactional|@Entity|@ManyToOne|@OneToMany" BE/SilverLink-BE/src/main/java/com/aicc/silverlink/domain/<domain>`

## Work Rules

- Inspect controller, service, repository, DTO/entity, and matching test only for the target domain.
- Do not read `uploads`, generated build output, or SQL dump files unless the bug is about that artifact.
- Maintain `ApiResponse`/`PageResponse` patterns when a controller already uses them.
- Check security role assumptions in both backend and FE route guards before changing auth behavior.

## Verification

- Targeted: `./gradlew test --tests "*ClassNameTest"`.
- Default full unit suite: `./gradlew test`.
- Integration tests: `./gradlew integrationTest` only when `*IT` or external integration behavior is touched.
