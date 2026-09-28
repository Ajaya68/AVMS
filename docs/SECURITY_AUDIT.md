# Security + Production-Readiness Audit — AVMS (2026-09-28)

Role: senior app-sec / architect / production-readiness review of an AI-assisted codebase.
Rule followed throughout: smallest safe change, no working functionality removed,
every fix verified by a real run (no invented results).

## 1. Architecture (as found)

- Backend: Java 17 / Spring Boot 3.4 (Web, Data JPA, Security, Validation), Flyway
  V1–V7, Oracle 21c (prod) / H2 (tests). `ddl-auto: validate`, stateless JWT.
- Auth: `POST /api/auth/login` (BCrypt primary, legacy Django PBKDF2 re-hashed on
  success), rotating refresh tokens in `REFRESH_TOKEN` table, `POST
  /api/auth/refresh|logout`, `GET /api/auth/me`, `POST /api/auth/change-password`.
  Password-reset DTOs exist but **no controller endpoints** — frontend
  Forgot/Reset pages call APIs that 404 (broken feature, not a vuln).
- Tenant isolation: `X-Venture-Id` header → `VentureScopeFilter` →
  `ThreadLocal` → per-service `findByIdAndVentureId` / `(:ventureId is null or …)`
  queries. There is **no user→venture membership model** — see §3 item 1.
- Frontend: React 19 + Vite, tokens in `localStorage`, Axios interceptors with
  single-flight refresh. No `dangerouslySetInnerHTML` (0 hits). React-escaped.
- Deploy: `docker-compose.yml` (dev), `docker-compose.prod.yml` + `Caddyfile`
  (VPS, auto-TLS), `render.yaml` + `frontend/vercel.json` (split hosting).

## 2. Fixes applied + verified in this pass

| # | Finding | Severity | Fix (minimal) | Verification |
|---|---------|----------|---------------|--------------|
| 1 | App booted with published placeholder JWT secret (`change-me-…` in code/compose); only a 32-char length check | **Critical** | `JwtService` now rejects any secret containing `change-me`/`please-change`; test profile got its own test-only secret; dev compose/`.env.example` use a labeled dev-only value; prod compose/Render already require a real secret | `mvn -B verify`: **20 tests, 0 failures, JaCoCo 60% gate met, BUILD SUCCESS** (2026-09-28) |
| 2 | Swagger/OpenAPI publicly reachable in prod (SecurityConfig permitAll + Caddy proxied `/v3/api-docs/*`, `/swagger-ui*`) | High | springdoc **disabled by default** (`SPRINGDOC_ENABLED:false`), enabled in dev profile only; Caddy docs handles removed; deploy guides updated | Backend suite green (tests run with docs disabled); no endpoint removed in dev |
| 3 | No security headers anywhere (backend, nginx, Caddy, Vercel) — clickjacking/MIME-sniff | Medium | Additive-only headers: backend `frame DENY + frame-ancestors 'none' + same-origin referrer + minimal permissions-policy`; nginx/Caddy/Vercel `DENY/nosniff/same-origin/permissions-policy`; Caddy `HSTS`. No CSP on SPA (would break Vite inline bootstrap) | `npm run build` success (2.86s); backend suite green |
| 4 | CORS origins split without trim; `*` not rejected despite `allowCredentials=true` | Medium | Trim + drop empties; fail fast if `*` configured | Backend suite green |
| 5 | `POST /api/auth/logout` required authentication — users with expired access tokens couldn't log out server-side | Low | Added `/api/auth/logout[/]` to permitAll (handler already ignores bad tokens) | Existing `AuthControllerTest` logout step passes |
| 6 | Refresh-token replay only revoked the single token; stolen refresh = silent session theft | High | On revoked/expired refresh reuse, **revoke all tokens for that user** (`findByUserEmailIgnoreCase` + `saveAll`) before returning 401 | `AuthControllerTest` rotation + old-token-rejected + logout flow passes unchanged |
| 7 | Health endpoint version string + verbose degraded body (fingerprinting) | Info | **Kept as-is**: `Dashboard.jsx:306` renders `health.version` — removing would break working UI for negligible gain | n/a (conscious no-change) |

