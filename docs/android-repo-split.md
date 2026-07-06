# Android repo split — decision sketch

> **DECISION (Tim, 2026-07-06): Option C** — stay monorepo and fix the friction (CI path
> filters, an android build workflow, branch conventions), land the android branch's
> master-safe shared changes, and execute the split as Option B (Epic-2-style published
> artifacts + a baked-assets release bundle) once the shared-seam rate flattens — at the
> A1→A2 boundary or after two phases with zero main-repo PRs.

*Read-only analysis, 2026-07-06. Sources: `/home/tim/Repos/bang-Android` (worktree, branch
`android`, HEAD `188e9485d`, merge-base with master `935fcda96`) and
`/home/tim/Repos/bang-JRECutover` (master). Nothing was modified or built in either tree.*

---

## 1. Ground truth (measured)

### 1.1 Build coupling — what the android module actually consumes

`client/android` is a **standalone Gradle build**, deliberately excluded from the root
`settings.gradle` (AGP `com.android.application` cannot coexist with the root's
`java-library`-in-`allprojects`). Its own `settings.gradle` does
`includeBuild('../..')` — a **composite build** — and the app declares
`implementation 'com.threerings:shared:1.1.0'`, which Gradle substitutes to the sibling
`:client:shared` project (the root build sets `group = "com.threerings"`, `version = "1.1.0"`
on all projects, so coordinates already exist).

Dependency chain pulled through the composite:

```
client/android APK
  └─ com.threerings:shared  →  :client:shared        (bulk of game code, client AND server logic)
        └─ api :app          →  com.threerings.jme.*  (jME3 scenegraph/sprite framework)
              └─ api :bui    →  BUI toolkit on jME3
        └─ api narya/nenya/vilya/samskivert/getdown-core/ooo-util (external, GitHub Packages/mavenLocal)
        └─ api jme3-core/terrain/effects (Maven Central)
  └─ com.samskivert:depot (transitive=false, ByteEnum only)
  └─ jme3-android(+native), androidx.activity, ro.andob.androidawt + local awtcompat shim jar
```

**Not** consumed as projects: `:assets`, `:server`, `:client:desktop`, `:jme`, `:tools`.

File-level (non-Gradle) coupling into the parent tree — exactly two seams:

1. **`stageMessageBundles`** (client/android/build.gradle): copies `../../assets/rsrc/i18n/**`
   into the APK's Java resources at build time (small — .properties bundles).
2. **`tools/sideload-rsrc.sh`**: pushes `assets/build/staging/rsrc` — the **baked** output of the
   root build's `./gradlew assets:deploy` — to the device's external-files dir. The APK itself
   carries no game media.

Android-only mavenLocal forks, self-contained under `client/android/tools/`:
`patch-guice.sh` → `guice:6.0.0-bang1` (ART fix), `patch-nenya.sh` → `nenya:1.7.2-android1`
(strips the shadowed openal package). These travel with the android dir wherever it goes.

**One publish-side loose end:** the android branch bumps root `build.gradle`
`naryaVersion` 1.20 → **1.22** (GenStreamableTask flatten mode / GenStreamUtil). 1.21/1.22 exist
**only in `~/.m2`**; GitHub Packages has narya ≤ 1.20. Whatever option is chosen, narya 1.22 must
be published to GitHub Packages before these shared changes can merge to master (main CI resolves
from Packages).

### 1.2 Shared-code delta on the android branch (the drift-cost number)

`git diff master...android`, 8 commits:

| Area | Files | Nature |
|---|---|---|
| `client/android/**` (new module) | 57 | android-only |
| `client/shared/src/**` | **265** | **263 are generated** `genStreamable` flatten streamers (explicit `readObject`/`writeObject` via `GenStreamUtil`, committed output of a new one-shot Gradle task) — designed to be **byte-identical on the wire on HotSpot**, i.e. desktop/server-inert. Only **2 hand-edited**: `BangUI.java` (+17/−2), `openal/SoundManager.java` (`bang.disable_openal` guard + `Throwable` catch) |
| `app/` | 1 | `JmeApp.java` — `bang.disablePostProcessing` guard around the FilterPostProcessor chain (desktop never sets it) |
| `bui/` | 1 | `BTextFactory.java` — new `Provider` seam for a non-AWT text stack (null on desktop) |
| build files | 2 | root `build.gradle` (narya 1.22), `client/shared/build.gradle` (genStreamable task) |
| docs | 8 | android epics + edits to epics.md/epic5 |

