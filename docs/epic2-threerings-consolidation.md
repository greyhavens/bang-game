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
- Centralize every Three Rings / samskivert version into the root `build.gradle` `ext` block (several
  are still hardcoded per-module: `vilya:1.7.1`, `samskivert:1.11`, `getdown:1.4`, `ooo-util:1.5`,
  `ooo-user:1.5.1`, the `jmustache`/`pythagoras` transitives).
- Confirm the override path: `mavenLocal()` is already first in `repositories`, so a locally-published
  `com.x:lib:<ver>-bang1` transparently wins. Document the publish→override loop.
- Optionally stand up our own mirror of Three Rings' `maven-repo` (or a GitHub Packages registry) so
  forked builds resolve without relying on the upstream OOO Maven repo.

### Phase 2 — Fork & vendor (the insurance)
- Fork all 10 repos above to our GitHub; take local clones alongside the existing
  `narya`/`nenya`/`vilya` reference checkouts.
- Get each fork building from source on the JDK 21 toolchain and publishing to `mavenLocal` (verify
  bottom-up in dependency order).
- Pin Bang to the forked coordinates (via the Phase-1 version block) and confirm a full
  `./gradlew deploy` + a `bin/devtest` run still passes on the forked artifacts.

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
