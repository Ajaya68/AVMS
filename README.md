# AVMS - Ajaya Venture Management System

Multi-venture business management system for Ajaya Ventures. Manage multiple
businesses (Mushroom, Fish Farming, Agriculture, and more) from one centralized
application: masters, inventory, purchases, sales, payments, expenses,
employees, reports, notifications and audit trail.

## Technology Stack

| Layer    | Technology                                              |
| -------- | ------------------------------------------------------- |
| Frontend | React 19, Vite, Bootstrap 5, React Router, Axios        |
| Backend  | Java 17, Spring Boot 3.4 (Web, Data JPA, Security)      |
| Database | Oracle 21c (Flyway migrations)                          |
| Auth     | JWT (access + rotating refresh tokens)                  |
| API Docs | springdoc-openapi (Swagger UI)                          |

## Project Structure

```text
AVMS/
├── backend-java/            # Spring Boot REST API (port 8081)
│   ├── src/main/java/com/avms/
│   │   ├── accounts/        # users, roles, permissions, JWT auth
│   │   ├── ventures/        # enterprise units (V-#### codes)
│   │   ├── customers/       # customers (C-####)
│   │   ├── suppliers/       # suppliers (S-####)
│   │   ├── products/        # units, categories, products (P-#### SKUs)
│   │   ├── inventory/       # warehouses (W-####), stock, movements
│   │   ├── sales/           # sale bills (SINV-####) + returns (SRET-####)
│   │   ├── purchases/       # purchase bills (PINV-####) + returns (RET-####)
│   │   ├── payments/        # money in/out against bills
│   │   ├── expenses/        # categories + operating expenses
│   │   ├── employees/       # employees (EMP-####) + attendance
│   │   ├── reports/         # sales, purchases, inventory, financial reports
│   │   ├── notifications/   # per-user inbox + auto alerts
│   │   ├── audit/           # audit log trail
│   │   ├── security/        # JWT filter, permission evaluator
│   │   └── common/          # base entity, responses, sequencing, filters
│   └── src/main/resources/db/migration/  # Flyway V1..V7 (Oracle DDL)
├── frontend/                # React SPA (Vite dev port 5174)
│   └── src/
│       ├── components/      # Sidebar, Topbar, MasterEntityPage, ...
│       ├── pages/           # Dashboard, masters, transactions, reports, ...
│       ├── services/        # per-module API clients
│       ├── context/         # AuthContext (JWT + permissions)
│       └── utils/           # navItems (permission-gated sidebar)
├── tests/jmeter/            # smoke test plan (CI)
├── docker-compose.yml       # Oracle XE + backend + frontend containers
└── Jenkinsfile              # CI: backend verify, frontend build, docker
```

## Prerequisites

- Java 17+, Maven 3.9+, Node.js 20+
- Oracle 21c running locally (listener on port 1521) with a pluggable
  database (e.g. `orclpdb`) and an `avms` user (see step 1)

## Step-by-step Setup

### 1. Database (once)

```sql
-- as SYSDBA
ALTER SESSION SET CONTAINER = orclpdb;
CREATE USER avms IDENTIFIED BY avms;
GRANT CONNECT, RESOURCE TO avms;
ALTER USER avms QUOTA UNLIMITED ON USERS;
```

Tables are created automatically by Flyway on first backend start.

### 2. Backend

```bash
cd backend-java
mvn -B package -DskipTests        # builds target/avms-backend-*.jar

# run (PowerShell / CMD)
set SPRING_PROFILES_ACTIVE=dev
set DB_URL=jdbc:oracle:thin:@localhost:1521/orclpdb
set DB_USER=avms
set DB_PASSWORD=avms
java -Duser.timezone=UTC -jar target/avms-backend-0.1.0.jar --server.port=8081
```

Backend runs at http://localhost:8081 (API under `/api/`).
Swagger UI: http://localhost:8081/api/docs.html.

### 3. Frontend (new terminal)

```bash
cd frontend
npm install                        # first time only
set VITE_API_URL=http://localhost:8081/api
npm run dev
```

Frontend runs at http://localhost:5174. Log in with your admin account
(e.g. `ajayamahanty68@gmail.com`). New users are created from the
**Users** page; permissions are granted via **roles** on the **Roles** page.

### 4. Verify

```bash
curl http://localhost:8081/api/health/
# {"success":true,"data":{"status":"ok","service":"avms-backend",...}}
```

## Notes

- Every request can carry an `X-Venture-Id` header (set automatically from
  the venture selector in the top bar) to scope data to one venture.
- Document codes (`V-0001`, `C-0001`, `SINV-0001`, ...) are generated per
  venture and never reused.
- Run backend tests with `mvn -B verify` inside `backend-java/`.
- With Docker available: `docker compose up` starts Oracle XE + backend
  (8080) + frontend (5173) using the default `avms/avms` database user.

## License

Proprietary - internal use.