Total outside `client/android`: **277 files, +8215/−21** — but the honest coupling number is
**6 hand-written files**; the other 263 are regenerable, mechanical, and deliberately
master-safe. Every shared change so far is guarded behind a system property or an unused-on-desktop
seam. Uncommitted worktree state (the other instance is live): 3 modified files + 1 new script,
all under `client/android/` — nothing shared.

Caveat for extrapolation: A1 isn't finished, and Epics A2/A3 explicitly plan more shared-side
work (re-enable post-processing on GLES, the texture-pipeline seam, touch input through `bui`).
Expect the hand-edited-seam rate to continue at a few files per phase for a while.

### 1.3 CI that fires on an android PR today (the named friction)

All main-repo workflows trigger on bare `pull_request:` — **no path filters anywhere**:

- `ci.yml` → `build` (compile+test all modules, JDK 21), `gltf-validate` (regenerate corpus from
  .mxml + byte-compare + Khronos validator), `integration` (full `deploy`, docker devstack
  MySQL + BangServer, bot-vs-AI game to GAME_OVER, ≤240 s)
- `security.yml` → OWASP dependency-check, **gating** (CVSS ≥ 7), NVD cache dance
- `claude-code-review.yml` → Claude review on every PR

Since `client/android` is not in the root `settings.gradle`, **none of these jobs even compile the
code an android-only PR changes.** They are pure cost on android PRs (except when shared files are
touched — then `build`/`integration` are genuinely load-bearing, see the recommendation).

### 1.4 The publish pattern available (Epic 2)

The forked Three Rings/samskivert stack is pure-Maven, built by `bin/rebuild-libs` into
mavenLocal (with sources), and published to **GitHub Packages on `claridtimo/bang-game`** —
13 maven packages live there today (narya, nenya, vilya, samskivert, depot, getdown-core, …).
Resolution order everywhere (root build AND the android standalone build): `mavenLocal()` →
`githubBang` Packages → Central. So the "edit lib → publish local → consumer picks it up
transparently" dev loop is already the house pattern.

What does **not** exist yet: the game's own modules (`shared`, `app`, `bui`) have coordinates but
**no `maven-publish` plugin and no publish workflow** — nothing publishes them anywhere. Adding
that is straightforward and pattern-conformant, but it is new work.

### 1.5 The asset question

`assets/rsrc` is **212 MB** of CC BY-NC media, and the android client consumes the **baked**
staging tree (model .j3o bake, compiled XML configs, avatar bundles — produced by the heavyweight
`assets:processResources`/`deploy` pipeline in the main repo), not raw source. Epic A2 then layers
an ETC2/ASTC re-encode and Play-Asset-Delivery pack layout **on top of** that bake, with a
permanent sideload/HTTP path because the NC license may rule out Play entirely
(`docs/epicA2-android-assets.md`, superseding `epic5-mobile-platforms.md` Part A Phase 3).

Implication for a split: **the android repo must not carry the media in git.** The APK only needs
`rsrc/i18n/**` at build time (tiny); everything else reaches the device out-of-band (sideload dir
today; PAD packs / self-hosted fetch later). So the options are:

- **Packaged artifact (recommended for any split):** main repo publishes a versioned
  `bang-assets-baked-<ver>` bundle (GitHub **Release asset** — the 2 GiB/file limit fits;
  Packages maven is wrong-shaped for a couple hundred MB of media) as a side-effect of
  `assets:deploy` on tag. The android repo's sideload script and future PAD packaging fetch it.
  The i18n subset can ride in the same bundle (the stage task just points at the unpacked tree
  instead of `../../assets/rsrc`).
- **Git submodule of the main repo:** gets the *source* rsrc, not the bake — the android repo
  would also need the whole asset pipeline (`:assets`, `:jme` model compiler, `:tools`). Drags
  ~all of the main repo back in. No.
- **Checked-in copy:** 212 MB duplicated NC media that immediately drifts from the desktop bake
  (the fidelity oracle in A2 compares against desktop renders — divergent inputs poison it). No.

In the monorepo this question doesn't exist: `./gradlew assets:deploy` is already the documented flow.

---

## 2. Option A — separate repo, fork the client code

Copy `client/shared`, `app`, `bui` (+ the root build glue they need) into the new android repo
alongside `client/android`; the main repo keeps its own copies for desktop + server.

