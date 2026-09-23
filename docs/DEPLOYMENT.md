# AVMS Deployment

Target deployment:
- **Backend (Django + MySQL)**: Railway
- **Frontend (React)**: Vercel

This document is completed in **Phase 14 (Production)**. The notes below are
the early checkpoints already baked into the foundation.

## Environment Variables (Production)

| Variable                  | Production value                           |
| ------------------------- | ------------------------------------------ |
| `DEBUG`                   | `False`                                    |
| `SECRET_KEY`              | long random value (never committed)        |
| `DJANGO_ALLOWED_HOSTS`    | your domain(s)                             |
| `CORS_ALLOWED_ORIGINS`    | your Vercel/Vite app origin                |
| `CSRF_TRUSTED_ORIGINS`    | your frontend origin                       |
| `DB_NAME` / `DB_USER`     | managed MySQL values                       |
| `DB_PASSWORD`             | managed, injected as env                   |
| `DB_HOST` / `DB_PORT`     | managed MySQL endpoint                     |
| `JWT_ACCESS_MINUTES`      | `30`                                       |
| `JWT_REFRESH_DAYS`        | `7`                                        |

**`.env` files are never committed.** Only `.env.example` templates live in the
repository.

## Backend Hardening Checklist (Phase 14)

- [ ] `DEBUG=False`, valid `SECRET_KEY`
- [ ] `DJANGO_ALLOWED_HOSTS` restricted
- [ ] CORS origins restricted to the deployed frontend
- [ ] HTTPS everywhere (terminate TLS at the platform edge)
- [ ] Migrations applied (`python manage.py migrate`)
- [ ] Static assets collected (`python manage.py collectstatic`)
- [ ] Run Django production-grade server (gunicorn/uWSGI) behind a reverse proxy
- [ ] Sensitive settings vaulted / encrypted in CI

## Frontend Build

```bash
cd frontend
npm ci
npm run build        # outputs dist/
# Preview what Vercel will serve
npm run preview
```

Set `VITE_API_URL` to the deployed backend for the production build:

```env
VITE_API_URL=https://your-production-api-domain/api
```

## Backend Quick Reference

```bash
cd backend
python -m venv venv
source venv/bin/activate
pip install -r requirements.txt
python manage.py collectstatic
python manage.py migrate
python manage.py runserver 0.0.0.0:8000   # dev only; use a WSGI server in prod
```

## Database

- Railway managed MySQL: create database `avms`, use its connection values for
  `DB_*`, unicode collation `utf8mb4`.
- Apply schema exclusively via `python manage.py migrate`. Never edit tables
  manually in production.

## Smoke Test After Deploy

```bash
curl https://your-api-domain/api/health/
# expect: {"success":true,"data":{"status":"ok",...,"database":"ok"},...}
```