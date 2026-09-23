# AVMS API

## Summary

- Base path: `/api/`
- Format: JSON
- Auth: JWT Bearer tokens (`Authorization: Bearer <access>`)
  - Implemented in Phase 2. Until then endpoints with no auth listed are public.
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

## Endpoints Planned by Phase

### Phase 2 - Authentication
- `POST /api/auth/login/`
- `POST /api/auth/refresh/`
- `POST /api/auth/logout/`
- `GET  /api/auth/me/`

### Phase 3 - Ventures
- `GET/POST /api/ventures/`
- `GET/PUT/PATCH/DELETE /api/ventures/{id}/`

### Phase 4 - Masters
- `GET/POST /api/customers/`, `/api/customers/{id}/`
- `GET/POST /api/suppliers/`, `/api/suppliers/{id}/`
- `GET/POST /api/categories/`, `/api/categories/{id}/`
- `GET/POST /api/units/`, `/api/units/{id}/`
- `GET/POST /api/products/`, `/api/products/{id}/`

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