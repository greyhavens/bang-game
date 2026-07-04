# Epic 3 — Production hardening, CI & operations

Epics 1 and 2 make the game *run on a modern, owned stack* (jME3/LWJGL3 + forked Three Rings libs).
This epic makes it *operable and safe to ship*: the game currently has **no CI**, runs its
client↔server traffic **in the clear** (Narya on port 47624, MD5-era auth), creates its DB schema
ad hoc on first run with **no migrations**, and is deployed/configured by hand. None of that is
covered by the engine cutover or the dependency-ownership work.

The throughline:

1. **Verifiability** — turn the Phase-5 test harnesses (offscreen render-to-PNG + `SnapshotDiff`,
   the headless Narya bot) into an automated gate that runs on every change, instead of by hand.
2. **Safety** — encrypt the transport, review auth/credential storage and the wire trust boundary,
   and know (and patch) our dependency CVEs.
3. **Operability** — reproducible containerized environments, externalized config/secrets,
   observability, real DB lifecycle (migrations/backups), and a documented deploy.

## Relationship to Epics 1 & 2

Additive, and mostly independent — but with a few ordering ties:
- **Phase 1 (CI build+test)** can land any time, even before Epic 1 finishes; it protects the
  cutover work in flight.
- The **integration/visual-regression gates** (Phase 3) depend on the Phase-5 harnesses (Epic 1) and
  the containerized server (Phase 2 here).
- **CVE patching** (Phase 5) depends on Epic 2 ownership to actually fix vulnerable upstream libs;
  the *scanning* can start now.
- A little **cleanup overlaps Epic 1 Phase 8** (the residual libGDX keycode dependency) — do it in
  whichever epic reaches it first; cross-reference, don't duplicate.

## Phases

### Phase 1 — CI foundation
The entry gate. Lands immediately, no prerequisites.
- GitHub Actions workflow: JDK 21 toolchain, Gradle cache, `./gradlew build test` on every PR and
  push to `JRECutover`/`master`.
- Surface unit-test results and the `client:shared` `genService` / `compileModels` steps so a broken
  generator or model bake fails the build, not a later manual run.
- Branch protection: required green check before merge.

### Phase 2 — Containerized devtest + server
A reproducible environment — also the substrate CI's integration jobs and ops both need.
- Docker Compose for `bin/devtest`: a MySQL service (with the `bang`/`ooouser` DBs seeded) + the
  `BangServer` node, wired to the `etc/test/` config.
- One-command bring-up that mirrors the documented local setup, so "works on my machine" and "works
  in CI" converge.
- Publish the server image build (so deploys and CI pull the same artifact).

### Phase 3 — Integration & visual-regression gates in CI
Cash in the Phase-5 testability investment. Depends on Phases 1–2 + the Epic-1 harnesses.
- Spin up the containerized server (Phase 2) in CI and run the headless **Narya bot** smoke test
  (`tools:bot-client`) to assert an AI-vs-AI game reaches `GAME_OVER` with a winner/points.
- Render a fixed set of models/scenes via the offscreen harness and gate on **`SnapshotDiff`**
  against checked-in `baseline/` PNGs; upload diff images as build artifacts on failure. The
  Phase-7d `verifyVisuals` golden suite (`tools:j3o-converter`) already implements this gate — the
  remaining work is hardening it for unattended CI (below).
- **Harden `verifyVisuals` for CI** (review follow-ups from the Phase-7d landing):
  - **Calibrate tolerances on the actual CI image.** The committed goldens were captured on one
    host's GL driver; the deterministic model/prop/scene renders use a tight 0.005 mean tolerance
    that a different CI GPU/Mesa version may exceed (MSAA sample pattern / float rounding). Validate
    on the real CI runner and loosen per-entry tolerances only as far as needed to stay green on an
    unchanged render.
  - **Add a per-render timeout.** `VerifyVisuals.render()` does `Process.waitFor()` with no timeout;
    a hung GL init on a headless agent hangs the whole gate. Use a bounded wait, then destroy the
    child and count it as a failure.
  - **Surface failure diagnostics.** The forked render's merged stdout/stderr is currently drained
    and discarded; on `RENDER-FAIL` capture and print the tail so a CI failure is debuggable without
    a local repro.
  - **Dedup the diff metric.** `VerifyVisuals.diff()` copy-pastes the `SnapshotDiff` mean-per-channel
    metric; extract a shared `SnapshotDiff.compare(...)` so the gate and the standalone tool can't
    silently diverge.
