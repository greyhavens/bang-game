#!/usr/bin/env bash
# Bang! Howdy restore (Epic 3 Phase 7) — the counterpart of backup.sh.
#
#   restore.sh <backup-dir> <db>...        e.g. restore.sh /var/backups/bang/20260704-043000 bang
#   restore.sh <backup-dir> all            restore every *.sql.gz in the backup dir
#
# DESTRUCTIVE: each restore drops and recreates the named database from the dump. Stop every
# server node first (a live node would keep writing mid-restore and be left with stale caches).
# Requires typing the literal database name(s) to confirm, or pass BANG_RESTORE_YES=1 (automation).
# DB connection env is the same as backup.sh (BANG_DB_DEFAULT_URL/USERNAME/PASSWORD).
set -euo pipefail
umask 077

src="${1:?usage: restore.sh <backup-dir> <db>...|all}"; shift
[ -d "$src" ] || { echo "restore: no such backup dir: $src" >&2; exit 1; }

DB_USER="${BANG_DB_DEFAULT_USERNAME:-bang}"
DB_PASS="${BANG_DB_DEFAULT_PASSWORD:?BANG_DB_DEFAULT_PASSWORD is required}"
hostport="$(sed -nE 's#^jdbc:mysql://([^/]+)/.*#\1#p' <<< "${BANG_DB_DEFAULT_URL:-}")"
DB_HOST="${hostport%%:*}"; DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${hostport##*:}"; [[ "$DB_PORT" == "$hostport" || -z "$DB_PORT" ]] && DB_PORT=3306

defaults="$(mktemp)"
trap 'rm -f "$defaults"' EXIT
printf '[client]\nhost=%s\nport=%s\nuser=%s\npassword=%s\n' \
  "$DB_HOST" "$DB_PORT" "$DB_USER" "$DB_PASS" > "$defaults"

# resolve "all" to whatever the backup actually contains
if [ "${1:-}" = "all" ]; then
  set -- $(find "$src" -maxdepth 1 -name '*.sql.gz' -printf '%f\n' | sed 's/\.sql\.gz$//')
fi
[ $# -ge 1 ] || { echo "restore: no databases named and none found in $src" >&2; exit 1; }

for db in "$@"; do
  f="$src/$db.sql.gz"
  [ -f "$f" ] || { echo "restore: $f not found" >&2; exit 1; }
  gzip -t "$f" || { echo "restore: $f is corrupt" >&2; exit 1; }

  if [ "${BANG_RESTORE_YES:-0}" != "1" ]; then
    echo "About to DROP and restore database '$db' on ${DB_HOST}:${DB_PORT} from $f"
    read -r -p "Type '$db' to confirm: " answer
    [ "$answer" = "$db" ] || { echo "restore: aborted"; exit 1; }
  fi

  echo "restore: restoring $db ..."
  mysql --defaults-extra-file="$defaults" \
    -e "DROP DATABASE IF EXISTS \`$db\`; CREATE DATABASE \`$db\`;"
  zcat "$f" | mysql --defaults-extra-file="$defaults" "$db"
  echo "restore: $db done"
done

echo "restore: OK — restart the server nodes (systemctl restart 'bang@*')"
