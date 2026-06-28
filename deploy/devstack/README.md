# devstack — containerized devtest (Epic 3 Phase 2)

A one-command **MySQL + BangServer** stack for development/CI, so "works on my machine" and "works
in a container" converge. Rootless **podman-compose** (Docker-compatible; `docker compose` works too).

## What it is
- **`mysql`** — `mysql:8.0`, seeded by `mysql-init.sql` with empty `bang` + `ooouser` databases and
  the `bang`/`yeehaw` account (schema/tables are created on first run by the server itself).
- **`server`** — a stock `eclipse-temurin:21-jre` image that **bind-mounts the host's
  `./gradlew deploy` output** (`build/server` jars + `assets/build/staging` + `data/boards`) and runs
  a `frontier_town` `BangServer` node via `entrypoint.sh`. No custom image to rebuild when game code
  changes — just re-`deploy` on the host and restart the `server` service.

The server's baked `server.properties` points at `localhost:3306` and the host checkout path;
`entrypoint.sh` rewrites the jdbc host → the `mysql` service and `server_root` → `/bang` (podman-compose
uses per-container networking, not a shared pod), and shadows the baked config on the classpath.

## Use
```sh
./gradlew deploy                                          # build server jars + staged assets (host)
podman-compose -f deploy/devstack/compose.yaml up -d      # bring up MySQL + the server node
podman logs -f bang-devstack_server_1                     # watch it reach "Server listening on …:47624"
podman-compose -f deploy/devstack/compose.yaml down       # stop + remove (add -v to wipe the DB volume)
```
The server is published on `localhost:47624`, so a host client (`./bin/bangclient`) or the headless
bot (`tools:bot-client`) can connect. The `test`/`yeehaw` account is auto-created on first boot.

## Notes / follow-ups
- **Re-`deploy` to pick up code changes**, then `podman restart bang-devstack_server_1` (bind mounts,
  so no image rebuild).
- The harmless `refreshPeers failed` / `NodeRecord LocalDateTime↔Timestamp` warning is the known
  Connector/J-8 peer-presence quirk (single-node devstack is unaffected).
- **Portability:** the server mounts the host's gitignored `etc/test/server.properties`; for a fully
  self-contained / CI image, generate that from `etc/server.properties.dist` and bake a server image
  (the Epic-3 "publish the server image" bullet) instead of bind-mounting host build output.
- **rootless podman tip:** avoid rapid `down`/`up` churn — it can wedge a container in `Stopping`
  state. If that happens, `podman rm -f` the stack (the DB volume `bang-devstack_bang-db` persists).
