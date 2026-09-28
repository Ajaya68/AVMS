# Option A — Live on internet: single VPS (Oracle XE + backend + frontend + auto HTTPS)

Cheapest + simplest for AVMS. One server (~$6/mo Hetzner/DigitalOcean, or Oracle Cloud Free Ampere), everything in Docker. ~30–60 min.

## 0. What you get

`https://YOUR-DOMAIN/` (React) + `https://YOUR-DOMAIN/api/health/` (Spring) + Oracle XE inside Docker. Caddy auto-provisions Let's Encrypt certs. Flyway creates tables + seed on first boot.

Files added for this: `docker-compose.prod.yml`, `Caddyfile`, `.env.prod.example`, `frontend/nginx.conf` (SPA fallback so refresh on `/login`, `/sales` works).

## 1. Get a VPS + domain

1. VPS (pick one, 4 GB RAM minimum — Oracle XE needs ~2 GB):
   - Oracle Cloud Free: Ampere 4 OCPU / 24 GB (free) — best value
   - Hetzner CX22 / DigitalOcean $6–12 droplet, Ubuntu 22.04/24.04
2. Domain (e.g. `avms.example.com`): add DNS `A` record → VPS public IP. Wait until `ping avms.example.com` resolves.
3. Open firewall: `22` (ssh), `80` + `443` (web). Keep `1521`/`8080` closed — Caddy is the only entrypoint.

## 2. Prepare the server

```bash
ssh root@YOUR-VPS-IP
apt update && apt install -y docker.io docker-compose-plugin git ufw
ufw allow 22,80,443/tcp && ufw --force enable
git clone https://github.com/Ajaya68/AVMS.git /opt/avms
cd /opt/avms
cp .env.prod.example .env.prod
nano .env.prod   # fill DOMAIN, VITE_API_URL, 3 passwords, JWT_SECRET
```

Generate the JWT secret on the server:

```bash
openssl rand -base64 48
```

`.env.prod` checklist: `DOMAIN` = your domain, `VITE_API_URL=https://<DOMAIN>/api`, `ORACLE_PASSWORD` / `APP_USER_PASSWORD` / `DB_PASSWORD` strong + `APP_USER_PASSWORD == DB_PASSWORD`, `JWT_SECRET` 32+ chars.

## 3. Launch

```bash
cd /opt/avms
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build
docker compose --env-file .env.prod -f docker-compose.prod.yml ps
docker compose --env-file .env.prod -f docker-compose.prod.yml logs -f backend  # wait for Flyway V1–V7
```

Oracle first boot takes 2–5 min (`service_healthy`), backend runs migrations after that.

## 4. Verify live

- `https://YOUR-DOMAIN/` → login page
- `https://YOUR-DOMAIN/api/health/` → `{"success":true,"data":{"status":"ok"...}}`
- `https://YOUR-DOMAIN/api/docs.html` → 404 in prod by design (API docs are dev-only; run locally with the dev profile to use them)
- Login → create venture → product → purchase → sale (confirms DB writes)

## 5. Operate

```bash
# update to latest
cd /opt/avms && git pull
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build

# backups (nightly cron recommended)
docker exec avms-oracle-1 expdp avms/***@XEPDB1 full=y directory=DATA_PUMP_DIR dumpfile=avms_$(date +%F).dmp
# or snapshot the volume: /var/lib/docker/volumes/avms_oracle-data

# logs
docker compose --env-file .env.prod -f docker-compose.prod.yml logs -f caddy backend frontend
```

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| Caddy `TLS handshake / no cert` | DNS `A` record wrong or port 80 blocked — Caddy needs HTTP-01 on port 80 |
| Backend `ORA-01017` | `DB_PASSWORD` != `APP_USER_PASSWORD` in `.env.prod` — must match |
| Backend waits on `service_healthy` long | Normal first boot 2–5 min: `docker logs avms-oracle-1` |
| Frontend `Authentication required` / CORS | `CORS_ORIGINS` is derived from `DOMAIN` in prod compose — rebuild after domain change |
| Blank page on refresh `/sales` | Fixed by `frontend/nginx.conf` — rebuild frontend: `... up -d --build frontend` |
| Out of memory | Oracle XE + Java need 4 GB RAM — upgrade VPS, don't run dev stack alongside |
| `VITE_API_URL` wrong after deploy | It bakes at build time — edit `.env.prod`, then `--build frontend` |
