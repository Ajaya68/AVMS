# AJAYA VENTURE Architecture

## Runtime topology

```
React SPA (frontend/dist served by Nginx)
        │ HTTPS/JSON /api/*  (dev: Vite proxy → :8080)
        ▼
Tomcat 11 · Jakarta EE 10 (backend WAR)
        │ Servlet / REST-style API
        ▼
Service Layer  (authorization · validation · business rules ·
                transaction orchestration · audit)
        ▼
DAO Layer (JDBC PreparedStatement/CallableStatement)
        ▼
Oracle Database 21c (SQL + PL/SQL)
```

JSP is reserved for server-rendered reports/invoices, not the main UI.

## Schema ownership model

Three Oracle schemas per the least-privilege principle:

| Schema        | Role                                                        |
| ------------- | ----------------------------------------------------------- |
| `AV_MIGRATION`| Owns DDL (tables, views, sequences, packages). Runs migrations. |
| `AV_APP`      | Runtime schema. DML on tables, EXECUTE on packages, synonyms. Credentials used by the WAR. |
| `AV_REPORT`   | Read-only reports via granted SELECT on views/tables.        |

`scripts/grant_app.sh` regenerates grants/synonyms after every migration.

## Organization data model

- `AV_ORGANIZATION` is the root row (Ajaya Venture).
- `AV_BUSINESS_UNIT` are direct children (AB Mushroom Farming, Ajaya Fish Farming).
- Operational resources hang below business units through real composite
  foreign keys enforcing `(ORGANIZATION_ID, BUSINESS_UNIT_ID)` scope.
- Shared masters (products, customers, suppliers) are organization-owned and
  bridged to business units (`*_BUSINESS_UNIT` tables).

## Delivery phases

0. Foundation (this build) · 1. Organization & Security · 2. Locations & Master
Data · 3. Inventory · 4. Procurement · 5. Sales · 6. Mushroom · 7. Fish ·
8. Finance · 9. Reporting · 10. Production hardening.

## Database migrations

Ordered `database/migrations/V<version>__<name>.sql` scripts applied by
`scripts/migrate.sh`, tracked in `AV_SCHEMA_MIGRATION`. Manual production
schema edits are not part of the normal workflow.