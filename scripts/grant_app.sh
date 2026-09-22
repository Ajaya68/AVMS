#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# AJAYA VENTURE - grants AV_APP (runtime) privileges on all AV_MIGRATION
# objects and creates same-name synonyms in AV_APP so application SQL needs no
# schema prefix. AV_REPORT receives SELECT on views/tables for reporting.
# Idempotent: safe to run after every migration.
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

SQLPLUS="${SQLPLUS_BIN:-sqlplus}"
MIG_CONN="${MIG_USER}/${MIG_PASS}@//${HOST}:${PORT}/${SERVICE}"

"${SQLPLUS}" -L -S "${MIG_CONN}" > /tmp/av_grant.log 2>&1 <<'SQL'
SET ECHO OFF
SET SERVEROUTPUT ON
SET FEEDBACK OFF

DECLARE
  v_count NUMBER;
  v_stage VARCHAR2(60);
BEGIN
  -- Drop any AV_APP synonyms created previously (recreate fresh each run)
  FOR rec IN (SELECT synonym_name FROM all_synonyms
              WHERE synonym_name IN (SELECT table_name FROM user_tables)
                AND table_owner = USER)
  LOOP
    BEGIN
      EXECUTE IMMEDIATE 'DROP SYNONYM ' || rec.synonym_name;
    EXCEPTION WHEN OTHERS THEN NULL;
    END;
  END LOOP;

  -- Tables: full DML to AV_APP, SELECT to AV_REPORT, synonyms to AV_APP
  FOR rec IN (SELECT table_name FROM user_tables) LOOP
    EXECUTE IMMEDIATE 'GRANT SELECT, INSERT, UPDATE, DELETE ON ' || rec.table_name || ' TO AV_APP';
    EXECUTE IMMEDIATE 'GRANT SELECT ON ' || rec.table_name || ' TO AV_REPORT';
    EXECUTE IMMEDIATE 'CREATE OR REPLACE SYNONYM AV_APP.' || rec.table_name || ' FOR ' || rec.table_name;
  END LOOP;

  -- Views: SELECT only
  FOR rec IN (SELECT view_name FROM user_views) LOOP
    EXECUTE IMMEDIATE 'GRANT SELECT ON ' || rec.view_name || ' TO AV_APP';
    EXECUTE IMMEDIATE 'GRANT SELECT ON ' || rec.view_name || ' TO AV_REPORT';
    EXECUTE IMMEDIATE 'CREATE OR REPLACE SYNONYM AV_APP.' || rec.view_name || ' FOR ' || rec.view_name;
  END LOOP;

  -- Sequences: SELECT (nextval/currval) to AV_APP
  FOR rec IN (SELECT sequence_name FROM user_sequences) LOOP
    EXECUTE IMMEDIATE 'GRANT SELECT ON ' || rec.sequence_name || ' TO AV_APP';
  END LOOP;

  -- Packages/procedures/functions: EXECUTE
  FOR rec IN (SELECT object_name, object_type FROM user_objects
              WHERE object_type IN ('PACKAGE','PACKAGE BODY','PROCEDURE','FUNCTION')) LOOP
    EXECUTE IMMEDIATE 'GRANT EXECUTE ON ' || rec.object_name || ' TO AV_APP';
  END LOOP;
END;
/
PROMPT Grants and synonyms refreshed.
SQL

if ! grep -q "Grants and synonyms refreshed" /tmp/av_grant.log; then
  echo "Grant refresh failed:"
  cat /tmp/av_grant.log
  exit 1
fi
echo "Done. AV_APP and AV_REPORT privileges refreshed."