# AVMS — Module Mapping (Django → Spring Boot + React)

Status legend: `[ ]` Not started · `[~]` In progress · `[x]` Completed · `[!]` Needs investigation.
Nothing is marked `[x]` unless implemented AND tested in the new stack.

| # | Django app | Java package `com.avms.*` | Entities | Repositories | Services | Controllers | DTOs | Security | Oracle tables | React pages/components | Tests | Status |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 0 | core + config | `common`, `config` | `SequenceCounter`, `BaseEntity` | `SequenceCounterRepository` | `SequenceService`, `VentureScopeService`, `AuditService`, `NotificationService` | `HealthController` | `ApiResponse<T>`, `PageResponse<T>`, `ErrorResponse` | permitAll `/health`, `/api/health`, `/api/schema`, `/api/docs` | `CORE_SEQUENCE_COUNTER` | `systemService`, health badge, envelope unwrap, toasts | Health + envelope tests | `[~]` |
| 1 | accounts | `accounts` | `User`, `Role`, `Permission` (+join tables) | `UserRepository`, `RoleRepository`, `PermissionRepository` | `AuthService` (login/refresh/logout/me, password reset/change), `UserService`, `RoleService`, `SeedRolesService` | `AuthController`, `UserController`, `RoleController` | Login/Refresh/Me/UserCreate/UserUpdate/Role DTOs | JWT filter, `users.manage` for user/role admin, self-delete/superuser guards | `ACCOUNTS_USER`, `ACCOUNTS_PERMISSION`, `ACCOUNTS_ROLE` + M2M | Login/Forgot/Reset/Users/Roles/Settings, AuthContext, ProtectedRoute | Auth flow, RBAC, seed idempotency (forgot/reset-password pending) | `[~]` |
| 2 | ventures | `ventures` | `Venture`, `VentureCodeCounter` (compat) | `VentureRepository` | `VentureService` (code gen, search/status filter) | `VentureController` | `VentureRequest/Response` | `ventures.view/manage` per-method | `VENTURES_VENTURE` | VenturesPage, VentureDetailPage, VentureSelector/Context/Remount | CRUD + code + scoping | `[x]` |
| 3 | customers | `customers` | `Customer` | `CustomerRepository` | `CustomerService` (generic master pattern) | `CustomerController` | `CustomerRequest/Response` | `customers.view/manage` | `CUSTOMERS_CUSTOMER` | CustomersPage via MasterEntityPage | CRUD + per-venture code + cross-venture 404 | `[ ]` |
| 4 | suppliers | `suppliers` | `Supplier` | `SupplierRepository` | `SupplierService` | `SupplierController` | `SupplierRequest/Response` | `suppliers.view/manage` | `SUPPLIERS_SUPPLIER` | SuppliersPage | same as customers | `[ ]` |
| 5 | products | `products` | `Unit`, `Category`, `Product` | 3 repos | `ProductService`, `CategoryService`, `UnitService` | `ProductController`, `CategoryController`, `UnitController` | Request/Response ×3 | `products/categories/units.view/manage`; units unscoped | `PRODUCTS_UNIT|CATEGORY|PRODUCT` + seed units | Products/Categories/Units pages | CRUD + SKU uniqueness + PROTECT delete | `[ ]` |
| 6 | inventory | `inventory` | `Warehouse`, `Inventory`, `StockMovement` | 3 repos | `StockMovementService` (atomic, transfer pairing, low-stock alert) | `WarehouseController`, `InventoryController`, `StockMovementController` | Warehouse/Inventory/StockMovement DTOs | `warehouses/inventory/stock_movements.view/manage` | `INVENTORY_WAREHOUSE|INVENTORY|STOCKMOVEMENT` | Warehouses/Inventory/StockMovements pages | movement math, insufficient-stock, transfer pair, low-stock notify | `[ ]` |
| 7 | purchases | `purchases` | `Purchase`, `PurchaseItem`, `PurchaseReturn`, `PurchaseReturnItem` | 4 repos | `PurchaseService.finalizePurchase/finalizeReturn` | `PurchaseController`, `PurchaseReturnController` | Bill + line + return DTOs | `purchases/purchase_returns.view/manage` | `PURCHASES_*` (4 tables) | Purchases/PurchaseReturns/ReturnForm pages | totals, stock posting, over-return reject, delete-block | `[ ]` |
| 8 | sales | `sales` | `Sale`, `SaleItem`, `SaleReturn`, `SaleReturnItem` | 4 repos | `SaleService.finalizeSale/finalizeReturn` | `SaleController`, `SaleReturnController` | Bill + line + return DTOs | `sales/sales_returns.view/manage` | `SALES_*` (4 tables) | Sales/SaleReturns/ReturnForm pages | same as purchases + oversell reject | `[ ]` |
| 9 | payments | `payments` | `Payment` (logical ref) | `PaymentRepository` | `PaymentService.applyPayment/reversePayment` | `PaymentController` | `PaymentRequest/Response` | `payments.view/manage` | `PAYMENTS_PAYMENT` | PaymentsPage | cap-at-outstanding, type↔ref validation, reversal | `[ ]` |
| 10 | expenses | `expenses` | `ExpenseCategory`, `Expense` | 2 repos | `ExpenseService` | `ExpenseCategoryController`, `ExpenseController` | Expense DTOs | `expenses.view/manage` | `EXPENSES_CATEGORY|EXPENSE` + seed 8 | ExpensesPage | CRUD + category filter | `[ ]` |
| 11 | employees | `employees` | `Employee`, `Attendance` | 2 repos | `EmployeeService`, `AttendanceService` (+bulk upsert, self check-in/out) | `EmployeeController`, `AttendanceController` | Employee/Attendance DTOs | `employees.view/manage`, `attendance.view/manage` + self-scope | `EMPLOYEES_EMPLOYEE|ATTENDANCE` | Employees/Attendance/MyProfileCard pages | CRUD + bulk + self-service + unique per day | `[ ]` |
| 12 | reports | `reports` | none (queries over sales/purchases/inventory/expenses/payments) | query via existing repos | `ReportService` (sales/purchases/inventory/financial) | `ReportController` | Report response DTOs | `reports.view` | none (computed) | ReportsPage (4 tabs), Dashboard KPIs | aggregate correctness vs Django | `[ ]` |
| 13 | notifications | `notifications` | `Notification` | `NotificationRepository` | `NotificationService.notify/notifyVentureUsers` | `NotificationController` | `NotificationResponse` | `notifications.view`, owner-scoped | `NOTIFICATIONS_NOTIFICATION` | NotificationsPage, Topbar bell | fan-out service + read API done; triggers pending | `[~]` |
| 14 | audit | `audit` | `AuditLog` | `AuditLogRepository` | `AuditService.log` | `AuditController` (GET only) | `AuditLogResponse` | `audit.view` | `AUDIT_AUDITLOG` | AuditLogsPage, Dashboard activity feed | service + read API + hooks in accounts/ventures done | `[~]` |
| 15 | regression | `regression` (tests only) | — | — | — | — | — | — | — | — | 5 E2E scenarios ported to Spring integration tests | `[ ]` |

## Migration order (dependency-safe)

1. `common/config` (envelope, exceptions, JWT skeleton, sequence, venture scope, health, OpenAPI).
2. `accounts` (users/roles/permissions/seed) + `ventures`.
3. Masters: `customers`, `suppliers`, `products` (incl. unit/category seeds).
4. `inventory` (warehouses, ledger, movements).
5. `purchases` → `sales` (symmetric; purchases first as reference implementation).
6. `payments` → `expenses` → `employees`.
7. `reports` → `notifications` → `audit` wiring (audit must be available from step 1 as a service, read API last).
8. Frontend re-point + regression/E2E + hardening.

Every step: entity → repository → DTO/validation → service → controller → security → audit/notify hooks → tests → checklist update.
