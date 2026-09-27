# AVMS — Testing Strategy (JUnit/Mockito + integration + JMeter + SonarQube)

## 1. Backend (Java)

- **Unit** (JUnit 5 + Mockito + AssertJ, H2 or mocked repos): money `recompute()` math, code generation format/no-reuse, validators (phone regex, salary≥0, qty>0, type↔ref), permission evaluator (incl. superuser bypass), over-payment/over-return/insufficient-stock guards, venture-consistency checks.
- **Slice/integration** (`@SpringBootTest` + `@Transactional`, H2Oracle-mode or Testcontainers Oracle-XE): every endpoint happy-path + 403 (no perm) + 404 (cross-venture) + 422 (business rule) + audit row written + notification fan-out; refresh rotation/blacklist; seed idempotency; report aggregates vs hand-computed fixtures.
- **Regression ports** (from Django `regression/tests.py`, 5 scenarios): low-stock reorder-type crash; sale-notify audience (managers+superusers, not plain staff); notification ownership 404; cross-venture detail invisibility (customers/products/expenses/sales+items); purchase→settle→over-pay-reject (count stays 1).
- **Contracts:** OpenAPI snapshot (`/v3/api-docs`) diffed per wave; envelope/pagination shape asserted.
- **Coverage gate:** ≥80% line on `service/` + `controller/`, 100% on money/stock/payment paths. `mvn verify` runs all; Jenkins fails build below gate or on Sonar blocker/critical.

## 2. Frontend

- Keep `npm run lint` (oxlint) + `npm run build` green per wave.
- Component tests (Vitest + Testing Library, add when touching a page): MasterEntityPage CRUD shell, Sale/Purchase dynamic-line forms (totals math), PaymentForm amount-cap, return forms, Reports tabs, Notifications/Audit filters, VentureSelector header injection, ProtectedRoute redirect.
- API/integration: mock Axios adapter asserting `X-Venture-Id` + `Bearer` sent, envelope unwrap, 401 single-flight refresh, toast events.

## 3. Performance (JMeter)

Plans in `tests/jmeter/`: `auth_login_refresh.jmx`, `masters_crud.jmx`, `sale_post_oversell.jmx`, `purchase_settle_overpay.jmx`, `reports_30d.jmx`, `stock_movement_burst.jmx`. Baseline vs Django (p95 latency, error %, DB locks under concurrent `SALE` on same product — must serialize, not oversell). Run pre-cutover per environment; gate: 0 oversell/overpay under concurrency, p95 within 1.5× Django or documented cause.

## 4. Quality (SonarQube)

`sonar-project.properties` + Jenkins `sonar:sonar` stage; rules: no hardcoded secrets, no `SELECT *` native queries, `@Transactional` on all mutating services, no swallowed exceptions, validated DTOs (`@Valid`), SLF4J parameterized logging (no string concat in hot paths). OWASP dependency-check on `pom.xml`.
