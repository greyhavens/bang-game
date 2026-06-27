# Epic 2 — Three Rings code consolidation

Bang! Howdy is built on a stack of Three Rings Design / Michael Bayne (`samskivert`) libraries,
consumed as published Maven artifacts. Three Rings became Grey Havens and is effectively dormant;
these artifacts are unlikely to see new releases. This epic brings the stack under our own control:

1. **Insurance** — fork every upstream repo so the loss of the Three Rings / samskivert GitHub orgs
   (or their Maven repo) can never block us, and keep local clones of the forks.
2. **Patchability** — be able to fix/modernize these libs ourselves (JDK bumps, security, behavior
   tweaks) instead of being stuck on the last published version.
3. **Consolidation (optional, later)** — vendor and combine the layers into a unified, modernized
   source tree aligned with the Epic-1 jME3/LWJGL3 world.

**None of these libraries touch jME/LWJGL** (verified against the resolved runtime classpath — the
only native graphics/audio deps come from `jme3-lwjgl3`). So this epic is about *ownership and
longevity*, not the engine cutover. The only engine-coupled Three Rings code is the vendored `jme`
fork module, which Epic 1 / Phase 8 deletes.

## The dependency set (what Bang relies on)

Resolved across all modules (`client:shared`, `server`, `client:desktop`, `tools`, `assets`,
`app`, `bui`). Versions are the resolved/winning versions; `*-tools` artifacts are produced by their
parent repo (forking the parent covers them).

| Maven coordinate | Resolved ver | GitHub repo to fork | Notes |
|---|---|---|---|
| `com.threerings:narya` | 1.19 | `threerings/narya` | Presents distributed-object / networking framework |
| `com.threerings:narya-tools` | 1.19 | `threerings/narya` | code-gen tools; same repo as narya |
| `com.threerings:nenya` | 1.7.2 | `threerings/nenya` | 2D/iso/3D media + sound (`com.threerings.openal`) |
| `com.threerings:nenya-tools` | 1.7.2 | `threerings/nenya` | asset tools; same repo as nenya |
| `com.threerings:vilya` | 1.7.1 | `threerings/vilya` | MMOG components (parlor/scene/puzzle) |
| `com.threerings:ooo-util` | 1.5 | `threerings/ooo-util` | OOO general utilities |
| `com.threerings:ooo-user` | 1.5.1 | `threerings/ooo-user` | user/account management |
| `com.threerings:getdown` | 1.4 | `threerings/getdown` | auto-updater / installer |
| `com.samskivert:depot` | 1.8 | `threerings/depot` | relational persistence (ORM) — note `com.samskivert` groupId, `threerings` repo |
| `com.samskivert:samskivert` | 1.11 | `samskivert/samskivert` | foundation utilities — everything depends on it |
| `com.samskivert:jmustache` | 1.4 | `samskivert/jmustache` | Mustache templates (transitive) |
| `com.samskivert:pythagoras` | 1.3.2 | `samskivert/pythagoras` | geometry primitives (transitive) |

**10 repos to fork** (the two `*-tools` are subprojects of narya/nenya). 7 under `github.com/threerings/`,
3 under `github.com/samskivert/`. `react` (`threerings/react`) is **not** a Bang dependency — skip it.
`threerings/ooo-build` (Ant infra) is not used by the modern Gradle build, but the older repos may
reference it to build from source — grab it only if a fork won't build standalone.

Dependency order (fork/build bottom-up): `samskivert` → `ooo-util`, `pythagoras`, `jmustache` →
`depot`, `narya` → `nenya`, `vilya`, `ooo-user` → `getdown` → (Bang).

## Phases

### Phase 1 — Dependency-control mechanism
Make any single lib forkable + patchable + swappable with a one-line change, without restructuring
the build. Low risk, can land any time.
- **[DONE]** Centralize every Three Rings / samskivert version into the root `build.gradle` `ext`
  block. All direct deps now resolve from `ext`: `naryaVersion 1.19`, `nenyaVersion 1.7.2`,
  `vilyaVersion 1.7.1`, `depotVersion 1.8`, `samskivertVersion 1.11`, `oooUtilVersion 1.5`,
  `oooUserVersion 1.5.1`, `getdownVersion 1.4`. (`jmustache`/`pythagoras` are pulled in transitively,
  not declared directly — pin them in `ext` too only if/when a fork needs an explicit override.)
- **[DONE]** Override path confirmed + documented (see below): `mavenLocal()` is first in
  `repositories`, so a locally-published `com.x:lib:<ver>-bang1` transparently wins.
- **[TODO, optional]** Stand up our own mirror of Three Rings' `maven-repo` (or a GitHub Packages
  registry) so forked builds resolve without relying on the upstream OOO Maven repo.

