---
name: react-admin-ui
description: Use for Vite React TypeScript UI work, admin/guardian/counselor/senior pages, API client wiring, routing, auth guards, shadcn/Tailwind components, and frontend build issues.
---

# React Admin UI Skill

## Start Here

- Read `docs/architecture-map.md`.
- Find the route/page in `FE/SilverLink-FE/src/App.tsx`, then inspect that page and its API client only.

## Primary Files

- Entry/routing: `src/main.tsx`, `src/App.tsx`
- API base/client: `src/api/index.ts`
- Domain API clients: `src/api/*.ts`
- Auth/session: `src/contexts/*`, `src/components/auth/*`, `src/hooks/useTokenRefresh.ts`, `src/hooks/useSessionTimeout.ts`
- Layout/nav: `src/components/layout/DashboardLayout.tsx`, `src/config/*NavItems.tsx`
- Role pages: `src/pages/admin`, `guardian`, `counselor`, `senior`, `user`, `auth`, `map`, `faq`
- UI primitives: `src/components/ui/*`

## Search Recipes

- `rg -n "path=\"/admin|allowedRoles|ProtectedRoute|getRoleHomePath" FE/SilverLink-FE/src`
- `rg -n "apiClient|VITE_API_BASE_URL|VITE_AI_API_BASE_URL|axios" FE/SilverLink-FE/src`
- `rg -n "ComponentName|function .*|const .* =" FE/SilverLink-FE/src/pages FE/SilverLink-FE/src/components`

## Work Rules

- Do not bulk-read `src/components/ui`; open only primitives imported by the target page.
- Keep role boundaries aligned with `ProtectedRoute` and backend role names: `ADMIN`, `GUARDIAN`, `COUNSELOR`, `ELDERLY`.
- Prefer existing components, Tailwind utilities, Axios client, toast/error helpers, and layout patterns.
- For API changes, inspect the matching backend controller/DTO before editing frontend types.
- Watch for duplicate admin routes in `App.tsx` before assuming routing behavior.

## Verification

- `npm run lint` for focused UI changes.
- `npm run build` for routing, import, type, or API client changes.