- Establish the baseline-refresh workflow (how a legitimate visual change re-blesses the baseline).

### Phase 4 — Transport & credential security  *(TLS + credentials DONE; token review remains)*
Close the in-the-clear gaps.

**Full-session TLS — DONE.** The entire Narya client↔server TCP object stream (login, chat,
gameplay — *all* of it, not just credentials) is now wrapped in **TLS 1.3**. Modern games encrypt
the whole session via vetted middleware over an ephemeral key exchange (Valve GameNetworkingSockets =
AES-GCM-256 + Curve25519; GameLift = TLS 1.2 + DTLS 1.2); we follow that consensus with TLS rather
than rolling our own AEAD on the existing (and, in Bang, never-enabled) RSA→AES handshake.
- Implemented in the forked **narya** (`1.20`, published to GitHub Packages) via
  [marianobarrios/tls-channel](https://github.com/marianobarrios/tls-channel) — a thin, vetted
  `ByteChannel`-over-`SSLEngine` wrapper (MIT, zero transitive deps). `TlsContextFactory` builds the
  contexts; `Connection` gained a `ByteChannel` indirection so the selector loop registers the raw
  socket but does app I/O through the TLS channel. Server side maps tls-channel's non-blocking
  `NeedsRead`/`NeedsWrite` onto the existing overflow-queue / interest-op machinery; the client
  (`BlockingCommunicator`) uses tls-channel's blocking mode (near drop-in for its reader/writer
  threads). Inter-node **peer** connections also speak TLS (`PeerManager.setPeerClientContext`).
- **Strictly opt-in:** a null `SSLContext` = byte-for-byte the original plaintext path. Enabled via
  `server_tls` + keystore/truststore config (`server.properties` server-side, the shipped
  `deployment.properties` client-side). `bin/gen-tls-certs` generates a self-signed server keystore +
  a **pinned** client truststore (no public CA for a self-hosted/community deployment).
- **No DTLS:** Bang never binds the UDP datagram channel (server `getDatagramPorts` is empty, the
  client passes none), so DTLS would protect dead code. `DatagramChannelReader` instead **fails
  closed** — refuses to bind datagram ports while TLS is on, so plaintext UDP can never silently run
  alongside the encrypted TCP session.
- **Verified:** the headless bot played a full all-AI game to GAME_OVER over TLS, and the GUI client
  logs on + chats over TLS 1.3. Cluster-peering note: the dev two-node cluster still can't fully peer,
  but only because of the pre-existing `NodeRecord` `LocalDateTime`↔`Timestamp` Connector/J 8 bug
  (unrelated to TLS); peer connections were confirmed to attempt TLS (no plaintext-vs-TLS errors).

**Credential storage — DONE (Epic 2).** `ooo-user`'s `Crypto` uses **Argon2**
(`de.mkammerer:argon2-jvm`) with `verify`/`needsRehash`, not MD5.

**Still remaining:** review the session-token / admin-token / MAC-ident scheme (the path the bot
client mirrors) for replay and spoofing. Note TLS now protects these tokens in transit (the main
over-the-wire replay vector), but the at-rest/issuance scheme review is still open.

