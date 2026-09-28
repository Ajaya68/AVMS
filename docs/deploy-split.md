# Option B — Live on internet: Oracle Autonomous DB + Render + Vercel

This is the split-hosting path. ~30–45 min if you have Oracle Cloud + GitHub accounts.

## 1. Database — Oracle Cloud Free Autonomous DB

1. Sign up at cloud.oracle.com (Free Tier, no card charge).
2. Create Autonomous Database:
   - Workload: Transaction Processing
   - Deployment: Serverless
   - Name: `avmsdb`, 1 OCPU, 20 GB (free limits)
   - Admin password: save it
   - Access: **Secure access from everywhere**
   - **UNCHECK "Require mutual TLS (mTLS)"** — this lets Spring connect with a plain JDBC URL, no wallet files needed on Render.
3. After provisioning → DB Connection → TLS (not mTLS) → copy the `medium` connection string. It looks like:
   `jdbc:oracle:thin:@(description=...)(service_name=..._medium)...)`
   Oracle also shows a short form host:port/service. Either works as `DB_URL`.
4. SQL Developer / worksheet as ADMIN, create app user:
   ```sql
   CREATE USER avms IDENTIFIED BY "YourStrongPass123!";
   GRANT CONNECT, RESOURCE TO avms;
   ALTER USER avms QUOTA UNLIMITED ON DATA;
   ```
   Flyway will create all tables + seed on first backend boot.

## 2. Backend — Render (Docker)

1. Go to dashboard.render.com → New → Blueprint → select `Ajaya68/AVMS` repo. It reads `render.yaml`.
   Or New → Web Service → Docker, root `backend-java/`, Dockerfile `./backend-java/Dockerfile`.
2. Set env vars (Dashboard → Environment):
   | Key | Value |
   |-----|-------|
   | `SPRING_PROFILES_ACTIVE` | `prod` |
   | `DB_URL` | JDBC string from step 1 |
   | `DB_USER` | `avms` |
   | `DB_PASSWORD` | password from step 1 |
   | `JWT_SECRET` | 32+ random chars (Render Generate) |
   | `CORS_ORIGINS` | `https://<your-vercel-app>.vercel.app` (fill after step 3, then redeploy) |
   | `LOG_LEVEL` | `INFO` |
3. Deploy. Health check path is `/api/health/`. First boot runs Flyway V1–V7 (1–2 min).
   Verify: `https://avms-backend-xxxx.onrender.com/api/health/` → `{"success":true,"data":{"status":"ok"...}}`
4. Note your backend URL, e.g. `https://avms-backend-xxxx.onrender.com`.

Render free sleeps after 15 min idle — first request takes ~50s to wake. Paid Starter ($7/mo) avoids this.

## 3. Frontend — Vercel

1. Go to vercel.com → Add New Project → import `Ajaya68/AVMS`.
   - Root Directory: `frontend`
   - Framework: Vite (auto), Build: `npm run build`, Output: `dist`
   - `frontend/vercel.json` already handles SPA rewrites (`/login`, `/dashboard` → `index.html`).
2. Environment variable:
   - `VITE_API_URL` = `https://avms-backend-xxxx.onrender.com/api`
3. Deploy. You get `https://avms-xxx.vercel.app`.
4. Go back to Render → update `CORS_ORIGINS` to that Vercel URL → Manual Deploy → Clear build cache → Deploy.

## 4. Verify live

- `https://<vercel>.vercel.app/login` → login works, no CORS error in DevTools.
- `https://<render>.onrender.com/api/docs.html` → Swagger UI.
- Create a venture → product → purchase → sale to confirm DB writes.

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| Render `ORA-12514 / ORA-01017` | Wrong PDB/service in `DB_URL` or wrong `DB_USER`/`DB_PASSWORD` |
| Render `mTLS / wallet / ORA-29024` | You left mTLS on. Disable mTLS on Autonomous DB, use TLS connection string |
| Vercel `Authentication required` / CORS error | `CORS_ORIGINS` on Render must exactly match Vercel URL, no trailing slash |
| Vercel blank page on refresh `/sales` | `vercel.json` rewrites missing — it is committed, redeploy |
| Backend sleeps (free tier) | Normal — upgrade to Starter or hit `/api/health/` to wake before demo |
| Flyway validate error | `ddl-auto: validate` — never edit applied migrations V1–V7, add V8+ instead |
