# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

The source code and media for Bang! Howdy, a western-themed multiplayer tactics MMO created
by Three Rings Design in 2004. Rendering runs on **stock jMonkeyEngine 3.9 + LWJGL3** (the
Epic-1 cutover off the vendored 2005-era jME fork + LWJGL2 + libGDX — see `docs/epics.md` and
`docs/jme3-cutover-plan.md`). The running client/editor ship and load **zero fork code**; the
old `jme` fork module survives only as a **build-time** model/particle asset compiler (deleting
it entirely is deferred to Epic 4). Code is BSD-licensed; media (everything under
`assets/rsrc/`) is CC Attribution-NonCommercial.

## Build and run

Built with Gradle 8.14 (via the checked-in wrapper) on a JDK 21 toolchain. Gradle is the only
build — the old per-module Maven `pom.xml` files were stale and have been removed. (The forked
Three Rings / samskivert dependency libs *are* Maven-built; see `docs/epic2-threerings-consolidation.md`.)

```sh
./gradlew deploy                  # build everything into build/client, build/server, build/assets
./gradlew test                    # run unit tests
./gradlew client:shared:test --tests '*RatingUnitTest'   # run a single test class
./gradlew server:createTestUser   # create user `test` / password `yeehaw` in the user DB
```

Local setup before running:
1. Copy each `etc/*.dist` file to `etc/test/<name-without-.dist>` (e.g. `etc/deployment.properties.dist`
   → `etc/test/deployment.properties`). The `processResources` tasks bake `etc/${deployment}/` files
   into the built jars; `deployment` defaults to `test` (set in the root `build.gradle`).
2. The server needs MySQL: create `bang` and `ooouser` databases and set
   `db.default.*` / `db.userdb.url` / `db.sitedb.url` in `etc/test/server.properties`.

Run via the shell scripts in `bin/`:
- `./bin/bangclient` — dev client (flags: `-test=<board>`, `-autoplay`, `-tutorial=<name>`, `-go=<where>`;
  add `-Dusername=test -Dpassword=yeehaw` to auto-logon; `-autoplay` needs the account to
  hold the admin token — see README — and the server caches session tokens across reconnects)
- `./bin/bangserver` — server (creates DB tables on first run, listens on port 47624)
- `./bin/bangeditor` — board editor

Dev flags worth knowing:
- **Audio plays by default.** Pass `-Dmute=true` to silence music + effects for a dev session
  (`BangPrefs.SILENT`; does not persist to the volume prefs).
- **Screenshots capture only the Bang window** — use `bin/shot <out.png>` (or `bin/devtest --shot`,
  which calls it), never a full-display grab. This is for privacy (a full-display grab leaks
  whatever else is on the multi-monitor desktop into shared/committed images) and relevance.
  `bin/shot` matches the window precisely (title `Bang Editor` or starting with `Bang! Howdy`,
  excluding browsers/terminals by WM_CLASS) and grabs only that window's pixmap.

### Running the game (esp. from sub-agents) — singleton + memory constraints

The live game is a **singleton**: one server (port 47624), one client on `DISPLAY=:1`, one gradle
build, one working tree. **Only one sub-agent may touch the running game / build / shared source files
at a time.** Two agents editing the same files race and corrupt the tree; two game clients (each a
~768M heap) plus concurrent builds exhaust RAM+swap and crash the server/client. Parallelize only
read-only or compile-only work (e.g. the migration-audit fan-out) — never two game-touching agents.

**A `TaskStop` does NOT reliably kill an agent that is running the game/build.** These agents spawn
long-running background children (`nohup ./bin/bangserver &`, the client, `./gradlew deploy`); an agent
with a live child only "comes to rest" on stop and then RESUMES when the child reports — it keeps
billing tokens even after a "killed" notification. **Before launching a replacement agent, confirm the
old one is actually gone** (check the `/tasks` agents panel — is it still counting time/tokens?). If it
is, stop it again, or use "stop all agents" (ctrl+x ctrl+k). (Mistake made 2026-06-19: a stop didn't
stick, a second agent was launched on the same files, and the two ran concurrently — wasted tokens,
raced edits, OOM'd the server.)

Process order still applies: `./gradlew deploy` must FULLY finish before starting the server, and the
server must be (re)started AFTER a deploy (a stale server throws `NoClassDefFoundError`/`ClassNotFound`).
This applies to **client-only** changes too: `client/shared` is a shared client+server jar that the
server lazy-loads game classes from, so *any* deploy invalidates the running server(s) — always cycle
them. (Symptom of a stale node: in-game `NoClassDefFoundError` for sprite/handler classes, AI units not
spawning / `liveUnits=0`.)

### Running Indian Post (and any specific scenario) in dev — two-node cluster

A Bang server node serves exactly **one** town (`-Dnode=<name>`, town from `server.properties`'s
`<node>.town_id`). To use **Indian Post** in dev (the Station train to it, the Wendigo Attack scenario,
etc.) you must run a **second node**, because a single node can't serve two towns and the client picks
the town's node by port. The dev-localhost cluster (segregated from production, which runs one node per
host via `.dist` config):

- **`bin/dev-cluster {start|stop|status}`** (committed) launches both local nodes: `frontier_town` →
  `localhost:47624`, `indian_post` → `localhost:47625`. **Restart it after every `./gradlew deploy`**
  (`bin/dev-cluster stop && bin/dev-cluster start`).
