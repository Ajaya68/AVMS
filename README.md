# AVMS - Ajaya Venture Management System

Multi-venture business management system for Ajaya Venture. Manage multiple
businesses (Mushroom, Fish Farming, Agriculture, and more) from one centralized
application.

## Technology Stack

| Layer       | Technology                                            |
| ----------- | ----------------------------------------------------- |
| Frontend    | React 19, Vite, Bootstrap 5, React Router, Axios      |
| Backend     | Python 3.14, Django 5.2 LTS, Django REST Framework    |
| Database    | MySQL 8.0                                             |
| Auth        | JWT (`djangorestframework-simplejwt`)                 |
| API Docs    | drf-spectacular (Swagger/OpenAPI)                     |

## Project Structure

```text
AVMS/
├── backend/                 # Django REST API
│   ├── config/              # settings, urls, wsgi/asgi, PyMySQL bootstrap
│   ├── accounts/            # users, roles, permissions, JWT auth
│   ├── ventures/            # enterprise units (Phase 3)
│   ├── customers/           # customers (Phase 4)
│   ├── suppliers/           # suppliers (Phase 4)
│   ├── products/            # categories, units, products (Phase 4)
│   ├── inventory/           # warehouses, inventory, stock movements (Phase 5)
│   ├── sales/               # sales, sales returns (Phase 7)
│   ├── purchases/           # purchases, purchase returns (Phase 6)
│   ├── expenses/            # expenses (Phase 8)
│   ├── employees/           # employees (Phase 9)
│   ├── payments/            # payments (Phase 8)
│   ├── reports/             # reports (Phase 10)
│   ├── notifications/       # notifications (Phase 11)
│   ├── audit/               # audit logs (Phase 11)
│   └── core/                # shared base models, responses, health
└── frontend/                # React SPA
    └── src/
        ├── components/      # Sidebar, Topbar, LoadingSpinner, ProtectedRoute
        ├── layouts/         # MainLayout (sidebar + navbar + content)
        ├── pages/           # Dashboard, Login, NotFound, Placeholder
        ├── routes/          # AppRoutes
        ├── services/        # api, authService, systemService
        ├── context/         # AuthContext
        ├── hooks/           # useApi
        └── utils/           # navItems, feature flags
```

## Current Status

**Phase 1 (Foundation) - COMPLETE**

- Django project with clean module separation (models / serializers / views /
  services / permissions are per-app)
- Custom email-based `User` model (`accounts.User`), chosen now so the initial
  migrations are correct and the auth model never needs to be swapped later
- MySQL 8 wired through environment variables (PyMySQL driver)
- Initial migrations applied; Django checks pass
- Health endpoints (`/api/health/`, `/health/`) report service + database state
- OpenAPI schema + Swagger UI served under `/api/schema/` and `/api/docs/`
- React (Vite) shell with Bootstrap layout
- `.env.example` templates and `.gitignore`
- Dev credentials: superuser `admin@avms.local` / `Admin@12345`

**Phase 2 (Authentication) - COMPLETE**

- Email + password JWT login; refresh (rotated) and logout (blacklisted) tokens
- `Role` / `Permission` models with a capability matrix
  (`module.view` / `module.manage`); six seeded roles: ADMIN, MANAGER,
  ACCOUNTANT, SALES_STAFF, INVENTORY_STAFF, EMPLOYEE (`python manage.py seed_roles`)
- Backend-enforced authorization via DRF permission classes; superusers bypass
- Endpoints: login, refresh, logout, me, users (list/create/detail/patch), roles
- Minimal `AuditLog` records LOGIN / LOGOUT / user administration events
- React login page, real `AuthContext` with `/auth/me/` session hydration,
  single-flight JWT refresh interceptor, permission-aware sidebar, Users and
  Roles pages, protected routes enabled
- Test suite green (29 tests: accounts roles/permissions + full auth API flow)

Planned: Phase 3 (Venture) -> Phase 4 (Masters) -> ... per `docs/ARCHITECTURE.md`.

## Running the Project

### Prerequisites

- Python 3.14+
- Node.js 20+
- MySQL 8.0 running locally on port 3306

### 1. Backend

```bash
cd backend

# Create a virtual environment and install dependencies
python -m venv venv
venv/Scripts/activate            # Windows (git-bash: source venv/Scripts/activate)
pip install -r requirements.txt

# Configure environment
cp .env.example .env             # then edit values for your MySQL

# Prepare the database (create once; MySQL 8)
mysql -u root -p -e "CREATE DATABASE avms CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# Optional: dedicated app user (recommended over root)
mysql -u root -p -e "CREATE USER 'avms_user'@'localhost' IDENTIFIED BY 'your-password'; \
                     GRANT ALL PRIVILEGES ON avms.* TO 'avms_user'@'localhost'; \
                     GRANT ALL PRIVILEGES ON \`test_avms\`.* TO 'avms_user'@'localhost';"

# Migrate, seed roles and run
python manage.py migrate
python manage.py seed_roles
python manage.py createsuperuser --email admin@avms.local
python manage.py runserver 0.0.0.0:8000
```

Backend runs at http://localhost:8000 (API under `/api/`).

### 2. Frontend

```bash
cd frontend
npm install
cp .env.example .env        # VITE_API_URL=http://localhost:8000/api (or use the dev proxy)
npm run dev
```

Frontend runs at http://localhost:5173. In dev, `VITE_API_URL` may be left
unset; Vite proxies `/api/*` requests to Django automatically.

### 3. Verify

```bash
curl http://localhost:8000/api/health/
# {"success":true,"data":{"status":"ok","service":"avms-backend","version":"0.1.0","database":"ok"},...}
```

## Environment Variables

See `backend/.env.example` and `frontend/.env.example`. The backend `.env` is
authoritative for development; it overrides any pre-existing shell variables
with the same names.

## Testing

```bash
cd backend
python manage.py test
```

## API Documentation

Browse Swagger UI at http://localhost:8000/api/docs/ once the backend is running.

## License

Proprietary - internal use.