### Phase 5 — Wire trust boundary & dependency CVEs — DONE
- **Wire boundary hardened** (PRs #22–#27): streamed-class whitelist, gang service auth-bypass +
  SQLi fixes, gang dobj subscription access control, poster/matchmaking arg validation, null-config
  deserialization guards, criterion bitmask clamps. Three subtle items deliberately backlogged
  (task #61: colorization entitlement, ident-MAC demote, peer nonce'd-HMAC).
- **CVE patching**: commons-digester 1.8→2.1 (kills the log4j-1.x chain), mysql-connector-j 8.4.0,
  commons-io 2.16.1, forced commons-beanutils 1.11.0 (CVE-2025-48734, #34) + protobuf-java 3.25.5.
- **OWASP dependency-check** scans the resolved classpath in CI (aggregate, `failBuildOnCVSS=7.0`,
  NVD_API_KEY + weekly-keyed NVD cache, suppressions in `config/owasp-suppressions.xml`). GATING
  via its own `security.yml` workflow (#35) — see that file for the cancel-safety/cache design.
  Baseline: clean above the gate; two below-gate Mediums (ant 1.7.1, CVE-2020-1945) on the tools
  chain remain as backlog.

### Phase 6 — Ops & observability — DONE
- **Config externalized** (#28): `BANG_<KEY>` env-var bridge over the baked properties (a key must
  be present in the baked config to be overridable — the `.dist` templates keep secrets
  present-but-blank on purpose); TLS keystore/truststore passwords decoupled.
- **Logging** (#29, #31): one-line (default) or JSON-lines (`server_log_format=json`) formatting on
  stdout for container log collection; a default uncaught-exception handler; the 15-minute state
  report mirrored to stdout alongside the `state_<node>.log` audit file.
- **/health + /metrics** (#32): `OpsHttpServer` on the JDK http server (zero deps), loopback-only,
  `server_mgmt_port`/`server_mgmt_host` (+ per-node override). `/health` 200 = init fully complete;
  `/metrics` = Prometheus text (JVM, sessions/connections, ConMgrStats counters, node label).
- **Deploy + runbook** (#33): `docs/runbook.md`, `deploy/prod/` systemd template unit
  (`bang@<node>`, env-file secrets, `ProtectSystem=strict` with the audited write surface) +
  launcher + env examples; devstack container healthcheck against /health makes the CI
  `compose up --wait` a real init-complete gate; devstack entrypoint converted from sed rewrites
  to explicit env-derived appended overrides.

### Phase 7 — Database lifecycle — DONE (backups + baseline/freeze scope)
- **Backups (#37):** `deploy/prod/backup.sh` + `restore.sh` + nightly `bang-backup.timer` —
  per-database dumps (`--single-transaction`, split-host aware via `db-common.sh`), audit-log
  tarball, truncation-detecting integrity check, retention pruning; verified with a real
  drop-both-DBs restore + bot game on the restored data.
- **Flyway, baseline + freeze:** Flyway (`flyway-core`/`flyway-mysql`) runs at the top of
  `BangServer.main` — before the legacy repositories' constructor-time DDL — with an empty `V1`
  baseline marker per database (`baselineOnMigrate` for pre-existing installs) and fail-closed
  startup on migration errors. **All new schema changes ship as versioned SQL** under
  `server/src/main/resources/db/migration/{bang,ooouser}/`; the two legacy auto-migrators are
  frozen as the fresh-install bootstrap (full cutover deliberately rejected as high-risk — see
  recon below). Policy documented in the runbook + CLAUDE.md.
- Deferred: Depot connection pooling / sizing review (revisit with real load data).
- **Recon done (2026-07-04):** TWO auto-migrating stacks own the schema today — Depot
  (`DepotSchemaVersion`-versioned, `CanMigrate.ALLOWED`, minority of tables) and legacy samskivert
  JDBC (majority: PLAYERS/ITEMS/GANGS/… + the whole `ooouser` DB; hand-written DDL `String[]` +
  conditional ALTERs in each repo's `migrateSchema`, run at *constructor* time, no version
  tracking, soft-fail). No `.sql` defines the game-server schema (the only `.sql` in the repo is
  devstack DB/user seeding + the separate bangmeta Rails app's card data); all Bang tables
  physically live in the one `bang` DB; ooo-user's versioned Depot user records are dormant. Non-DB backup surface: `log/`
  item/state audit logs + `data/boards`. Proposed shape (to be decided): backups first, then
  Flyway **baseline + freeze for new changes only** — a full cutover means neutralizing both
  auto-migrators across 8+ repos and is high-risk.

### Phase 8 — Cleanup & idiom modernization
- Finish the **libGDX excision**: `gdx 1.5.4` (2015) still lingers in `jme`/`bui` only for the
  `com.badlogic.gdx.Input.Keys` keycode constants — replace with our own/jME3 codes and drop the
  dependency. (Coordinate with Epic 1 Phase 8.)
- Content-pipeline note: the authoring source is still the fork `model.dat` binary (baked to `.j3o`);
  sketch a standard-format (glTF) path for any *new* content. Only pursue if content will change.
- Opportunistic modern-Java idioms in the Bang app code (records/switch-expressions/`var`, stdlib
  over superseded samskivert utils) — low priority, ongoing; the *library* side is Epic 2 Phase 5.

## Acceptance

Every change is gated by CI that builds, unit-tests, runs the headless game smoke test, and checks a
visual-regression baseline; the client↔server transport is encrypted and credential storage is
modern-hashed; dependencies are CVE-scanned and patchable on our forks; the server + DB run from
reproducible containers with externalized config, versioned schema migrations, and a documented
deploy.
