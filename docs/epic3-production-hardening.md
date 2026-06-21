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

### Phase 4 — Transport & credential security
Close the in-the-clear gaps.
- Wrap the Narya client↔server connection in **TLS** (or document the trusted-network assumption if
  it is intentionally LAN-only).
- Review credential storage in `OOOAuthenticator` / `ooo-user`: confirm passwords are salted +
  modern-hashed (bcrypt/argon2), not MD5; migrate if not.
- Review the session-token / admin-token / MAC-ident scheme (the path the bot client mirrors) for
  replay and spoofing.

### Phase 5 — Wire trust boundary & dependency CVEs
- Audit the Narya **wire deserialization** trust boundary: what a hostile client can stream into the
  server, and what validation gates it (Presents `InvocationService` args, dobj subscriptions, the
  unit/prop config deserialization the bot notes can NPE).
- Add **dependency vulnerability scanning** (OWASP dependency-check / Gradle `dependencyCheck` or
  GitHub Dependabot) to the Phase-1 CI; triage the 2005-era transitive graph.
- Patch findings via the Epic-2 forks (this phase is the concrete consumer of Epic-2 ownership).

### Phase 6 — Ops & observability
- Externalize config/secrets out of the baked `etc/<deployment>/` properties (env/secret-store), so
  images are environment-agnostic.
- Structured logging (replace ad-hoc `Log` output where it matters), basic metrics, and crash/error
  reporting for the server nodes.
- A documented deploy + runbook for the cluster-per-town topology, building on the Phase-2 images.

### Phase 7 — Database lifecycle
- Replace create-tables-on-first-run with a **schema migration framework** (Flyway/Liquibase) so DB
  changes are versioned and repeatable.
- Review Depot connection pooling / sizing under the cluster-per-town node model.
- A backup/restore story for the `bang` + `ooouser` databases.

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
