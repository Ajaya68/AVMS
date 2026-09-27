# AVMS Frontend

React 19 + Vite single-page application for the Ajaya Venture Management
System. Talks to the Spring Boot backend (`backend-java/`).

## Run

```bash
npm install                        # first time only
set VITE_API_URL=http://localhost:8081/api
npm run dev                        # http://localhost:5174
```

- `VITE_API_URL` points at the backend API (default `http://localhost:8000/api`).
- In dev, `/api/*` requests are proxied when no absolute URL is set.
- Other scripts: `npm run build` (production bundle into `dist/`), `npm run lint`.

## Notes

- Auth uses JWT access/refresh tokens stored in `localStorage`
  (`src/services/api.js`, `src/context/AuthContext.jsx`).
- The sidebar is permission-gated (`src/utils/navItems.js`); permissions come
  from `/api/auth/me/`.
- The `X-Venture-Id` header (top-bar venture selector) scopes data per venture.
- Lists refresh in place after every create/update/delete (no page reload).
