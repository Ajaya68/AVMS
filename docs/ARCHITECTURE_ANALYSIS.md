# AVMS — Architecture Analysis (Source of Truth: Python/Django + React)

Date: 2026-09-26. Source inspected: `backend/` (15 Django apps), `frontend/src` (60 files), `docs/*.md`, migrations.

## 1. System overview

- React 19 SPA (Vite, Bootstrap 5, React Router 7, Axios) ↔ Django 5.2 + DRF REST API ↔ MySQL 8 (`avms`).
- Auth: email-based custom `User` (`accounts.User`), JWT via SimpleJWT (access 30m / refresh 7d, rotation + blacklist), `AUTH_USER_MODEL=accounts.User`.
- Multi-venture platform: `ventures.Venture` is the tenant root. Almost every business table carries `venture FK CASCADE`. Request scoping via `X-Venture-Id` header (`ventures/services.py:get_request_venture`, `scope_queryset_by_venture`); detail endpoints scope by venture so cross-venture ID guessing returns 404.
- Envelope: `core/responses.py` → success `{success:true,data,message}` / failure `{success:false,message,errors}`. Frontend unwraps `data`.
- Pagination: DRF PageNumber, `PAGE_SIZE=20`; list responses are paginated page dicts inside envelope.
- Code generation: `core.SequenceCounter(module,venture)` + `SELECT FOR UPDATE` → prefixes `V,C,S,P,W,PINV,RET,SINV,SRET,EMP` formatted `PREFIX-0001`, never reused. Legacy `VentureCodeCounter` single-row for `V-####`.
- Thin views, business rules in per-app `services.py`, explicit audit + notification calls (no Django signals anywhere).
- No caching, no background/scheduled jobs, no file uploads, no email/SMS (forgot-password returns `reset_url` only when `DEBUG=True`).
- Health: `GET /health/`, `/api/health/` (DB probe, 200 ok / 503 degraded). OpenAPI `/api/schema/`, Swagger `/api/docs/`.
- Production hardening (Phase 14): `CONN_MAX_AGE` pooling, HTTPS proxy header + secure-cookie/HSTS flags when `DEBUG=False`, structured console logging, gunicorn `workers=3`.
- Tests: 134 backend tests (`manage.py test`), lint clean, build OK.

## 2. Backend modules (15 apps + config + regression)

