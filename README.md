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

### Phase 3 - Ventures (complete)
- `ventures` app: venture CRUD at `/api/ventures/` with auto `V-####` codes,
  business type + status, blanket `ventures.view`/`ventures.manage` permissions
- Serialized code generator (`VentureCodeCounter`) — DB-counter based (test-safe)
- Venture-scoping helpers (`get_request_venture`, `scope_queryset_by_venture`)
  ready for customer/supplier/product phases
- Frontend: Ventures list (search), detail page, create/edit modal, activate/
  deactivate + delete (write actions hidden without `ventures.manage`)
- Every venture create/update/delete writes an `AuditLog`
- Backend suite green (42 tests), lint 0 warnings, build OK

### Phase 4 - Masters (complete)
- `customers`, `suppliers`, `products`, `categories`, `units` apps with shared
  CRUD base views (`core/crud_views.py`) + serialized per-venture code
  generator (`core.SequenceCounter`, `generate_code`)
- Auto codes: customers `C-####`, suppliers `S-####`, products `P-####` (unique
  SKU per venture); units seeded (kg, g, pcs, packet, litre, box)
- All master reads honour the `X-Venture-Id` header; `{module}.view/manage`
  permissions enforced; writes audited
- Frontend: generic `MasterEntityPage` CRUD screen, Venture selector in Topbar
  drives `X-Venture-Id`, dedicated Customers/Suppliers/Products/Categories/Units
  pages; lint 0 warnings, build OK
- Backend suite green (57 tests)

### Phase 5 - Inventory (complete)
- `inventory` app: warehouses (auto `W-####` codes), stock movements that
  transactionally apply quantity deltas under row locks, transfer pairs
  (OUT→IN), and an inventory view (on hand / reserved / available / low stock)
- Movement types: PURCHASE, SALE, PURCHASE_RETURN, SALES_RETURN, ADJUSTMENT_IN,
  ADJUSTMENT_OUT, TRANSFER_OUT; outgoing types enforce sufficient stock
- `GET /api/inventory/?low=true` returns only low-stock lines
- Frontend: Warehouses CRUD, Inventory table with warehouse + low-stock filters,
  Stock Movements list + record-movement modal (transfer aware)

### Phase 6 - Purchases (complete)
- `purchases` app: purchase bills with nested item lines, auto invoice codes
  (`PINV-####`), computed totals (subtotal/discount/tax/total/paid/due),
  statuses (PENDING/PARTIAL/COMPLETED/RETURNED/CANCELLED)
- Posting a purchase atomically records `PURCHASE` stock movements at its
  warehouse; purchase returns apply `PURCHASE_RETURN` movements and reduce the
  bill balances (returned/due) with cumulative-quantity validation
- Delete of a stock-received purchase blocked (reverse via return instead)
- Frontend: Purchases (list/search/status filter, new-purchase form with
  dynamic item rows, detail w/ balances, return-from-detail), Purchase Returns
  (list + shared return form)
- Backend suite green (84 tests)

### Phase 7 - Sales (complete)
- `sales` app: bills with nested item lines, auto invoice codes (`SINV-####`),
  computed totals, statuses (PENDING/PARTIAL/COMPLETED/RETURNED/CANCELLED)
- Posting a sale atomically applies `SALE` stock-out movements at its dispatch
  warehouse and rejects if any line exceeds available stock; sales returns
  apply `SALES_RETURN` movements (stock back in) and reduce balances with
  cumulative-quantity validation
- Delete of a stock-dispatched sale blocked (reverse via return instead)
- Frontend: Sales (list/search/status filter, new-sale form with dynamic item
  rows sized from selling price, detail w/ balances, return-from-detail),
  Sales Returns (list + shared return form)
- Backend suite green (98 tests)

### Phase 8 - Finance (complete)
- `payments` app: RECEIVED payments settle sale dues (money in), PAID payments
  settle purchase dues (money out); amount is capped at the bill's outstanding
  balance, applying updates the bill's paid/due, DELETE reverses the payment
- `expenses` app: category-based operating expenses (static categories seeded
  via data migration), full CRUD
- Frontend: Payments (list w/ type filter, record-payment modal that picks
  open sale/purchase references and caps the amount, one-click reversal),
  Expenses (list/search/category filter, inline delete, totals)
### Phase 9 - Employees (complete)
- `employees` app: auto-coded (`EMP-####`) HR records scoped to the owning
  venture - department, designation, joining date, salary, active/inactive
  status; search + status filter, full CRUD with audit trail
- Frontend: Employees master page (searchable, status badges, inline edit/delete)
### Phase 10 - Reports (complete)
- `reports` app: period sales/purchases (summary + top products/suppliers +
  day series), inventory stock/valuation + low-stock alerts, financials
  (revenue, COGS, expenses, margin, profit, receivables, payables, cash flow)
- All report endpoints are venture-scoped and gated by `reports.view`
- Frontend: Reports hub with 4 tabs, date-range filters, summary cards,
  lightweight CSS charts and low-stock alert panel
### Phase 11 - Audit & Notifications (complete)
- `notifications` app: per-user inbox; auto-fires LOW_STOCK, SALE_CREATED,
  PURCHASE_CREATED, PAYMENT_RECEIVED/PAID alerts to superusers and the relevant
  managers whenever stock/bills/payments are recorded
- `audit` read API: paginated, filterable audit trail behind `audit.view`
- Frontend: Notifications page (unread badge, mark read / mark all) and
  Audit Logs page (filters: module, action, date range, search + pagination)
- Backend suite green (129 tests)

Planned: Phase 12 (UI/UX) -> Phase 13 (Testing) -> ... per `docs/ARCHITECTURE.md`.

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