#!/usr/bin/env bash
# Epic 3 Phase 2 devstack — BangServer container entrypoint.
# Mirrors bin/bangserver: classpath = staged rsrc + build/server jars; same -D flags. Waits for
# MySQL, ensures the test/yeehaw account (so the bot client / a real client can log in), then runs
# the frontier_town node. DB_HOST defaults to 127.0.0.1 (podman-compose runs services in one pod,
# sharing localhost); override to the mysql service name if running under per-container networking.
set -euo pipefail

BANG_HOME=/bang
DB_HOST="${DB_HOST:-127.0.0.1}"
NODE="${BANG_NODE:-frontier_town}"
MGMT_PORT="${BANG_SERVER_MGMT_PORT:-8081}"

echo "devstack: waiting for MySQL at ${DB_HOST}:3306 ..."
for i in $(seq 1 60); do
  if (echo > "/dev/tcp/${DB_HOST}/3306") 2>/dev/null; then echo "devstack: MySQL is up."; break; fi
  sleep 2
  [ "$i" = 60 ] && { echo "devstack: MySQL never came up"; exit 1; }
done

# The baked server.properties carries host-specific values (jdbc URLs point at localhost:3306,
# server_root at the host checkout path), and this container needs the Phase-6 management endpoint
# on regardless of how old the mounted host config is. Rather than sed-ing patterns in the baked
# file, APPEND explicit env-derived overrides — java.util.Properties keeps the last value for a
# repeated key, so appended keys always win — and put the override dir FIRST on the classpath so
# samskivert Config("server") loads it instead of the copy baked into the server jar.
#
# Why an override file and not the BANG_* env bridge (ServerConfig.loadConfig)? Two reasons: the
# legacy UserTool pre-steps below read Config("server") directly and predate the bridge, and the
# bridge only overrides keys PRESENT in the loaded config — a dev's pre-Phase-6
# etc/test/server.properties has no server_mgmt_* keys, so a pure env override would silently
# no-op. Appended keys work for both consumers and any vintage of mounted config. (Production
# servers — no UserTool, config generated fresh from the .dist templates — use the pure BANG_* env
# bridge; see docs/runbook.md.)
mkdir -p "$BANG_HOME/conf" "$BANG_HOME/log"
{
  cat "$BANG_HOME/conf-src/server.properties"
  cat <<EOF

# --- devstack container overrides (appended; last value wins) ---
db.default.url = jdbc:mysql://${DB_HOST}:3306/bang?serverTimezone=UTC
db.userdb.url = jdbc:mysql://${DB_HOST}:3306/ooouser?serverTimezone=UTC
db.sitedb.url = jdbc:mysql://${DB_HOST}:3306/ooouser?serverTimezone=UTC
server_root = ${BANG_HOME}
# management endpoint (/health for the container healthcheck, /metrics for scraping); loopback
# only — the compose healthcheck runs inside the container's network namespace
server_mgmt_port = ${MGMT_PORT}
server_mgmt_host = 127.0.0.1
EOF
} > "$BANG_HOME/conf/server.properties"

# the compose healthcheck runs `bash /bang/healthcheck.sh` (a deliberately quote-free command:
# podman-compose 1.0.6 mangles quoted CMD lists, and its /bin/sh is dash, which has no /dev/tcp —
# so the bash-isms live here, not in compose.yaml). Probes the Phase-6 /health endpoint; the
# temurin JRE image ships no curl/wget.
cat > "$BANG_HOME/healthcheck.sh" <<EOF
#!/usr/bin/env bash
exec 3<>/dev/tcp/127.0.0.1/${MGMT_PORT} || exit 1
printf 'GET /health HTTP/1.0\r\n\r\n' >&3
grep -q ' 200 ' <&3
EOF
chmod +x "$BANG_HOME/healthcheck.sh"

# classpath: override conf dir FIRST, then the staged rsrc tree + every server jar (bin/bangserver)
CP="$BANG_HOME/conf:$BANG_HOME/assets/build/staging"
for jar in "$BANG_HOME"/build/server/*.jar; do CP="$CP:$jar"; done

JOPTS=(-server -Xmx512m
  -Djava.awt.headless=true
  -Dbang.home="$BANG_HOME"
  -Dresource_dir="$BANG_HOME/assets/build/staging/rsrc"
  -Drsrc_cache_dir=/tmp
  -Dno_unpack_resources=true
  -Dhostname=devstack -Dis_node=true)

# ensure the test user exists (idempotent — ignore "already exists" on restarts). UserTool/Depot
# auto-create the ooouser tables on first touch.
echo "devstack: ensuring test/yeehaw account ..."
java -cp "$CP" "${JOPTS[@]}" -Dooo.propfile=server \
  com.threerings.user.tools.UserTool create test yeehaw test@test.com || echo "devstack: (user likely already exists)"
# grant the ADMIN token (autoplay / the bot smoke test require it; takes effect next fresh session,
# i.e. when BangServer starts below). Idempotent: make_admin exits non-zero if already admin.
java -cp "$CP" "${JOPTS[@]}" -Dooo.propfile=server \
  com.threerings.user.tools.UserTool make_admin test || echo "devstack: (test is already admin)"
java -cp "$CP" "${JOPTS[@]}" -Dooo.propfile=server \
  com.threerings.bang.server.tools.SetUserBirthday test 1990-01-01 || true

echo "devstack: starting BangServer node=${NODE} (mgmt on 127.0.0.1:${MGMT_PORT}) ..."
exec java -cp "$CP" "${JOPTS[@]}" -Dnode="$NODE" com.threerings.bang.server.BangServer