| App | Responsibility | Key models | Endpoints (mount) |
|---|---|---|---|
| accounts | Identity, RBAC, JWT, password reset | User, Role, Permission | `api/auth/` login/refresh/logout/me/users/roles/forgot/reset/change |
| ventures | Tenant root | Venture, VentureCodeCounter | `api/ventures/` CRUD + search/status |
| customers | Master | Customer (C-####, per-venture unique) | `api/customers/` CRUD via `core.crud_views` |
| suppliers | Master | Supplier (S-####) | `api/suppliers/` CRUD |
| products | Masters | Unit (global), Category, Product (P-####, SKU/venture unique, unit PROTECT) | `api/products|categories|units` CRUD |
| inventory | Warehouses + stock ledger | Warehouse (W-####), Inventory (wh×product unique), StockMovement (8 types, transfer pairing) | `api/warehouses`, `api/inventory`, `api/stock-movements` |
| purchases | Purchase bills + returns | Purchase (PINV-####), PurchaseItem, PurchaseReturn (RET-####), PurchaseReturnItem | `api/purchases`, `api/purchase-returns`, `…/items/` |
| sales | Sales bills + returns | Sale (SINV-####), SaleItem, SaleReturn (SRET-####), SaleReturnItem | `api/sales`, `api/sales-returns`, `…/items/` |
| payments | Settle dues | Payment (RECEIVED/PAID, logical ref SALE/PURCHASE) | `api/payments/` (GET/POST/DELETE only) |
| expenses | Operating costs | ExpenseCategory (seeded 8), Expense | `api/expense-categories`, `api/expenses/` |
| employees | HR + attendance | Employee (EMP-####, optional 1-1 User), Attendance (employee×date unique) | `api/employees/`, `api/employees/me/`, `api/attendance/`, `api/attendance/bulk/` |
| reports | Read-only aggregates | none (queries) | `api/reports/sales|purchases|inventory|financial/` |
| notifications | Per-user inbox | Notification (6 types) | `api/notifications/` + unread-count/read/read-all |
| audit | Immutable trail | AuditLog (13 actions) | `api/audit-logs/` (GET only) |
| core | Shared infra | TimeStampedModel, SequenceCounter, SoftDelete (unused), HealthView, envelope, Master CRUD base | `/health/`, `/api/health/` |
| config | Settings/urls/wsgi | — | mounts all routers, JWT/DRF/CORS/Logging config |
| regression | E2E guards | none | 5 scenarios: low-stock crash, sale-notify audience, notification ownership, cross-venture 404, purchase→pay→overpay-reject |

## 3. Key business rules (must preserve verbatim)

1. All money math server-side: `Sale/Purchase.recompute()` (`subtotal=Σqty*price`, `total=subtotal-discount+tax`, `due=total-paid-returned clamp0`); item `total=(qty*price-discount)*(1+tax/100)`.
2. Stock is transactional: `record_stock_movement` uses `transaction.atomic + select_for_update`, lazy-creates inventory row, `TRANSFER_OUT` decrements source + increments dest + paired `TRANSFER_IN` linked both ways; outgoing types (`SALE,PURCHASE_RETURN,ADJUSTMENT_OUT,TRANSFER_OUT`) raise `InsufficientStockError` → 400/422 if insufficient.
3. `finalize_sale` rejects any line exceeding available stock; `finalize_purchase` posts `PURCHASE` movements; returns validate cumulative returned ≤ original sold/purchased qty; over-return/over-payment rejected with descriptive 400.
4. Delete of a stock-received purchase or stock-dispatched sale blocked (reverse via return instead).
5. Payments: `RECEIVED` must ref SALE, `PAID` must ref PURCHASE; amount capped at outstanding `total-paid-returned`; `DELETE` reverses (`paid-=amount clamp0`) then deletes.
6. Venture consistency validated at serializer level (customer/warehouse/product must belong to bill's venture).
7. Low-stock alert: after any movement, if `available<=reorder` → `notify_venture_users(perm inventory.manage)`; sale/purchase/payment creations fan out to superusers + holders of `sales.manage/purchases.manage/payments.manage`.
8. Every create/update/delete + auth events write `AuditLog` with user + IP (`X-Forwarded-For` aware).
9. RBAC: `{module}.view` read / `{module}.manage` write; superuser bypasses; attendance has self-service scoping (own records without `attendance.view`).
10. Financial reports: revenue net of returns, COGS `Σqty*purchase_price`, `gross=revenue-cogs`, `net=gross-expenses`, receivables `Σsale.due`, payables `Σpurchase.due`, cash `received-paid`.

## 4. Permission matrix (from `seed_roles.py`)

~44 permissions (`{module}.view/manage` × ~22 modules incl. dashboard, users, roles, audit, settings, attendance, sales_returns, purchase_returns, warehouses, stock_movements, expense-categories implied under expenses).
ADMIN=all; MANAGER=ops full + payments/expenses view + reports; ACCOUNTANT=sales/purchases view + payments/expenses manage + reports; SALES_STAFF=customers+sales(+returns) manage + payments view; INVENTORY_STAFF=products/warehouses/inventory/stock manage + categories/units view; EMPLOYEE=dashboard.view only.

## 5. Frontend architecture

- `App.jsx`: `AuthProvider > ToastProvider > VentureProvider > RouterProvider`.
- `services/api.js`: Axios `baseURL=VITE_API_URL||http://localhost:8000/api`, injects `Bearer` + `X-Venture-Id`, single-flight 401 refresh (`POST /auth/refresh/`), unwraps envelope, global success/error toasts via `avms:toast`.
- `AuthContext`: `fetchMe()` hydration, `hasPerm()` (superuser bypass), login/logout, `ProtectedRoute` guard.
- `VentureContext + VentureSelector + VentureRemount`: Topbar venture dropdown (`GET /ventures/?page_size=100`), persists `active_venture_id`, remounts outlet on change.
- `MasterEntityPage`: generic CRUD shell (search, table/card, modal form, perm-gated actions) used by Customers/Suppliers/Products/Categories/Units/Warehouses/Employees.
- 28 routes (3 public auth, 25 protected). Sales/Purchases pages mirrored (622 lines each): search+status filter, detail modal with balances, dynamic-line create form, delete-blocked-if-stocked, return-from-detail. Reports hub (4 tabs, date filters, CSS charts). Dashboard KPIs (net sales/purchases 30d, receivables, stock value) + low-stock panel + activity feed. Notifications inbox + bell (30s poll). Audit log page (filters + pagination). Settings (profile + change-password). Attendance roster (bulk upsert).
- `PlaceholderPage.jsx` is dead code (no route references it).

## 6. Database inventory

~19 migrations (15 schema + 2 data seeds + 2 constraint alters). Tables: `accounts_user(+m2m groups/permissions/roles)`, `accounts_permission`, `accounts_role(+m2m)`, `ventures_venture`, `ventures_venturecodecounter`, `core_sequencecounter`, `customers_customer`, `suppliers_supplier`, `products_unit|category|product`, `inventory_warehouse|inventory|stockmovement`, `purchases_purchase|items|returns|return_items`, `sales_sale|items|salreturn|return_items`, `payments_payment`, `expenses_expensecategory|expense`, `employees_employee|attendance`, `audit_auditlog`, `notifications_notification`. No stored procedures/functions. All FKs use explicit `CASCADE/PROTECT/SET_NULL` per §2 relationships; per-venture uniques on codes/skus/invoices; `Inventory(warehouse,product)`, `Attendance(employee,date)` uniques; indexes on `Venture.status`, `Permission.module`, `AuditLog.module/created_at`, `Attendance.date`.

## 7. Risks / constraints for migration

- Cross-venture 404 scoping must be replicated on every detail endpoint (regression-guarded).
- Exact money/stock transactional semantics (row locks, cumulative guards) must be preserved — highest regression risk.
- Payment reference is logical `(type,id)`, not DB FK — needs application-level integrity in JPA.
- `reset_url` DEBUG-only behavior, single-flight refresh, envelope contract are frontend-coupled — keep compatible or update frontend in lockstep.
- No async jobs today; do not introduce queues unless justified.
