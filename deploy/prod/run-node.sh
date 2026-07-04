#!/usr/bin/env bash
# Production node launcher, invoked by the bang@<node>.service systemd template (one town node
# per service instance). Expects a deployed tree under $BANG_HOME (default /opt/bang):
#
#   build/server/*.jar            server jars     (./gradlew deploy output)
#   assets/build/staging/rsrc/    staged assets   (./gradlew deploy output)
#   data/                         boards etc.
#   conf/server.properties        generated from etc/server.properties.dist — non-secret values
#   conf/deployment.properties    generated from etc/deployment.properties.dist
#
# Secrets and per-host values are NOT baked into conf/ — they arrive via the BANG_* env bridge
# (ServerConfig.loadConfig): systemd injects /etc/bang/common.env + /etc/bang/<node>.env. NB the
# bridge only overrides keys PRESENT in conf/server.properties, so keep the present-but-blank
# secret keys (server_secret, server_tls_password, ...) from the .dist template when generating it.
set -euo pipefail

BANG_HOME="${BANG_HOME:-/opt/bang}"
NODE="${1:?usage: run-node.sh <node>}"

# conf dir FIRST so samskivert Config("server") loads it over any copy baked into a jar
CP="$BANG_HOME/conf:$BANG_HOME/assets/build/staging"
for jar in "$BANG_HOME"/build/server/*.jar; do CP="$CP:$jar"; done

exec java -server -Xmx"${BANG_JVM_HEAP:-1024m}" \
  -Djava.awt.headless=true \
  -Dbang.home="$BANG_HOME" \
  -Dresource_dir="$BANG_HOME/assets/build/staging/rsrc" \
  -Drsrc_cache_dir=/tmp \
  -Dno_unpack_resources=true \
  -Dhostname="${BANG_NODE_HOSTNAME:-$(hostname -f)}" \
  -Dis_node=true \
  -Dnode="$NODE" \
  com.threerings.bang.server.BangServer