- Local config (these live in the **gitignored** `etc/test/`, so they're per-dev, not committed):
  `server.properties` needs `indian_post.town_id = indian_post`, `indian_post.server_host = localhost`,
  and a `server_secret` (so the two nodes peer); `deployment.properties` needs
  `indian_post.server_host = localhost` and `indian_post.server_ports = 47625` (read by BOTH the client,
  for the train target, AND the indian_post node, as its listen port via `getServerPorts(townId)`).
- The client logs into a town by `lastTownId`; once you've traveled to Indian Post it stays there on
  relaunch (so `-go=ranch`/in-game launches land on the indian_post node). Known dev caveat: the peer
  layer logs `refreshPeers failed` / a `NodeRecord` LocalDateTime↔Timestamp warning (Connector/J 8) —
  harmless for travel, only degrades cross-node presence.

**Auto-start a specific scenario** (no menu clicking): `startTestGame` honors `-Dscenario=<ident>`, else
random-picks from the *current town's* scenario set. So from Indian Post,
`./bin/bangclient -test -autoplay -Dscenario=wa ...` drops straight into a **Wendigo Attack** game; the
same `-Dscenario=<ident>` works for any scenario on whichever town's node you're logged into. (`-Dboard=`
and `-Dplayers=` are also honored.) This is the deterministic way to self-verify ITP/wendigo visuals.

## Module structure

Gradle modules (see `settings.gradle`), in dependency order:

- `jme` — the **vendored 2005-era jMonkeyEngine fork** (LWJGL 2.9; its libGDX backend was excised
  in Epic 3 Phase 8 — the repo is now gdx-free). **Build-time only**: no
  longer on any runtime/main classpath — it survives solely as the model/particle asset compiler
  (`CompileModelTask` + the fork `BinaryImporter` the `.dat`→`.j3o` bake reads). Epic 4 retires it.
- `app` — `com.threerings.jme.*`: scenegraph/sprite/model/camera framework, now on **jME3** (a thin
  `Model` facade over baked `.j3o`); its fork-coupled model *compiler* lives in a separate
  `modeltool` source set (the only `app` code that still sees the `jme` fork).
- `bui` — BUI, the in-game UI toolkit, on **jME3** (`Jme3RenderBackend`/`Jme3RootNode`; input via the
  jME3 `InputManager`, keycodes are jME3 `KeyInput` — the old gdx keycode source is gone)
- `client/shared` — the bulk of all game code (`com.threerings.bang.*`), client AND server logic
- `client/desktop` — thin **jME3/LWJGL3** launchers: `BangDesktop` (client), `EditorDesktop` (editor
  on a jME3 AWT canvas via `org.lwjglx:lwjgl3-awt`)
- `client/getdown` — Getdown auto-updater installer stub
- `server` — `BangServer` entry point plus persistence (`server/persist`, Depot ORM, MySQL)
- `assets` — all media under `rsrc/`, plus heavyweight resource-processing tasks (model
  compilation → `model.dat`, the `model.dat`/`particles.jme` → `.j3o` bake, avatar component bundles,
  XML config compilation) run as part of `processResources`
- `tools` — build-time ant tasks; `tools/j3o-converter` hosts the fork→jME3 asset bake + the
  offscreen render harnesses (`RenderModelToPng`/`SnapshotDiff`/`verifyVisuals`)

For non-obvious engine internals — the jME3/LWJGL3 GL-context coupling, threading rules,
model/board pipelines, and narya wire-protocol gotchas — read `docs/engine-notes.md` before
touching rendering, assets, or streaming code.

## Architecture

The game is built on Three Rings' Narya "Presents" distributed-object framework (deps:
`com.threerings:narya`, `nenya`, `vilya`, `com.samskivert:samskivert`, `com.samskivert:depot`).
Understanding the Presents pattern is essential to navigating this code:

- Each game area ("place") lives in a package under `com.threerings.bang.*` (e.g. `saloon`,
  `store`, `ranch`, `gang`, `bounty`, `station`, `avatar`, and `game` for the actual board game),
  each split into `client/`, `data/`, and `server/` subpackages.
- `data/` holds `*Object` distributed objects (shared client/server state, auto-synced),
  `*Service` interfaces (client→server RPC), `*Marshaller`/`*Codes` classes.
- `server/` holds `*Manager` (place lifecycle) and `*Provider` (service implementations).
- `*Marshaller`, `*Dispatcher`, and `*Provider` stubs are **generated** from the `*Service`
  interfaces by the `client/shared` `genService` task — don't edit generated classes by hand;
  re-run `./gradlew client:shared:genService` after changing a `*Service` interface.

Note that most server-side game logic lives in `client/shared` (in the `*/server` subpackages),
not in the `server` module — the `server` module mainly adds persistence and the
`BangServer` bootstrap. Core gameplay (board game rules, effects, AI) is in
`com.threerings.bang.game.{client,data,server}` within `client/shared`.

**Database schema changes MUST ship as Flyway migrations** (Epic 3 Phase 7, baseline+freeze):
add a new `V<n>__description.sql` under `server/src/main/resources/db/migration/{bang,ooouser}/`
— never new DDL in Java repositories (`migrateSchema` overrides / Depot `SchemaMigration`
registrations are frozen legacy, kept only as the fresh-install bootstrap), and never edit an
already-applied migration (Flyway checksums them). See `docs/runbook.md` "Schema migrations".

The game world is divided into "towns" (`frontier_town`, `indian_post` — listed in the root
`build.gradle` `towns` ext property). Asset jars and avatar bundles are built per-town, and the
server runs in cluster mode as one node per town.

Resource pipeline: `assets/rsrc/` contains XML definitions (tutorials, avatar articles/aspects,
color definitions) that get compiled to serialized binary form, 3D models compiled from
`model.properties` files, and avatar component PNGs bundled into jars — all wired into
`assets:processResources`. Asset changes generally require re-running `./gradlew assets:deploy`.
