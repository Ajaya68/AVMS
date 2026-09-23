# AVMS API

## Summary

- Base path: `/api/`
- Format: JSON
- Auth: JWT Bearer tokens (`Authorization: Bearer <access>`)
- Response envelope (all endpoints):
  ```json
  { "success": true,  "data": {}, "message": "Operation successful" }
  { "success": false, "message": "Unable to complete operation", "errors": {} }
  ```

Interactive documentation (Swagger UI):
- http://localhost:8000/api/docs/
- OpenAPI schema: http://localhost:8000/api/schema/

## Endpoints Implemented (Phase 1)

### Health

| Method | Path          | Auth | Description                              |
| ------ | ------------- | ---- | ---------------------------------------- |
| GET    | `/health/`    | No   | Service + DB status (alias)              |
| GET    | `/api/health/`| No   | Service + DB status                      |

Response:
```json
{
  "success": true,
  "data": {
    "status": "ok",
    "service": "avms-backend",
    "version": "0.1.0",
    "database": "ok"
  },
  "message": "Service healthy"
}
```

If the database is unreachable the HTTP code becomes `503` with
`"status": "degraded"` and `"database": "unavailable"`; the service itself is
still running.

### Documentation

| Method | Path           | Auth | Description          |
| ------ | -------------- | ---- | -------------------- |
| GET    | `/api/schema/` | No   | OpenAPI 3 schema     |
| GET    | `/api/docs/`   | No   | Swagger UI           |

### Django Admin

| Path     | Description                         |
| -------- | ----------------------------------- |
| `/admin/`| Django admin (superuser required)   |

## Endpoints Implemented (Phase 2)

### Authentication

| Method | Path                  | Auth | Description                                   |
| ------ | --------------------- | ---- | --------------------------------------------- |
| POST   | `/api/auth/login/`    | No   | `{email, password}` -> `{access, refresh, user}` |
| POST   | `/api/auth/refresh/`  | No   | `{refresh}` -> rotated `{access, refresh}`    |
| POST   | `/api/auth/logout/`   | Yes  | `{refresh}`; blacklists the refresh token     |
| GET    | `/api/auth/me/`       | Yes  | Current user profile, roles and capabilities  |

`login` response `user` includes `role_codes` (active role codes), `roles`
(detail incl. `permission_codes`) and `permissions` (all granted capability
codes). Bad credentials return `401`.

### Users & Roles (ADMIN only)

| Method | Path                    | Permission     | Description                              |
| ------ | ----------------------- | -------------- | ---------------------------------------- |
| GET    | `/api/auth/users/`      | users.view     | Paginated list, `?search=` by name/email |
| POST   | `/api/auth/users/`      | users.manage   | Create user (`email`, `password`, `full_name`, `phone`, `roles[]`) |
| GET    | `/api/auth/users/{id}/` | users.manage   | User detail                              |
| PATCH  | `/api/auth/users/{id}/` | users.manage   | Update `full_name`, `phone`, `is_active`, `roles[]` |
| GET    | `/api/auth/roles/`      | roles.view     | All active roles with `permission_codes` |

## Roles & Permissions

Roles are seeded by `python manage.py seed_roles` (idempotent).

| Role           | Summary                                                 |
| -------------- | ------------------------------------------------------- |
| ADMIN          | Everything, incl. users, roles, audit, settings         |
| MANAGER        | Operations: customers, suppliers, products, inventory, sales, purchases, reports |
| ACCOUNTANT     | Payments, expenses, sales/purchases view, reports       |
| SALES_STAFF    | Customers and sales (create), payments view             |
| INVENTORY_STAFF| Products, categories, warehouses, inventory, stock      |
| EMPLOYEE       | Dashboard only                                          |

Permission codes follow `module.view` (read) / `module.manage` (write).
Superusers bypass all permission checks. The backend always enforces
authorization; the frontend only hides what the user cannot do.

## Endpoints Planned by Phase

### Phase 3 - Ventures
- `GET /api/ventures/` — list (search by name/code/city via `?search=`), `POST` create.
- `GET /api/ventures/{id}/`, `PUT/PATCH/DELETE /api/ventures/{id}/`.
- Auth: `ventures.view` (read), `ventures.manage` (write). Code auto-generated (`V-0001`…) via serialized counter; read-only.
- Fields: venture_name, business_type (MUSHROOM | FISH_FARMING | AGRICULTURE | POULTRY | DAIRY | GENERAL | OTHER), status (ACTIVE | INACTIVE), phone, email, address, city, state, pincode, description.
- Every create/update/delete writes an `AuditLog` (action CREATE/UPDATE/DELETE, entity_type `Venture`).

### Phase 4 - Masters (complete)
- `customers`: `GET/POST /api/customers/`, `GET/PATCH/DELETE /api/customers/{id}/`
  — auto `C-####` per-venture codes, search by name/code/city/phone/email.
- `suppliers`: `GET/POST /api/suppliers/`, `GET/PATCH/DELETE /api/suppliers/{id}/`
  — auto `S-####` per-venture codes.
- `products`: `GET/POST /api/products/`, `/api/products/{id}/` — auto `P-####`
  SKU (unique per venture), category + unit lookups.
- `categories`: `GET/POST /api/categories/`, `/api/categories/{id}/`.
- `units`: `GET/POST /api/units/` (kg, g, pcs, packet, litre, box seeded via data
  migration).
- Auth: `{module}.view` (reads) / `{module}.manage` (writes) for customers,
  suppliers, products, categories, units. All reads honour the `X-Venture-Id`
  header (unscoped = all ventures). Every create/update/delete writes an
  `AuditLog`.
- All master endpoints are driven by the shared `core.crud_views`
  (`MasterListCreateView`/`MasterDetailView`) and the `core.SequenceCounter`
  code generator (`generate_code(module, venture, prefix)`).

### Phase 5 - Inventory
- `GET/POST /api/warehouses/`, `/api/warehouses/{id}/`
- `GET /api/inventory/`
- `GET/POST /api/stock-movements/`

### Phase 6 - Purchases
- `GET/POST /api/purchases/`, `/api/purchases/{id}/`
- `GET /api/purchases/{id}/items/`
- `GET/POST /api/purchase-returns/`

### Phase 7 - Sales
- `GET/POST /api/sales/`, `/api/sales/{id}/`
- `GET /api/sales/{id}/items/`
- `GET/POST /api/sales-returns/`

### Phase 8 - Finance
- `GET/POST /api/payments/`
- `GET/POST /api/expenses/`

### Phase 9 - Employees
- `GET/POST /api/employees/`, `/api/employees/{id}/`

### Phase 10 - Reports
- `GET /api/reports/sales/`
- `GET /api/reports/purchases/`
- `GET /api/reports/inventory/`
- `GET /api/reports/financial/`

## HTTP Status Codes

| Code | Meaning                                          |
| ---- | ------------------------------------------------ |
| 200  | Success                                          |
| 201  | Created                                          |
| 204  | Deleted                                          |
| 400  | Validation / bad request                         |
| 401  | Unauthenticated / expired token                  |
| 403  | Authenticated but forbidden (role)               |
| 404  | Not found                                        |
| 422  | Unprocessable business-rule violation            |
| 503  | Service degraded (DB down)                       |

## Pagination & Filtering

List endpoints support backend pagination:

```text
GET /api/products/?page=1&page_size=20&search=mushroom
```

- `page` (1-based), `page_size` (default 20)
- `search` for keyword search, plus module-specific `filter_<field>` params