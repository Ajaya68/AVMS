# AVMS Architecture

## Overview

AVMS is separated into two independently deployable applications:

```text
React SPA (frontend/port 5173)
      │  browser calls /api/*   (dev: proxied by Vite; prod: CORS + HTTPS)
      ▼
Django REST Framework (backend/port 8000)
      │  views / viewsets -> serializers               (thin)
      │  services (business logic)                     (intended home of rules)
      │  permissions (role-based authorization)        (enforced server-side)
      ▼
Django ORM -> MySQL 8 (database `avms`)
```

The backend enforces all business rules and authorization. The frontend is a
consumer of the API only; it never trusts its own UI state for security.

## Backend Layout

Every feature lives in its own Django app. Apps follow the same internal
convention so behavior is predictable:

| Module          | Where logic lives                              |
| --------------- | ---------------------------------------------- |
| `config/`       | settings, root URLs, WSGI/ASGI, PyMySQL hook   |
| `core/`         | base models, shared managers, responses, health|
| `<feature>/`    | models, serializers, views/viewsets, services, permissions, urls, admin |

### Request flow

1. DRF authenticates the JWT (`JWTAuthentication`).
2. Permission classes run (role checks) - unauthorized requests stop here.
3. View(sets) perform minimal orchestration.
4. `service` objects contain business rules (totals, stock movement, balances).
5. Serializers validate input and shape output.
6. The ORM persists to MySQL.

## The Custom User Model

The `accounts.User` model is email-based (`USERNAME_FIELD = "email"`,
`AbstractBaseUser` + `PermissionsMixin`). It was introduced in Phase 1 *before*
the first migration so the schema is correct from the start - swapping
`AUTH_USER_MODEL` after migrations exist is destructive and avoided entirely.

Roles and permissions are layered on in Phase 2 (models + permission classes),
never as a frontend-only behavior.

## Multi-Venture Data Isolation

Every business-facing model carries a `venture` foreign key. Scoping is applied
in the querysets/views; a selected venture is expected to be part of the request
context (either from an authed user's default venture or an explicit header) in
later phases. The design deliberately keeps `ventures` independent so new
enterprise units can be added without schema redesign.

## Frontend Layout

```text
src/
├── main.jsx                 # bootstrap CSS + icons, mounts <App/>
├── App.jsx                  # AuthProvider + RouterProvider
├── assets/styles/theme.css  # AVMS theme (Bootstrap vars + layout)
├── components/              # reusable UI (Sidebar, Topbar, LoadingSpinner, ...)
├── layouts/MainLayout.jsx   # sidebar + topbar + content (<Outlet/>)
├── routes/AppRoutes.jsx     # route table (createBrowserRouter)
├── services/                # centralized Axios + per-module API services
├── context/                 # AuthContext (session state)
├── hooks/useApi.js          # loading/data/error wrapper for async calls
└── utils/                   # navItems, feature flags
```

### API envelope

The Axios instance unwraps the backend's uniform response envelope so
components receive `data` directly:

```json
{ "success": true,  "data": {...},  "message": "..." }
{ "success": false, "message": "...", "errors": {...} }
```

### Feature flags

Client-only concerns (e.g. authentication gating) are exposed through
feature flags (`AUTH_ENABLED` in `ProtectedRoute`) so foundation work can be
verified end-to-end before a phase ships.

## Development Order (Phases)

1. **Phase 1 - Foundation** (complete): Django + React scaffolds, MySQL, env, health.
2. **Phase 2 - Authentication** (complete): JWT login/logout, roles, permissions, protected routes.
3. **Phase 3 - Venture** (complete): venture CRUD, auto `V-####` codes, audit-trailed, staff 403.
4. **Phase 4 - Masters** (complete): customers, suppliers, categories, units, products;
   shared `core.crud_views` + `core.SequenceCounter` code generator; `X-Venture-Id` scoping.
5. **Phase 5 - Inventory** (current): warehouses, stock, movements, low-stock alerts.
6. **Phase 6 - Purchases**: purchases, purchase items, returns, balances.
7. **Phase 7 - Sales**: sales, sale items, returns, balances, stock integration.
8. **Phase 8 - Finance**: payments, expenses, receivables, payables.
9. **Phase 9 - Employees**: basic employee management.
10. **Phase 10 - Reports**: sales/purchase/inventory/financial reports + charts.
11. **Phase 11 - Audit & Notifications**: audit log, notifications.
12. **Phase 12 - UI/UX**: responsive polish, empty/error states, toasts.
13. **Phase 13 - Testing**: end-to-end and regression fixes.
14. **Phase 14 - Production**: hardening + deployment.

## Rules of Engagement

- Business logic belongs in services, not views.
- Authorization is enforced in the backend, always.
- Every stock-affecting transaction writes a `stock_movement`.
- No module ships with a UI unless it has a backend.
- New phases keep every previously completed phase green.