#### The publish → override loop (how to run a patched lib without touching Bang's build)
Each Bang module's `repositories` lists `mavenLocal()` **before** `mavenCentral()`, and Gradle picks
the first repo that has the requested coordinate, so anything published to the local `~/.m2`
repository wins for that exact `group:artifact:version`. To run a locally-patched copy of, say,
`samskivert`:

1. In your fork of the lib, bump its version to a Bang-private suffix (e.g. `1.11-bang1`) and
   `./gradlew publishToMavenLocal` (or `mvn install`) — it lands in `~/.m2/repository`.
2. In Bang's root `build.gradle` `ext`, change the one line: `samskivertVersion = "1.11-bang1"`.
3. `./gradlew deploy` — Gradle resolves `com.samskivert:samskivert:1.11-bang1` from `mavenLocal`;
   every module that declares `${samskivertVersion}` follows automatically.

To revert, set the version back to the upstream string. Using a distinct `-bangN` suffix (rather than
re-publishing the upstream version number) keeps the local artifact from silently shadowing the real
one and makes "are we on a patched build?" obvious from the version string.

### Phase 2 — Fork & vendor (the insurance)  *(scoped 2026-06-27)*

**Recon (2026-06-27):** all 10 upstream repos exist and are reachable; **every one builds with Maven**
(`pom.xml`; several also carry a legacy Ant `build.xml` we can ignore). No local reference checkouts
exist yet. The repos are *alive but unreleased* — recent commits (2026) but the published Maven
artifacts are still the old 1.x versions — so the fork-as-insurance + patchability rationale holds, and
any JDK-21 build fixes we make may even be upstreamable.

**Decisions (locked):**
- **Versioning:** keep each lib's **upstream version** and republish our build; `mavenLocal()` is first
  in `repositories` so our `~/.m2` copy wins per-dev. Reserve the `-bangN` suffix for when we actually
  *patch* a lib (then bump just that lib + its dependents). *(See the remote-registry caveat below.)*
- **Order:** **critical path first** — fork+build the directly-declared core, prove Bang runs, then add
  the rest.
- **Publish target:** `mavenLocal` for per-dev, **plus a remote registry (GitHub Packages)** so CI /
  other machines resolve the forks.
- **Patching:** build as-is; only if a fork won't compile on JDK 21, make the **smallest** fix (bump
  the maven-compiler `release`/`source`/`target`) and note it. No proactive modernization (that's
  Phase 5).
- **Fork destination:** `github.com/claridtimo/<repo>` (same account as the `bang-game` fork).

**Build the stack with Maven, bottom-up.** `mvn install` publishes straight to `~/.m2` (= mavenLocal),
which is exactly the Phase-1 override path. Per repo: fork on GitHub → clone locally under
`/home/tim/Repos/` → `mvn -DskipTests install` on the JDK 21 toolchain → fix a compiler target only if
it fails → confirm the artifact appears in `~/.m2`.

