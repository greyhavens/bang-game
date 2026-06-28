#!/usr/bin/env bash
# Generate etc/test/ from the committed etc/*.dist templates — the documented local setup
# (copy *.dist, fill the DB/path placeholders). Idempotent: an existing etc/test/<file> is left
# untouched, so this never clobbers a dev's hand-tuned config. Used by the CI integration gate
# (which has no gitignored etc/test/) and handy for first-time local setup.
#
# Env overrides (defaults match the devstack): DB_HOST, DB_USER, DB_PASS, SERVER_ROOT.
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
DB_HOST="${DB_HOST:-localhost}"
DB_USER="${DB_USER:-bang}"
DB_PASS="${DB_PASS:-yeehaw}"
SERVER_ROOT="${SERVER_ROOT:-$root}"

mkdir -p "$root/etc/test"
for f in server.properties deployment.properties server.conf; do
  dest="$root/etc/test/$f"
  if [ -f "$dest" ]; then echo "gen-etc-test: $dest exists, leaving"; continue; fi
  # NB: substitute USERDBHOST before DBHOST (the former contains the latter as a substring)
  sed -e "s#USERDBHOST#${DB_HOST}#g" \
      -e "s#DBHOST#${DB_HOST}#g" \
      -e "s#USERNAME#${DB_USER}#g" \
      -e "s#PASSWORD#${DB_PASS}#g" \
      -e "s#^server_root = .*#server_root = ${SERVER_ROOT}#" \
      "$root/etc/$f.dist" > "$dest"
  echo "gen-etc-test: generated $dest"
done
