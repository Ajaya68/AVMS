# AVMS — Security Mapping (Django/SimpleJWT → Spring Security + JJWT)

## 1. Current state (Django)

- Email+password login; `TokenObtainPair` customized (`LoginSerializer` returns `{access,refresh,user}`); `WWW-Authenticate: Bearer realm=api` on login.
- Access 30m / refresh 7d (env-overridable `JWT_*`), `ROTATE_REFRESH_TOKENS + BLACKLIST_AFTER_ROTATION`, `UPDATE_LAST_LOGIN`, `token_blacklist` app.
- `IsAuthenticated` default; per-view `HasPermission(code)` / `HasPermissionFromView` / `IsAdmin`; superuser bypass (`User.has_permission_code`).
- Passwords: Django PBKDF2 hasher; reset via `default_token_generator` + `uidb64`; change-password requires current; forgot never enumerates; `reset_url` in DEBUG only.
- Guards: superuser undeletable, self-delete blocked, cross-venture detail 404, cross-user notification read 404, attendance self-scope.
- CORS allows `x-venture-id`; prod flags (secure cookies/HSTS/SSL-redirect opt) when `DEBUG=False`.

## 2. Target (Spring Security 6 + JJWT)

- `SecurityFilterChain`: stateless (`SessionCreationPolicy.STATELESS`), CSRF disabled (JWT bearer API), CORS (`allowedHeaders: x-venture-id, authorization, content-type`; origins from env), `authorizeHttpRequests`: permitAll `/health`, `/api/health`, `/api/auth/login|refresh|forgot-password|reset-password`, `/v3/api-docs/**`, `/swagger-ui/**`; everything else authenticated + permission voter.
- `JwtAuthenticationFilter` (OncePerRequestFilter): validates HS256 access token (secret from env `JWT_SECRET`, ≥256-bit), loads `UserDetails` (email principal, authorities = `permission_codes` + `ROLE_*`), sets `SecurityContext`; 401 on expired/invalid with envelope `{success:false,message}`.
- Permission enforcement: method security (`@EnableMethodSecurity`) + custom `@RequiresPermission("sales.manage")` / SpEL `@PreAuthorize("@perm.has('sales.manage')")`; `PermEvaluator` grants if `authentication.authorities` contains code or user is superuser (`ROLE_SUPERUSER` / `isSuperuser` flag). `IsAdmin` → `@RequiresPermission("users.manage")` + superuser OR.
- Password storage: `BCryptPasswordEncoder` (strength 10-12). **Django PBKDF2 hashes are not BCrypt** — support dual-check on login (PBKDF2 verifier for legacy `$pbkdf2-sha256$…` hashes, re-hash to BCrypt on success) OR force reset; document choice in deployment notes.
- Refresh rotation: `REFRESH_TOKEN` table (jti, user, expires, revoked); refresh endpoint rotates (revoke old, issue new pair); logout revokes by jti. Equivalent to `token_blacklist`.
- Password reset: stateless HMAC time-bound token (`uidb64 + expiry + signature` with `JWT_SECRET`) mirroring `default_token_generator` semantics; single-use optional via consumed-token cache; DEBUG/dev profile returns `reset_url`, prod returns generic 200.
- Venture scoping: `VentureScopeFilter`/`@VentureScoped` — resolves `X-Venture-Id` header per request into `VentureContextHolder` (request-scoped); repositories filter by it; missing entity in venture → `ResourceNotFoundException` → 404 (preserves ID-guessing behavior).
- Audit: `AuditService` captures principal + IP (`X-Forwarded-For` first) on every mutation (AOP or explicit service call, mirroring Django explicit calls).
- Secrets: env-only (`JWT_SECRET`, `DB_*`, `CORS_*`); never hard-code; fail-fast at startup if `JWT_SECRET` missing/weak in prod profile.
- Headers: `HSTS`, `X-Frame-Options DENY`, `X-Content-Type-Options`, secure/HttpOnly cookie flags (cookies only if used); keep parity with Django prod hardening.

## 3. Role → permission matrix (unchanged)

Seed identical 6 roles × ~44 `{module}.view/manage` codes via `SeedRolesService` (idempotent, mirrors `seed_roles.py`): ADMIN=all, MANAGER, ACCOUNTANT, SALES_STAFF, INVENTORY_STAFF, EMPLOYEE=dashboard.view. Superuser bypass preserved. Frontend `hasPerm()` logic unchanged.

## 4. What must NOT weaken

- No endpoint left without auth except the public list above; no `permitAll` on business APIs.
- Keep per-method view/manage split (GET=view, writes=manage), owner-scoping (notifications), self-scoping (attendance/me), delete guards, non-enumerating forgot-password.
