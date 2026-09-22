# AJAYA VENTURE — Business Management Platform

Full-stack, multi-business-unit management platform.

- **Parent organization:** Ajaya Venture
- **Initial business units:** AB Mushroom Farming, Ajaya Fish Farming
- **Database:** Oracle Database 21c — Oracle SQL + PL/SQL
- **Backend:** Java SE + Jakarta EE (Servlet + JSP) + JDBC
- **Frontend:** React + JavaScript + Bootstrap
- **DevSecOps:** Maven, Docker, Git, Jenkins, JUnit, Mockito

## Repository layout

```
backend/      Jakarta EE Servlet/JSP backend (Maven WAR)
frontend/     React single-page application
database/     Ordered SQL migrations, seeds, procedures, views
docs/         Architecture and operations documentation
deployment/   Nginx and compose deployment assets
tests/        End-to-end / integration test resources
```

## Development quick start

1. Copy `.env.example` to `.env` and set `DB_PASSWORD` / `DB_MIGRATION_PASSWORD`.
2. Bootstrap the Oracle schema and users:

   ```bash
   bash scripts/bootstrap_db.sh
   ```

3. Apply migrations:

   ```bash
   bash scripts/migrate.sh
   ```

4. Build and test the backend:

   ```bash
   mvn -f pom.xml clean verify
   ```

5. Run the frontend dev server:

   ```bash
   cd frontend && npm install && npm run dev
   ```

## Phase map

See `docs/architecture.md` and the master specification. Delivery is phased:

0. Foundation · 1. Organization & Security · 2. Locations & Master Data ·
3. Inventory · 4. Procurement · 5. Sales · 6. Mushroom · 7. Fish ·
8. Finance · 9. Reporting · 10. Production hardening