# AVMS — Migration Plan (Django → Spring Boot + Oracle + React)

## 1. Strategy

- **Modular monolith first.** Single Spring Boot 3 app (`com.avms`), one deployable, package-per-domain (see MODULE_MAPPING). Extract microservices only with genuine cause (independent scale/failure domain); none identified today (all modules share venture scope + single transactions across bills/stock/payments).
- **API-compatible rewrite.** Keep paths, envelope, pagination shape, status codes, permission codes, code formats, and `X-Venture-Id` semantics so the existing React app re-points with only base-URL change.
- **Dependency order:** common/config → accounts+ventures → masters → inventory → purchases → sales → payments → expenses → employees → reports → notifications → audit-read → frontend re-point → hardening. Audit *service* ships in step 1 (writes from day one); audit *read API* can land last.
- **No placeholders:** every feature ships entity→repo→DTO→validation→service→controller→security→audit/notify→tests, or stays `[ ] Not started`.

## 2. Phase 0 — Foundation (this iteration)

1. Scaffold `backend-java/` Maven project (Java 17, Spring Boot 3.4, Web/Validation/Data-JPA/Security, Oracle JDBC, JJWT, Springdoc, MapStruct optional, JUnit5/Mockito/AssertJ, H2 for tests).
2. `common`: `ApiResponse<T>`, `PageResponse<T>` (DRF-compatible), `GlobalExceptionHandler` (400/401/403/404/422/503), `BaseEntity`, `SequenceCounter` + `SequenceService` (PESSIMISTIC_WRITE), `VentureContextHolder`, `AuditService`, `NotificationService` skeleton, `HealthController`, OpenAPI config, `application.yml` (dev/test/prod profiles, env overrides).
3. Security skeleton: JWT filter, `PermEvaluator`, `SeedRolesService`, refresh-token entity, BCrypt + legacy PBKDF2 verifier.
4. Docker Compose (Oracle XE + backend + frontend), `Dockerfile`, `.env.example`, Jenkinsfile skeleton, SonarQube + Logback (SLF4J) wiring.
5. Port `accounts` + `ventures` fully with tests. Mark them `[x]` only after green build.

## 3. Phases 1–6 (module waves)

| Wave | Modules | Entry criteria | Exit criteria |
|---|---|---|---|
| 1 | customers, suppliers, products | accounts+ventures green | CRUD + codes + 404 scoping + seeds (units) tested |
| 2 | inventory | masters green | movement math + transfers + low-stock notify tested |
| 3 | purchases (+returns) | inventory green | totals + stock posting + over-return + delete-block tested |
| 4 | sales (+returns) | purchases green (reuse pattern) | oversell reject + returns + notify tested |
| 5 | payments, expenses | bills green | cap/reversal/type-ref + category seeds tested |
| 6 | employees, reports, notifications, audit-read | finance green | bulk/self-service + aggregates + fan-out + filters tested |

Each wave: implement → `mvn verify` → compare behavior vs Django (same requests, same responses) → update MODULE_MAPPING checklist → commit.

## 4. Frontend

- No rewrite: existing React 19 + Bootstrap app is already the target stack (HTML/CSS/JS/React/Bootstrap). Work per wave: re-point `VITE_API_URL` to Spring Boot (default `http://localhost:8080/api`), regression-click every touched page, fix only contract deltas (documented in API_MAPPING). Keep `X-Venture-Id`, refresh single-flight, envelope unwrap, toasts, responsive behavior intact. Remove only provably dead code (`PlaceholderPage.jsx`) after sign-off.

## 5. Data migration

- Flyway/Liquibase versioned DDL mirroring DATABASE_MAPPING; seed `V2` (units, expense categories, roles/permissions). Legacy MySQL → Oracle ETL with row-count verification; `IDENTITY` PKs re-based above max imported id; `SequenceCounter.nextValue` initialized above max per-module code.

## 6. Cutover

- Parallel-run Django + Spring (read-shadow or dual-write for bills), then DNS/env flip per environment (dev→staging→prod), Django kept read-only rollback for one cycle. Health + JMeter smoke before each flip.

## 7. Definition of done (per module)

Compiles · unit+integration tests green · SonarQube clean (no blocker/critical) · API parity spot-checked vs Django · security rules enforced · audit/notify hooks firing · checklist updated · deltas documented.
