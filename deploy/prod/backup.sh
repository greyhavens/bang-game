#!/usr/bin/env bash
# Bang! Howdy backup (Epic 3 Phase 7). Dumps the two MySQL databases and archives the audit logs
# into a timestamped directory, then prunes old backups. Boards and all other game content live in
# git — the runtime state worth backing up is exactly: the `bang` DB (players/items/gangs/...),
# the `ooouser` DB (accounts/auth), and the gitignored ${server_root}/log audit trails.
#
# Config via env (defaults suit /etc/bang/common.env, so the systemd unit just points its
# EnvironmentFile there). Connections resolve PER DATABASE (db-common.sh): ooouser honors
# BANG_DB_USERDB_URL/_USERNAME/_PASSWORD when set — the user DB living on a separate host is a
# documented deployment shape — and everything falls back to the defaults:
#   BANG_DB_DEFAULT_URL       jdbc URL — host:port are parsed from it (default 127.0.0.1:3306)
#   BANG_DB_DEFAULT_USERNAME  MySQL user           (default bang)
#   BANG_DB_DEFAULT_PASSWORD  MySQL password       (required)
#   BANG_BACKUP_DIR           backup root          (default /var/backups/bang)
#   BANG_BACKUP_DBS           databases to dump    (default "bang ooouser")
#   BANG_BACKUP_KEEP_DAYS     retention in days    (default 14)
#   BANG_HOME                 deploy tree with log/ (default /opt/bang; skip logs if absent)
#
# Run it on ONE host per cluster (the DB is shared; per-node audit logs only cover that node —
# on a multi-host cluster, also archive the other nodes' log/ dirs or run a logs-only variant
# there). Offsite replication of $BANG_BACKUP_DIR (rsync/rclone) is deliberately out of scope.
set -euo pipefail
umask 077

BANG_HOME="${BANG_HOME:-/opt/bang}"
BACKUP_DIR="${BANG_BACKUP_DIR:-/var/backups/bang}"
KEEP_DAYS="${BANG_BACKUP_KEEP_DAYS:-14}"
DBS="${BANG_BACKUP_DBS:-bang ooouser}"

# per-database host/credential resolution (ooouser may live on a separate USERDBHOST)
. "$(dirname "${BASH_SOURCE[0]}")/db-common.sh"

defaults="$(mktemp)"
trap 'rm -f "$defaults"' EXIT

stamp="$(date -u +%Y%m%d-%H%M%S)"
dest="$BACKUP_DIR/$stamp"
mkdir -p "$dest"
echo "backup: writing to $dest"

# --single-transaction = consistent InnoDB snapshot without locking the live game;
# --routines/--triggers/--events for completeness (none are used today, cheap insurance);
# --no-tablespaces because MySQL 8 otherwise wants the PROCESS privilege for NDB tablespace
# info the InnoDB game schema doesn't have anyway
for db in $DBS; do
  target="$(db_defaults "$db" "$defaults")"
  echo "backup: dumping $db (at $target) ..."
  mysqldump --defaults-extra-file="$defaults" --single-transaction \
    --routines --triggers --events --no-tablespaces "$db" | gzip > "$dest/$db.sql.gz"
done

# audit logs (item/state are the financial/inventory trails; server/perf ride along)
if [ -d "$BANG_HOME/log" ]; then
  echo "backup: archiving audit logs ..."
  tar czf "$dest/audit-logs.tar.gz" -C "$BANG_HOME" log
else
  echo "backup: no $BANG_HOME/log dir, skipping audit logs"
fi

# basic integrity check: every dump must be non-trivial and end with the mysqldump completion
# marker — a connection drop mid-dump otherwise leaves a silently-truncated .sql.gz
for db in $DBS; do
  f="$dest/$db.sql.gz"
  if ! gzip -t "$f" 2>/dev/null || ! zcat "$f" | tail -1 | grep -q "Dump completed"; then
    echo "backup: ERROR: $f is truncated or corrupt" >&2
    exit 1
  fi
done

# prune old backups (only timestamped children of OUR backup root, guarded against empty vars)
if [ -n "$BACKUP_DIR" ] && [ -d "$BACKUP_DIR" ]; then
  find "$BACKUP_DIR" -mindepth 1 -maxdepth 1 -type d -name '20*' \
    -mtime +"$KEEP_DAYS" -exec rm -rf {} +
fi

du -sh "$dest"
echo "backup: OK"
