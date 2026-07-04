# Shared per-database connection resolution for backup.sh / restore.sh (sourced, not executed).
#
# The user DB is NOT guaranteed to share a host with the game DB: server.properties supports
# db.userdb.url pointing ooouser at a separate USERDBHOST (documented deployment shape), with
# optionally distinct credentials. Resolve each database against its matching BANG_DB_* env vars
# (the same ones /etc/bang/common.env feeds the game nodes), falling back to the defaults.
# sitedb shares the ooouser database physically, so it needs no entry of its own.

# db_conn <db> — emits "host<TAB>port<TAB>user<TAB>password" for the given database
db_conn () {
  local db="$1" url user pass
  case "$db" in
    ooouser)
      url="${BANG_DB_USERDB_URL:-${BANG_DB_DEFAULT_URL:-}}"
      user="${BANG_DB_USERDB_USERNAME:-${BANG_DB_DEFAULT_USERNAME:-bang}}"
      pass="${BANG_DB_USERDB_PASSWORD:-${BANG_DB_DEFAULT_PASSWORD:?BANG_DB_DEFAULT_PASSWORD is required}}"
      ;;
    *)
      url="${BANG_DB_DEFAULT_URL:-}"
      user="${BANG_DB_DEFAULT_USERNAME:-bang}"
      pass="${BANG_DB_DEFAULT_PASSWORD:?BANG_DB_DEFAULT_PASSWORD is required}"
      ;;
  esac
  # host:port from the jdbc URL (jdbc:mysql://HOST:PORT/db?...), defaulting each piece
  local hostport; hostport="$(sed -nE 's#^jdbc:mysql://([^/]+)/.*#\1#p' <<< "$url")"
  local host="${hostport%%:*}"; host="${host:-127.0.0.1}"
  local port="${hostport##*:}"; { [ "$port" = "$hostport" ] || [ -z "$port" ]; } && port=3306
  printf '%s\t%s\t%s\t%s\n' "$host" "$port" "$user" "$pass"
}

# db_defaults <db> <file> — (re)writes a mysql option file for the db (password never on argv,
# where ps could see it) and echoes host:port for logging
db_defaults () {
  local db="$1" out="$2" host port user pass
  IFS=$'\t' read -r host port user pass <<< "$(db_conn "$db")"
  printf '[client]\nhost=%s\nport=%s\nuser=%s\npassword=%s\n' \
    "$host" "$port" "$user" "$pass" > "$out"
  echo "$host:$port"
}
