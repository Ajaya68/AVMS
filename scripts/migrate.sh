#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# AJAYA VENTURE - ordered database migration runner.
# Applies any pending database/migrations/V<ver>__<name>.sql scripts as the
# AV_MIGRATION schema user, tracking applied versions in AV_SCHEMA_MIGRATION.
# After schema migrations, grants/synonyms for AV_APP are regenerated.
#
# Usage:  bash scripts/migrate.sh
# ---------------------------------------------------------------------------
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

if [[ -f "${ROOT_DIR}/.env" ]]; then
  set -a
  # shellcheck source=/dev/null
  source "${ROOT_DIR}/.env"
  set +a
fi

HOST="${DB_HOST:-localhost}"
PORT="${DB_PORT:-1521}"
SERVICE="${DB_SERVICE:-orclpdb}"
MIG_USER="${DB_MIGRATION_USERNAME:-AV_MIGRATION}"
MIG_PASS="${DB_MIGRATION_PASSWORD:-change-me}"

SQLPLUS="${SQLPLUS_BIN:-sqlplus}"
MIG_CONN="${MIG_USER}/${MIG_PASS}@//${HOST}:${PORT}/${SERVICE}"

MIGRATION_DIR="${ROOT_DIR}/database/migrations"

if ! compgen -G "${MIGRATION_DIR}/V*__*.sql" >/dev/null; then
  echo "No migrations found under ${MIGRATION_DIR}"
  exit 0
fi

applied_versions() {
  "${SQLPLUS}" -L -S "${MIG_CONN}" <<'SQL'
SET PAGESIZE 0
SET FEEDBACK OFF
SET HEADING OFF
SET TRIMSPOOL ON
SELECT version FROM av_migration.av_schema_migration WHERE success = 'Y' ORDER BY version;
EXIT;
SQL
}

echo "=== Migrations against ${SERVICE} as ${MIG_USER} ==="
APPS="$(applied_versions || true)"

count=0
for FILE in "${MIGRATION_DIR}"/V*__*.sql; do
  NAME="$(basename "${FILE}")"
  VERSION="${NAME%%__*}"
  VERSION="${VERSION#V}"

  if echo "${APPS}" | grep -qx "${VERSION}"; then
    echo "skip     ${NAME} (already applied)"
    continue
  fi

  echo "applying ${NAME} ..."
  SQLFILE="$(cygpath -m "${FILE}" 2>/dev/null || echo "${FILE}")"
  if ! "${SQLPLUS}" -L -S "${MIG_CONN}" @ "${SQLFILE}" > /tmp/av_migration_${VERSION}.log 2>&1; then
    echo "MIGRATION FAILED: ${NAME}"
    cat /tmp/av_migration_${VERSION}.log
    exit 1
  fi

  # record successful application
  "${SQLPLUS}" -L -S "${MIG_CONN}" <<SQL > /tmp/av_migration_track.log 2>&1
WHENEVER SQLERROR EXIT FAILURE
INSERT INTO av_migration.av_schema_migration (version, filename, applied_by)
VALUES ('${VERSION}', '${NAME}', USER);
COMMIT;
EXIT;
SQL
  if [[ $? -ne 0 ]]; then
    echo "TRACKING FAILED for ${NAME}"
    cat /tmp/av_migration_track.log
    exit 1
  fi
  count=$((count + 1))
done

echo "Applied ${count} new migration(s)."
echo "=== Refreshing AV_APP grants/synonyms ==="
bash "${SCRIPT_DIR}/grant_app.sh"
echo "=== Migration complete ==="