## 3. NOT fixed in code — exact manual action required

1. **Cross-venture reads without membership (P1 architectural).** `findScoped`
   falls back to unscoped `findById` when `X-Venture-Id` is absent, and any
   authenticated user can set any venture id — there is no user→venture
   membership table. Safe fix needs a schema + product decision, so it was
   deliberately left untouched. Manual action: introduce
   `user_venture_membership(user_id, venture_id, role)` via a new Flyway
   migration (V8+; never edit V1–V7), enforce membership in `resolveVenture`
   and `findScoped` (return 404 on non-member), backfill existing users, and
   add MockMvc tests for cross-venture 404s.
2. **Login brute-force throttling (P2).** Not added in-app (needs new 429
   mapping + throttle state; edge is the correct layer). Manual action: enable
   rate limiting at the edge (Cloudflare WAF / Render / Caddy `rate-limit`
   plugin) on `/api/auth/login` + `/api/auth/refresh` (e.g. 20 req/min/IP with
   block), alert on spikes, and confirm `AuditService` LOGIN/failed-login rows
   are monitored.
3. **JWT_SECRET rotation (P0 operational).** The placeholder secret existed in
   git history and dev defaults. Manual action: set a fresh random 32+ char
   `JWT_SECRET` in **every** deployed environment (`openssl rand -base64 48`),
   restart backends (invalidates all previously issued tokens by key change),
   and never reuse the dev/test values in prod. Prod compose (`...:?` guard)
   and Render (generated value) already enforce presence.
4. **Token storage (P2 strategic).** `localStorage` access+refresh means any
   XSS = session theft. React output is escaped and no raw-HTML sinks exist,
   so likelihood is low, but impact is high. Manual action (larger rework):
   migrate to HttpOnly `Secure`/`SameSite` cookies with CSRF protection, or at
   minimum shorten refresh lifetime and add refresh-reuse alerting.
5. **Password-reset flow (P3 functional).** DTOs + frontend pages exist, backend
   endpoints don't (frontend 404s). Manual action: either implement
   forgot/reset with single-use HMAC tokens + expiry + generic responses, or
   remove the frontend routes. Left untouched to avoid deleting UI.
6. **Secret hygiene (verified clean).** No real `.env` files are tracked
   (`git ls-files` shows only `.env.example` files); `target/`,
   `node_modules/`, `dist/` untracked. Keep it that way: real secrets only via
   environment (Render dashboard / VPS `.env.prod`, never committed).

## 4. Verification log (real runs, not estimates)

- `mvn -B verify` (backend-java, H2 suite): **Tests run: 20, Failures: 0,
  Errors: 0, Skipped: 0 — BUILD SUCCESS, JaCoCo checks met** (01:03 min).
- `npm run build` (frontend): **success in 2.86s** (pre-existing >500 kB chunk
  warning unchanged).
- `vercel.json` JSON-parse check: OK. `docker-compose.prod.yml` config check:
  valid (earlier pass).

## 5. Secrets sweep + continuous scanning (2026-09-28)

Full-repo sweep (source, config, tests, seeds, docs, CI, tracked files, all 21
commits, built frontend bundle): **no live secrets found**. No API keys,
tokens, private keys, cloud credentials, live connection strings, `.env`
files (none exist, none ever committed), bundle secrets, logs, or screenshots.
Test credentials are H2-suite fixtures only; seed SQL carries no credentials;
dev defaults are labeled dev-only with fail-fast guards in prod paths, so
nothing required moving — all secret consumption was already env-driven.

Scanning added to keep it that way: `.gitleaks.toml` (default rules, with
allowlists strictly limited to labeled placeholders/dev-only/test fixtures)
plus `.github/workflows/secret-scan.yml` (Gitleaks on every push and PR).
Secret values are never reproduced in reports.

## 6. Residual risk statement

After this pass: no boot-with-known-secret, no public API docs in prod,
hardened transport headers on all four layers, CORS fail-closed, logout
usable, refresh replay contained. Remaining material risks are §3 items 1–4
(membership model, edge throttling, secret rotation in deployed envs, token
storage strategy) — all require owner decisions/actions beyond safe automation.
