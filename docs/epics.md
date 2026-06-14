# Bang! Howdy modernization — Epics

The modernization is organized into **Epics**, each a self-contained campaign with its own ordered
phases and acceptance bar.

## Epic 1 — jME3 / LWJGL3 engine upgrade  *(in progress)*
Move the running game off the vendored 2005-era jMonkeyEngine fork + LWJGL2 + libGDX onto stock
**jMonkeyEngine 3.9 + LWJGL3**. Plan + phase status: [`jme3-cutover-plan.md`](jme3-cutover-plan.md)
(Phases 1–8). Predecessor groundwork (migration map, model loader, board converter, BUI seam) is
already merged to master.

## Epic 2 — Three Rings code consolidation  *(planned)*
Bring the Three Rings / samskivert dependency stack under our own control — fork every upstream lib
as insurance against the Three Rings GitHub/studio going away, then optionally vendor, combine, and
modernize the layers to align with the Epic-1 jME3/LWJGL3 world. Plan + phases:
[`epic2-threerings-consolidation.md`](epic2-threerings-consolidation.md).

Epic 2 depends on Epic 1 only loosely (its Phase 1 — dependency-control mechanism — can land any
time; the heavier vendoring/combining phases are best done once the game is stable on jME3).

## Epic 3 — Production hardening, CI & operations  *(planned)*
Make the game operable and safe to ship: wire the Phase-5 test harnesses (render-to-PNG +
`SnapshotDiff`, the headless Narya bot) into **CI**, encrypt the currently-plaintext client↔server
transport and review auth/credential storage, scan & patch dependency CVEs, and stand up
reproducible containerized environments with externalized config, versioned DB migrations, and a
documented deploy. Plan + phases: [`epic3-production-hardening.md`](epic3-production-hardening.md).

Additive to Epics 1 & 2 and mostly independent: CI (Phase 1) can land any time; the
visual-regression gates build on the Epic-1 harnesses; CVE patching consumes Epic-2 ownership.

## Epic 4 — Native model toolchain (fork eradication)  *(planned)*
Replace the build-time jME-fork asset compiler with a jME3-native one so the vendored `jme` fork
module can be deleted entirely. Epic 1 got the fork off the **runtime** (the game ships/loads zero
fork code), but the fork remains the **build-time** model/particle compiler (`.mxml`/`model.properties`
→ `model.dat` via the fork's `CompileModelTask`, then `model.dat` → `.j3o` via our `ModelConverter`
which calls the fork's `BinaryImporter`). This epic writes a native `.mxml`→jME3 importer to retire
both fork stages, then removes the module. Plan + phases:
[`epic4-native-model-toolchain.md`](epic4-native-model-toolchain.md).

Strictly after Epic 1 (which keeps the fork as a walled-off build-time compiler — "Option A"). Its
verification leans on the Epic-1 render harness (`RenderModelToPng` + `SnapshotDiff`): the
current fork-wrapper `.j3o` are a verified-correct oracle to diff a native importer against.

## Epic 5 — Mobile platforms (Android port; iOS feasibility)  *(speculative — not planned)*
Take the game to mobile. The desktop trio (Linux/Windows/macOS) is one easy tier only because it
shares the `jme3-lwjgl3` desktop backend; mobile is not free. **Android** is a real but well-trodden
port (swap to the maintained `jme3-android` GLES backend, touch input, activity lifecycle, no Swing,
APK packaging + asset bundling). **iOS** is the genuinely hard one and Epic 1 makes it *harder* (it
removes libGDX, the one component with a first-class iOS story): no JVM (AOT via MobiVM), no
maintained jME3 iOS renderer, Guice/Narya reflection-vs-AOT friction, and a **licensing blocker**
(the CC *NonCommercial* media vs. a commercial App Store). Plan + Android phases + iOS feasibility +
licensing: [`epic5-mobile-platforms.md`](epic5-mobile-platforms.md).

Strictly after Epic 1 (the backend swap presupposes the jME3 cutover). Captured for the record; not
on the active roadmap.
