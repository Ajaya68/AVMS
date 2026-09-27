# AVMS — API Mapping (Django URL → Spring Boot endpoint)

Conventions preserved: base `/api`, envelope `{success,data,message}` / `{success,message,errors}`, pagination `{count,next,previous,results}` (Spring: map `Page<T>` to same shape), venture scope via `X-Venture-Id`, JWT `Authorization: Bearer`.

| Django (source) | Spring Boot (target) | Method/permission | Notes / behavior deltas |
|---|---|---|---|
| `POST /api/auth/login/` | `POST /api/auth/login/` | public | Same `{access,refresh,user}`. Spring: JJWT signed HS256, `last_login` update. |
| `POST /api/auth/refresh/` | `POST /api/auth/refresh/` | public | Rotation + blacklist (persist refresh jti; Django uses `token_blacklist` table → Spring `REFRESH_TOKEN` table or allowlist). |
| `POST /api/auth/logout/` | `POST /api/auth/logout/` | authenticated | Blacklist refresh. Same `{refresh}` body. |
| `GET /api/auth/me/` | `GET /api/auth/me/` | authenticated | Same user+`permissions[]`+`role_codes[]`. |
| `POST /api/auth/forgot-password/` | `POST /api/auth/forgot-password/` | public | Non-enumerating 200; `reset_url` only when `DEBUG`/dev profile. |
| `POST /api/auth/reset-password/` | `POST /api/auth/reset-password/` | public | `uidb64,token,new_password` (min 8, validator). Token = PasswordResetToken equivalent (implement HMAC time-bound token). |
| `POST /api/auth/change-password/` | `POST /api/auth/change-password/` | authenticated | `current_password,new_password` check. |
| `GET/POST /api/auth/users/` | `GET/POST /api/auth/users/` | `users.manage` | `?search=` email/full_name. Same self/superuser delete guards. |
| `GET/PATCH/DELETE /api/auth/users/<pk>/` | `GET/PATCH/DELETE /api/auth/users/{id}` | `users.manage` | — |
| `GET /api/auth/roles/` | `GET /api/auth/roles/` | `users.manage` | Active only. |
| `GET/POST /api/ventures/` `GET/PATCH/DELETE /api/ventures/<pk>/` | `GET/POST /api/ventures/` `GET/PATCH/DELETE /api/ventures/{id}` | `ventures.view` GET, `ventures.manage` writes; `?search=&status=` | Auto `V-####`. |
| `GET/POST /api/customers/` + detail | `GET/POST /api/customers/` + `/{id}` | `customers.view/manage`; `?search=&status=` | Auto `C-####` per venture; cross-venture detail → 404. |
| `GET/POST /api/suppliers/` + detail | same shape `/api/suppliers/` | `suppliers.view/manage` | Auto `S-####`. |
| `GET/POST /api/products\|categories\|units/` + detail | same paths | `products/categories/units.view/manage`; units unscoped | Auto `P-####`; `unit` delete PROTECT → 400/409 if referenced. |
| `GET/POST /api/warehouses/` + detail | same | `warehouses.view/manage` | Auto `W-####`. |
| `GET /api/inventory/?warehouse=&product=&low=` | same | `inventory.view` | `low=true` → `quantity<=reserved+reorder`. |
| `GET/POST /api/stock-movements/` `GET /api/stock-movements/<pk>/` | same | `stock_movements.view/manage`; no PATCH/DELETE | Movement date, transfer `destination_warehouse`. Errors: 400 validation, 422 insufficient stock. |
| `GET/POST /api/sales/?search=&status=` `GET/PATCH/DELETE /api/sales/<pk>/` `GET /api/sales/<pk>/items/` | same paths (`{id}`) | `sales.view/manage` | Nested `items[]` writable on POST; totals server-computed; DELETE 400 if dispatched; PATCH metadata-only. |
| `GET/POST /api/sales-returns/` `GET /api/sales-returns/<pk>/` | same | `sales_returns.view/manage` | Cumulative over-return → 400. |
| `GET/POST /api/purchases/` + detail + items | same | `purchases.view/manage` | Mirror of sales (`PINV-####`). |
| `GET/POST /api/purchase-returns/` + detail | same | `purchase_returns.view/manage` | `RET-####`. |
| `GET/POST /api/payments/?payment_type=&reference_type=&search=` `GET/DELETE /api/payments/<pk>/` | same | `payments.view/manage`; no PATCH | Type↔ref validation; over-payment 400; DELETE reverses then deletes. |
| `GET /api/expense-categories/` | same | `expenses.view` (unpaginated list) | Seeded 8. |
| `GET/POST /api/expenses/?search=&category=` + detail | same | `expenses.view/manage` | — |
| `GET/POST /api/employees/?search=&venture=&department=&designation=&status=` + detail | same | `employees.view/manage` | Auto `EMP-####`; phone regex, salary≥0. |
| `GET /api/employees/me/` | same | authenticated (self) | `{linked,employee,today,month_summary}`. |
| `GET/POST /api/attendance/?venture=&employee=&department=&status=&from=&to=` `PATCH/DELETE /api/attendance/<pk>/` `POST /api/attendance/bulk/` | same | authenticated + `attendance.view/manage` inline + self-scope | Upsert per (employee,date); `check_in/out:"now"` supported. |
| `GET /api/reports/sales\|purchases\|inventory\|financial/?from=&to=` | same | `reports.view` | Default 30d window; excludes CANCELLED. |
| `GET /api/notifications/?unread_first=` `GET /api/notifications/unread-count/` `POST /api/notifications/<pk>/read/` `POST /api/notifications/read-all/` | same | `notifications.view`, owner-scoped | Cross-user read → 404. |
| `GET /api/audit-logs/?module=&action=&object_id=&from=&to=&search=` | same | `audit.view` | GET only. |
| `GET /health/`, `/api/health/` | same | permitAll | 200 ok / 503 degraded with DB probe. |
| `GET /api/schema/`, `/api/docs/` | same (springdoc `/v3/api-docs`, `/swagger-ui.html` aliased) | permitAll | Keep legacy aliases if cheap. |

Status codes preserved: 200/201/204, 400 validation (`errors`), 401, 403 (`You do not have permission…`), 404 (`{X} not found`, incl. cross-venture), 422 business-rule (insufficient stock/over-return/over-pay), 503 degraded health.
Frontend delta: only `VITE_API_URL` base changes (port 8080); `X-Venture-Id`, envelope unwrap, and refresh flow unchanged.
