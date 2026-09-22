#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# AJAYA VENTURE - grants AV_APP (runtime) privileges on all AV_MIGRATION
# objects and creates same-name synonyms in AV_APP so application SQL needs no
# schema prefix. AV_REPORT receives SELECT on views/tables for reporting.
#
# Two phases: privileges are granted as AV_MIGRATION; synonyms are created as
# AV_APP (which holds CREATE SYNONYM). Idempotent: safe after every migration.
#
# Usage:  bash scripts/grant_app.sh
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
APP_USER="${DB_USERNAME:-AV_APP}"
APP_PASS="${DB_PASSWORD:-change-me}"

SQLPLUS="${SQLPLUS_BIN:-sqlplus}"
MIG_CONN="${MIG_USER}/${MIG_PASS}@//${HOST}:${PORT}/${SERVICE}"
APP_CONN="${APP_USER}/${APP_PASS}@//${HOST}:${PORT}/${SERVICE}"

echo "=== Privileges (as ${MIG_USER}) ==="
"${SQLPLUS}" -L -S "${MIG_CONN}" > /tmp/av_grant_mig.log 2>&1 <<'SQL'
SET ECHO OFF
SET SERVEROUTPUT ON
SET FEEDBACK OFF

DECLARE
  v_priv VARCHAR2(200);
BEGIN
  FOR rec IN (SELECT table_name FROM user_tables
              WHERE table_name <> 'AV_SCHEMA_MIGRATION') LOOP
    EXECUTE IMMEDIATE 'GRANT SELECT, INSERT, UPDATE, DELETE ON ' || rec.table_name || ' TO AV_APP';
    EXECUTE IMMEDIATE 'GRANT SELECT ON ' || rec.table_name || ' TO AV_REPORT';
  END LOOP;
  FOR rec IN (SELECT view_name FROM user_views) LOOP
    EXECUTE IMMEDIATE 'GRANT SELECT ON ' || rec.view_name || ' TO AV_APP';
    EXECUTE IMMEDIATE 'GRANT SELECT ON ' || rec.view_name || ' TO AV_REPORT';
  END LOOP;
  FOR rec IN (SELECT sequence_name FROM user_sequences) LOOP
    EXECUTE IMMEDIATE 'GRANT SELECT ON ' || rec.sequence_name || ' TO AV_APP';
  END LOOP;
  FOR rec IN (SELECT object_name, object_type FROM user_objects
              WHERE object_type IN ('PACKAGE','PACKAGE BODY','PROCEDURE','FUNCTION')) LOOP
    EXECUTE IMMEDIATE 'GRANT EXECUTE ON ' || rec.object_name || ' TO AV_APP';
  END LOOP;
  DBMS_OUTPUT.PUT_LINE('PRIVILEGES_OK');
END;
/
SQL

if ! grep -q "PRIVILEGES_OK" /tmp/av_grant_mig.log; then
  echo "Privilege grants failed:"
  cat /tmp/av_grant_mig.log
  exit 1
fi

echo "=== Synonyms (as ${APP_USER}) ==="
"${SQLPLUS}" -L -S "${APP_CONN}" > /tmp/av_grant_syn.log 2>&1 <<'SQL'
SET ECHO OFF
SET SERVEROUTPUT ON
SET FEEDBACK OFF

BEGIN
  FOR rec IN (SELECT synonym_name FROM user_synonyms WHERE table_owner = 'AV_MIGRATION') LOOP
    EXECUTE IMMEDIATE 'DROP SYNONYM ' || rec.synonym_name;
  END LOOP;
  FOR rec IN (SELECT table_name FROM all_tables WHERE owner = 'AV_MIGRATION'
              AND table_name <> 'AV_SCHEMA_MIGRATION') LOOP
    EXECUTE IMMEDIATE 'CREATE SYNONYM ' || rec.table_name || ' FOR AV_MIGRATION.' || rec.table_name;
  END LOOP;
  FOR rec IN (SELECT view_name FROM all_views WHERE owner = 'AV_MIGRATION') LOOP
    EXECUTE IMMEDIATE 'CREATE SYNONYM ' || rec.view_name || ' FOR AV_MIGRATION.' || rec.view_name;
  END LOOP;
  DBMS_OUTPUT.PUT_LINE('SYNONYMS_OK');
END;
/
SQL

if ! grep -q "SYNONYMS_OK" /tmp/av_grant_syn.log; then
  echo "Synonym creation failed:"
  cat /tmp/av_grant_syn.log
  exit 1
fi
echo "Done. AV_APP and AV_REPORT privileges refreshed."