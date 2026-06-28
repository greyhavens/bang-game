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

echo "devstack: waiting for MySQL at ${DB_HOST}:3306 ..."
for i in $(seq 1 60); do
  if (echo > "/dev/tcp/${DB_HOST}/3306") 2>/dev/null; then echo "devstack: MySQL is up."; break; fi
  sleep 2
  [ "$i" = 60 ] && { echo "devstack: MySQL never came up"; exit 1; }
done

# The baked server.properties carries host-specific absolute values: the jdbc URLs point at
# localhost:3306, and server_root is the host checkout path (used for the audit log dir). Rewrite both
# for the container (DB -> the mysql service host, server_root -> $BANG_HOME) and put the override
# FIRST on the classpath so samskivert Config("server") loads it instead of the copy baked into the
# server jar. Only the `//host:3306` jdbc authority is touched, so non-DB `localhost` settings (node
# server_host etc.) are left alone.
mkdir -p "$BANG_HOME/conf" "$BANG_HOME/log"
sed -e "s#//localhost:3306#//${DB_HOST}:3306#g" \
    -e "s#^server_root *=.*#server_root = ${BANG_HOME}#" \
  "$BANG_HOME/conf-src/server.properties" > "$BANG_HOME/conf/server.properties"

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
java -cp "$CP" "${JOPTS[@]}" -Dooo.propfile=server \
  com.threerings.bang.server.tools.SetUserBirthday test 1990-01-01 || true

echo "devstack: starting BangServer node=${NODE} ..."
exec java -cp "$CP" "${JOPTS[@]}" -Dnode="$NODE" com.threerings.bang.server.BangServer