**Day-1 migration**
1. New repo; copy the three modules + wrapper + the version ext block; port `genService`/`genStreamable` tasks.
2. Stand up android CI (assembleDebug + SDK setup).
3. Decide the asset feed (packaged bake artifact per §1.5 — the fork doesn't remove this need).
4. The android branch's existing shared edits become the first divergence *by construction*.

**Ongoing overhead — the drift cost, quantified**
- `client/shared` is not a client library — it is **client AND server logic sharing streamed
  classes**. The wire protocol is defined by these classes on both ends; the 263 generated
  streamers exist precisely because field-order determinism matters to the byte stream. A fork
  means every gameplay/effect/data change in master must be re-applied by hand or the android
  client **desyncs from the very server it logs into.** This is a correctness coupling, not merge
  debt.
- Master is the *active* half: the ongoing bug-fixing epics live in `client/shared`
  (effects/pieces — exactly the hot 263-file surface). Today's measured android-side divergence
  is only 6 hand files, but the drift that matters flows the *other* way: every master fix, forever.
- Streamer regeneration (`genStreamable`) must be re-run in both repos whenever a streamed class
  changes, and produce identical bytes.

**Parallel-instance workflow:** fully decoupled repos — but the android instance now silently
*owns a fork of the game logic*, and cross-repo protocol sync becomes a human coordination job
between two AI instances. The failure mode is invisible until a live logon breaks mid-game.

**Verdict:** the protocol coupling makes this the one clearly wrong option.

---

## 3. Option B — separate repo consuming versioned artifacts (extend Epic 2)

The android repo depends on `com.threerings:shared/app/bui:<ver>` as **published artifacts**; the
composite `includeBuild('../..')` line is deleted. Notably, `client/android/build.gradle` already
declares the dependency in coordinate form — the standalone build was explicitly designed for
this (its README: *"keeps this directory self-contained for an eventual split into its own
repository"*), so the build-side surgery is one settings.gradle line plus repo credentials.

**What the main repo must start publishing**
1. `maven-publish` on `:client:shared`, `:app`, `:bui` (jars + sources; `api` deps land in POMs
   automatically) → GitHub Packages, same `githubBang` repo the forks use.
2. A publish workflow: on tag (releases) + a `publishToMavenLocal` path for the dev loop; optionally
   run-number/`-SNAPSHOT` versions on master merges so the android repo can track head.
3. narya 1.22 to Packages (required in every option anyway).
4. The baked-assets bundle (§1.5) as a Release asset from `assets:deploy`.

**Day-1 migration**
1. **Land the android branch's shared changes in master first** (they're designed to be inert):
   one PR = 263 generated streamers + 6 hand files + genStreamable task + narya-1.22 bump, with
   narya 1.22 published to Packages beforehand. *(This step is identical in all options.)*
2. Add the publishing above; cut `shared/app/bui` `1.2.0` (or similar).
3. New repo from `client/android/**` + `docs/epicA*` + `docs/android-research-2026.json` — use
   `git subtree split`/`git filter-repo` on the android branch to keep the 8 commits of history.
4. Point `settings.gradle` at Packages instead of the composite; wire the asset-bundle fetch into
   `sideload-rsrc.sh` and `stageMessageBundles`.
5. Android repo CI: `assembleDebug` with SDK setup + a Packages read token. Main-repo CI is
   untouched and never fires on android work again.

**Dev-loop cost when a change spans both repos** (the recurring tax)
- Local: tolerable, because the Epic-2 loop already exists — edit shared code in the main repo,
  `./gradlew :client:shared:publishToMavenLocal` (seconds), rebuild the APK; the android build
  resolves `mavenLocal()` first, so the change wins transparently. Same loop devs already use for
  narya patches via `bin/rebuild-libs`.
- Cross-instance/CI: every shared seam becomes **main-repo PR → main CI → publish → version-bump
  PR in the android repo**. A1 needed 6 such seams; A2/A3 will need more (§1.2). Each one now
  requires the android instance to file work *in the other instance's repo* — coordination
  between the two AI instances gets harder, not easier, during exactly the phase where the seam
  rate is highest. It also removes the temptation to hack shared code ad-hoc (each seam gets
  reviewed against desktop impact) — a real quality upside, at a velocity cost.

**Fate of existing shared mods:** land in master (step 1); nothing diverges.

---

## 4. Option C — stay monorepo, fix the friction (do-least)

Address the three named pains directly; total change ≈ 50 lines of YAML + conventions.

**Day-1 steps**
1. **Path-filter the heavy CI** (one small PR to master):
   - Either `paths-ignore: ['client/android/**', 'docs/epicA*', 'docs/android-*']` on
     `ci.yml` and `security.yml` triggers, or (more precise, since shared+android mixed PRs must
     still run everything) a cheap `changes` job (`dorny/paths-filter`) that gates
     `build`/`gltf-validate`/`integration`/OWASP on non-android paths. `claude-code-review.yml`
     can stay universal or filter likewise.
   - Optionally add `android-ci.yml` with `paths: ['client/android/**']` running `assembleDebug`
     (self-contained: SDK via `android-actions/setup-android`, Packages token for the forked deps
     — the standalone build already resolves them, §1.1).
2. **Branch + ownership conventions:** android instance works on `android`/`android-*` branches
   only; CODEOWNERS mapping `client/android/**` to the android stream; a paragraph in both
   worktrees' CLAUDE.md ("shared-module edits ship as small master PRs, android branch merges
   master back"). The worktree mechanics already isolate working trees/indexes; the two instances
   only truly contend if they push the same branch, which the convention forbids.
3. **Land the shared changes in master** (same step-1 PR as Option B) and merge master back into
   `android` — the android branch then carries *only* `client/android/**` + its docs, which keeps
   the interleaving complaint structurally small: master history only sees android-related
   commits when they're deliberate shared-seam PRs, which desktop CI *should* gate.

**Ongoing overhead:** near-zero mechanically. The real cost is discipline — the android instance
must keep routing shared edits through master instead of letting the `android` branch drift; the
measured branch state (263 generated + 6 guarded hand files, all master-safe by design) shows the
other instance is already working exactly this way.

**Parallel-instance workflow:** unchanged day-to-day; android PRs stop triggering the 4 heavy
jobs; both instances keep one source of truth for shared code and one asset pipeline
(`assets:deploy` stays a local gradle invocation, no artifact plumbing needed).

**Fate of existing shared mods:** land in master; the branch re-syncs.

---

## 5. Recommendation (ranked for a hobby project, two parallel AI instances)

**C now → B at a natural boundary. Never A.** Ranking: **C > B ≫ A.**

Deciding factors, explicit:

1. **The coupling is currently high-frequency, low-surface.** Only 6 hand-edited shared files so
   far — but they arrived steadily across A1's phases, and A2/A3 plan more shared-side seams
   (GLES post-processing, texture pipeline, touch input in `bui`). While the seam rate is high, a
   split repo (B) converts each seam from *one commit* into *a two-repo publish/bump dance
   between two AI instances*. Split when the seam rate drops — the A1→A2 boundary is the natural
   checkpoint, or "two consecutive phases with zero main-repo PRs" as the concrete trigger.
2. **Wire-protocol coupling eliminates A.** `client/shared` is server code too; forked copies of
   streamed classes desync client from server. A is strictly worse than B at similar cost.
3. **The named friction is cheap to kill in-place.** CI-on-android-PRs and branch interleaving
   are ~50 lines of path filters plus a branch convention — Option C removes the actual stated
   pain this week without foreclosing anything.
4. **Everything B needs is worth building anyway, incrementally, from inside C.** Publishing
   `shared/app/bui` + a baked-assets Release artifact are independently useful (the asset bundle
   is essentially what PAD packaging wants as input), and the standalone android build was
   *designed* to detach (README says so; the composite is one line). C now doesn't defer B's cost
   — it amortizes it, and the eventual split gets cheaper over time, not dearer.
5. **Assets decide the tie-breaker at split time.** The android client needs the *baked* rsrc
   tree produced by main-repo gradle. In the monorepo that's the existing `assets:deploy`; in a
   split repo it's a versioned ~200 MB Release-asset download plus one more version to keep
   bumped. Manageable, but it's pure added machinery until the split buys something.

**Sequenced plan:** (1) publish narya 1.22 to Packages and land the android branch's shared
changes as a master PR — required under every option and shrinks the android branch to
`client/android/**`; (2) ship the CI path filters + android `assembleDebug` workflow + branch
conventions; (3) add `maven-publish` for `shared/app/bui` and the baked-assets Release artifact
opportunistically; (4) execute the split to Option B once the shared-seam rate flattens — at that
point it's a `git filter-repo` of a directory that already builds standalone.
