# AVMS — Deployment Architecture (Maven · Docker · Jenkins · AWS · ELK · Datadog)

## 1. Topology

- **Local:** `docker-compose.yml` → `oracle-xe:21-slim` (1521), `avms-backend` (8080, Spring `dev` profile), `avms-frontend` (5173, Vite, `VITE_API_URL=http://localhost:8080/api`). Volumes for Oracle data; `.env` for secrets.
- **CI (Jenkins):** `Jenkinsfile` stages: checkout → `mvn -B verify` (tests + coverage) → SonarQube → `docker build` backend+frontend → Trivy/dependency-check → push ECR → deploy to env (staging auto, prod manual gate) → JMeter smoke → Datadog deploy marker.
- **AWS (staging/prod):** VPC (2 AZ) → ALB (TLS, `/api/*` → backend TG, `/*` → frontend S3+CloudFront or ECS static) → ECS Fargate services (backend ×2+, frontend static via S3/CloudFront preferred) → RDS Oracle (Multi-AZ) → Secrets Manager (`JWT_SECRET`, `DB_*`) → CloudWatch logs → ELK forwarder + Datadog agent (APM/logs/RUM). Jenkins on EC2 or shared controller with AWS creds via IAM role.
- **Why not microservices now:** single transactional boundary (bill↔stock↔payment), shared venture scope, small-team ops. ECS services can split later along `reports` (read-heavy) or `notifications/audit` (write-fanout) if metrics justify.

## 2. Build

- `backend-java/pom.xml` (Maven; Gradle only if a module genuinely needs it — none does today): Spring Boot 3.4 parent, Java 17, profiles `dev/test/prod` via `application-{profile}.yml`, env var overrides (`DB_URL/USER/PASSWORD`, `JWT_SECRET/ACCESS_MIN/REFRESH_DAYS`, `CORS_ORIGINS`, `LOG_LEVEL`).
- `Dockerfile` (multi-stage: `maven:3.9-eclipse-temurin-17` build → `eclipse-temurin:17-jre` run, non-root user, `HEALTHCHECK /api/health/`).
- Frontend `Dockerfile` (build → Nginx static) for prod; dev uses Vite directly.

## 3. Config / secrets

- Never hard-code. `backend-java/.env.example` documents all tunables (mirrors Django `.env.example` + `JWT_SECRET`, `DB_URL`). Prod reads Secrets Manager → env injection. Fail-fast if `JWT_SECRET` missing/weak or DB unreachable (health 503, container restart).

## 4. Observability

- **Logging:** Logback (SLF4J) JSON console (`level,ts,logger,msg,traceId,user,venture,ip`) — mirrors Django structured console; fields `user/venture/ip` via MDC filters. Logstash/Beats → Elasticsearch → Kibana dashboards (error rate, 422 business rejects, auth failures, slow queries).
- **Datadog:** `dd-java-agent` APM + log correlation, RUM on frontend, monitors (5xx spike, p95 latency, failed logins, oversell/overpay attempts, Oracle storage/connections), deploy markers from Jenkins.
- **Health:** ALB/ECS target = `GET /api/health/` (200 ok / 503 degraded).

## 5. Database ops

- Flyway migrations versioned (`V1__schema.sql` …); RDS Oracle backups + PITR; pre-cutover ETL verified by row counts; `SequenceCounter` re-based; rollback = redeploy prior image + Django read-only fallback for one cycle.
