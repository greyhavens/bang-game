# Production runbook

Operating a Bang! Howdy server deployment (Epic 3 Phase 6). Dev-loop instructions live in
`running-the-game.md`; this covers running real nodes: layout, config/secrets, TLS, health,
logs, upgrades, and common failures.

## Topology

A deployment is one or more **town nodes** (`frontier_town`, `indian_post`, …), each a
`BangServer` JVM serving exactly one town. Single-town = one standalone node, no shared secret.
Multi-town = one node per town, peered as a cluster:

- every node needs the same non-blank `server_secret` (**>= 16 chars** — a node refuses to start
  with a shorter one) and a `nodes` table row is maintained automatically;
- `<node>.server_host` / `<node>.town_id` for every node must be in every node's
  `server.properties`;
- clients hop towns by connecting to the target town's node (`deployment.properties`
  `<node>.server_ports`).

## Host layout (systemd)

```
/opt/bang/                      deploy tree, owned by the `bang` user
  build/server/*.jar            server jars       — ./gradlew deploy output
  assets/build/staging/rsrc/    staged assets     — ./gradlew deploy output
  data/                         boards etc. (checkout or rsync of data/)
  conf/server.properties        from etc/server.properties.dist — NON-secret values only
  conf/deployment.properties    from etc/deployment.properties.dist
  log/                          audit logs (state/item/perf/server per node); lands here only
                                because common.env sets BANG_SERVER_ROOT=/opt/bang — the .dist
                                default is the historical /export/bang, don't skip the override
  deploy/prod/run-node.sh       node launcher (this repo, deploy/prod/)
/etc/systemd/system/bang@.service   template unit (copy from deploy/prod/)
/etc/bang/common.env            secrets shared by all nodes on the host (0600 root:bang)
/etc/bang/<node>.env            optional per-node overrides (unique mgmt port etc.)
/etc/bang/tls-server.p12        TLS keystore (private key — secret)
/etc/bang/tls-trust.p12         TLS truststore (public cert — shippable)
```

Enable a node per town:

```sh
systemctl enable --now bang@frontier_town
systemctl enable --now bang@indian_post     # second town, typically another host
```

## Configuration and secrets — the BANG_* env bridge

`conf/server.properties` is generated from `etc/server.properties.dist` and holds only
non-secret defaults. Everything host- or secret-shaped is injected as environment variables:
`BANG_<KEY>` overrides config key `<key>` (upper-case, `.`/`-` → `_`), e.g.
`BANG_SERVER_SECRET` → `server_secret`, `BANG_DB_DEFAULT_PASSWORD` → `db.default.password`.

**The bridge only overrides keys already present in the baked config** — the `.dist` templates
deliberately keep secret keys present-but-blank. If an env override seems ignored, check the key
exists (even blank) in `conf/server.properties`.

See `deploy/prod/common.env.example` and `node.env.example` for a complete starting set.

## TLS (full-session transport encryption)

`server_tls = true` wraps the entire client<->server stream (login, chat, gameplay) in TLS 1.3.

- Generate material with `bin/gen-tls-certs` (`TLS_KEYSTORE_PASS` / `TLS_TRUST_PASS` env);
  the **keystore password is a secret**, the truststore password is not.
- Cluster peering with TLS on: every node also needs the truststore
  (`server_tls_truststore` + `..._password`) pinning the cluster cert.
- Clients ship the truststore; rotating the cert means shipping a client update — plan rotations
  with a client release.

## Health and metrics

Each node exposes a **loopback-only** management HTTP endpoint when `server_mgmt_port` is set
(never expose it publicly — it is unauthenticated):

- `GET /health` — 200 `ok` once init fully completed and the connection manager runs; 503
  otherwise. Wire into monitoring or a container `HEALTHCHECK` (see `deploy/devstack/compose.yaml`
  for the probe idiom).
- `GET /metrics` — Prometheus text: `bang_up`, `bang_uptime_seconds`, `bang_sessions`,
  `bang_connections`, JVM heap gauges, and `bang_conmgr_*` network counters, all labeled
  `{node="…"}`.

Prometheus scrape (with a node_exporter-style local relay or an isolated network):

```yaml
- job_name: bang
  static_configs:
    - targets: ["127.0.0.1:8081", "127.0.0.1:8082"]
```

A "state of server report" (uptime, heap, sessions) also lands in the server log every 15
minutes, so plain `journalctl -u bang@<node>` / container logs give a coarse health history.

## Logs

- **Server log**: stdout/stderr — `journalctl -u bang@frontier_town` (or container logs).
  `server_log_format = json` (env `BANG_SERVER_LOG_FORMAT`) switches to one-JSON-object-per-line
  for Loki/ELK/CloudWatch; default is human-readable `oneline`. Uncaught exceptions from any
  thread are formatted into this stream too.
- **Audit logs**: `${server_root}/log/{server,item,state,perf}_<node>.log` — game-economy and
  state history, rotated daily by the server itself. Back these up with the database.

## Upgrades

1. Build the new tree: `./gradlew deploy` (CI must be green — the OWASP scan and the bot
   integration gate run on every PR).
2. Rsync `build/server`, `assets/build/staging`, `data`, **and `deploy/prod`** to `/opt/bang` —
   forgetting `deploy/prod` leaves a stale launcher silently in place (no error, unlike stale
   jars). If `bang@.service` itself changed, also re-copy it to `/etc/systemd/system/` and
   `systemctl daemon-reload`.
3. `systemctl restart 'bang@*'` — a **stale node against new jars throws
   `NoClassDefFoundError`** (`client/shared` is lazy-loaded), so always restart every node after
   deploying, even for "client-only" changes.
4. Watch `/health` return 200 and a clean startup in the journal.

DB schema is auto-migrated by Depot at startup (Phase 7 will move this to explicit migrations).

## Common failures

| Symptom | Cause / fix |
| --- | --- |
| exits at boot: `Missing 'node'/'hostname' system property` | launcher didn't pass `-Dnode`/`-Dhostname` — run via `run-node.sh <node>` |
| exits at boot: `Refusing to run in cluster mode with a weak server_secret` | `server_secret` < 16 chars; set a long random `BANG_SERVER_SECRET` |
| `server_auth is not configured` | `conf/server.properties` missing/not on classpath first |
| clients get EOF/handshake errors | TLS mismatch: server TLS on but clients lack the truststore (or vice versa) |
| `refreshPeers failed` warnings | peer DB reachable? all nodes sharing one `nodes` table + same secret? (harmless-looking single warnings on dev are a known Connector/J quirk) |
| in-game `NoClassDefFoundError`, AI units not spawning | stale node after a deploy — restart the node |
| env override ignored | key absent from the baked config — the bridge only overrides present keys |
| `/health` 503 | init incomplete or connection manager stopped — check the journal for the real error |

## Containerized dev/CI reference

`deploy/devstack/` runs the same server in podman/docker (MySQL + one node) and is exercised by
the CI integration gate; its `compose.yaml` healthcheck and `entrypoint.sh` env-derived config
are the reference for containerizing production nodes.