*Wave 1 — critical path (Bang's directly-declared deps + what they need to build):*
1. `samskivert/samskivert` (foundation)
2. `threerings/ooo-util`
3. `threerings/depot`, `threerings/narya` (+ `narya-tools`, same repo)
4. `threerings/nenya` (+ `nenya-tools`), `threerings/vilya`, `threerings/ooo-user`

After Wave 1: a full `./gradlew deploy` + `bin/devtest` game run should pass with the core resolving
from our local builds and `getdown`/`pythagoras`/`jmustache` still coming from Maven Central
(transitive — fine for the checkpoint).

*Wave 2 — finish the set:*
5. `threerings/getdown`
6. `samskivert/pythagoras`, `samskivert/jmustache` (leaf transitives)

Then a final `deploy` + `devtest` with **all** Three Rings / samskivert artifacts resolving from our
forks — that's the Phase-2 acceptance bar.

**Remote-registry caveat (confirm at the 2c sub-step):** keeping upstream version numbers means
`com.samskivert:samskivert:1.11` would name *two* different artifacts (ours on GitHub Packages vs the
real one on Central), resolved purely by **repository order**. For per-dev `mavenLocal` that's safe
(it's first). For the shared registry we'll add the GitHub Packages repo **ahead of** `mavenCentral`
in every module's `repositories` and authenticate via a `~/.gradle` token. If that same-version
shadowing proves fragile in CI, fall back to the `-bangN` suffix for the remote-published copies.

**Risks:** JDK-21 compile compat on the Java-6/8-era poms is the main one (rejected `-source/-target`
< 7 → minimal compiler-target bump per fork); a repo or two may resist a standalone Maven build if it
expects the `ooo-build` Ant infra (grab `threerings/ooo-build` only if needed). Execution is strictly
**sequential** (dependency order), est. ~½–1 day dominated by per-lib JDK-21 surprises.

#### Status — DONE 2026-06-27 (mavenLocal; remote registry still TODO)
All 10 libs forked to `claridtimo`, cloned under `/home/tim/Repos/`, built from the **release tag
matching Bang's pinned version** (repo HEADs are ahead, e.g. samskivert HEAD = `1.12-SNAPSHOT`), and
`mvn install`-ed to `~/.m2`. Bang **builds + an autoplay game runs** on the forked stack
(`./gradlew deploy --refresh-dependencies` → mavenLocal wins; In Play reached, no classload errors).
Maven isn't on the box — used a standalone Apache Maven 3.9.9 under `/home/tim/Repos/.tools/` (no sudo).

| Lib | Version built | Build note |
|---|---|---|
| samskivert | 1.11 | clean (`--release 10`) |
| ooo-util | 1.5 | clean |
| depot | 1.8 | clean |
| narya (+ narya-tools) | 1.19 | clean (multi-module core+tools) |
| nenya (+ nenya-tools) | 1.7.2 | clean |
| vilya | 1.7.1 | clean |
| ooo-user | 1.5.1 | clean |
| pythagoras | 1.3.2 | **patched** — compiler-plugin 2.3.2→3.13.0, source/target 6→8 (branch `bang-jdk21`) |
| jmustache | 1.4 | **patched** — compiler-plugin 2.3.2→3.13.0, source/target 5→8 (branch `bang-jdk21`); not on Bang's current classpath, forked for completeness |
| getdown | **1.4 → 1.8.10** | applet-era 1.4 won't build on JDK 21 (JSObject/source 1.5); **bumped** to modern `com.threerings.getdown:getdown-core:1.8.10`; one Bang API rename (`getJVMPath`→`getJVMBinaryPath`) |

Fork patches are committed + pushed to the `claridtimo` forks on a `bang-jdk21` branch so the build
fixes survive a re-clone (durable insurance). Bang-side changes are on branch
`epic2-dependency-control` (Phase-1 ext centralization + the getdown bump).

**Remote registry — DONE 2026-06-27.** All 12 artifacts published to GitHub Packages at
`maven.pkg.github.com/claridtimo/bang-game` (one endpoint, so Bang needs a single repo entry; they
show in the bang-game repo's Packages list). Published with `mvn deploy:deploy-file` from temp copies
(the deploy plugin refuses a `-Dfile` inside `~/.m2`). Bang's root `build.gradle` adds the `githubBang`
maven repo **ahead of `mavenCentral`**, creds from `~/.gradle/gradle.properties` (`gpr.user`/`gpr.token`)
or `GITHUB_ACTOR`/`GITHUB_TOKEN` in CI. **Proven end-to-end:** with samskivert hidden from mavenLocal +
the Gradle cache, resolution fetched `samskivert-1.11` from `maven.pkg.github.com` and a full deploy
stayed green. Auth: the publishing token needs `write:packages` (added via `gh auth refresh`).

Local machine config (NOT in the repo — secrets): `~/.m2/settings.xml` (server id `github-bang`, for
`mvn deploy`) and `~/.gradle/gradle.properties` (`gpr.user`/`gpr.token`, for Gradle resolution), both
`chmod 600`. To republish a lib: `mvn deploy:deploy-file ... -DrepositoryId=github-bang -Durl=…/bang-game`
from a temp copy of the jar+pom. Standalone Maven lives at `/home/tim/Repos/.tools/apache-maven-3.9.9/`.

**Phase 2 is COMPLETE** — Bang builds + runs on our forked Three Rings / samskivert stack, resolvable
from both per-dev `mavenLocal` and the shared GitHub Packages registry, with no hard dependency on the
upstream Three Rings GitHub orgs or Maven repo. (Caveat unchanged: same version numbers mean our copies
win by repo order, not by distinct coordinates; switch to `-bangN` if CI ever finds the shadowing
fragile.)

### Phase 3 — Source-build integration (optional)
- Wire the forks in as a Gradle **composite build** (`includeBuild`) or source modules so we can
  edit-and-run without a publish step — direct source-level debugging across the whole stack.

### Phase 4 — Consolidation / combining the layers
- Where it pays off, merge the libs into a unified source tree (dedupe overlapping utils, single
  version, atomic cross-lib refactors). This is the heavy "combine the layers" undertaking — do it
  deliberately once Epic 1 is stable.

### Phase 5 — Modernize & trim
- With the stack owned, modernize: bump JDK target in lockstep with Bang, replace samskivert utilities
  superseded by the JDK stdlib / Guava where sensible, delete dead code (PlayN-era paths, unused
  subsystems), and align anything that interacts with the rendering/audio host with the Epic-1
  jME3/LWJGL3 stack.

## Acceptance
Bang builds and runs (full `deploy` + `bin/devtest` game) entirely on our own forked, source-built
copies of the Three Rings / samskivert stack, with no dependency on the upstream Three Rings GitHub
orgs or Maven repo.
