# Epic 5 — Mobile platforms (Android port; iOS feasibility)

> **Status: speculative — NOT on the active roadmap.** Captured for the record so the analysis
> behind it isn't lost. Strictly presupposes Epic 1 (the jME3/LWJGL3 cutover).

Take the game to mobile. The headline finding is that the platform "tiers" are not what they look
like: the **desktop trio (Linux/Windows/macOS) is genuinely easy** because all three share the
**`jme3-lwjgl3` desktop backend** (GLFW + desktop OpenGL) — one codepath, three OSes. Mobile is not
free, and the two mobile platforms are very different in difficulty.

## Platform reality (why desktop ≠ mobile)

- **`jme3-lwjgl3` is desktop-only.** LWJGL3 is GLFW + desktop JNI natives; it does **not** run on
  Android or iOS. Both mobile targets need a *different* jME3 backend, so the Epic-1 host/window/
  input glue (built on LWJGL3) does not carry over to mobile as-is.
- **Android has a maintained jME3 backend; iOS does not.** `jme3-android` (OpenGL ES via the Android
  SDK) is supported upstream; `jme3-ios` (GLES via RoboVM) is effectively abandoned.
- **Irony of Epic 1:** the cutover *removes* libGDX (Phase 8), and libGDX + RoboVM is *the* canonical
  Java-game-on-iOS path. The jME3 choice optimizes desktop fidelity and maintainability at the
  explicit cost of the iOS path. (This is a deliberate, accepted trade — recorded here, not a
  regret.)

## Part A — Android port (a real but tractable project)

Android has a real managed runtime (ART) and a maintained jME3 backend, so this is "weeks–months",
not a research project.

### Phase 1 — Backend swap + boots-on-device
- Swap `jme3-lwjgl3` → `jme3-android` for the mobile build variant; get a black-screen `SimpleApp`
  booting in an `Activity` with a GLES context.
- Replace the LWJGL3 host glue (Epic-1 `JmeApp`/window/input) with the Android `JmeSurfaceView` /
  activity-lifecycle equivalents. The board/sprite/model rendering and **BUI** ride along, because
  BUI draws into the GL scene (engine-drawn UI ports with the renderer).

### Phase 2 — Input + UI for touch
- The game is mouse/keyboard-shaped (click-to-select/move, WASD camera pan/zoom/orbit — see the
  Phase-6b `GodViewHandler`/`Jme3RootNode` work). Map these to touch (tap = select/move, drag/pinch =
  camera). This is a UX design task, not a recompile.
- No Swing/AWT on Android — but the **editor** (the only Swing consumer, Phase 7) is a desktop dev
  tool and is simply not shipped to mobile, so that's a non-issue for the player client.

### Phase 3 — Asset delivery + packaging
- **Getdown can't ship code on mobile** (see iOS note below; Android is friendlier but the
  download-jars-at-runtime model still doesn't fit). Bundle assets in the APK / use Play Asset
  Delivery or a code-free CDN instead of the desktop auto-updater.
- APK/AAB packaging, signing, target-SDK compliance.

### Phase 4 — Networking + DI under mobile constraints
- **Narya** is pure Java sockets — fine on ART — but background/connectivity lifecycle and reconnect
  need attention on mobile.
- **Guice** works on ART but is reflection/bytecode-heavy; validate startup cost on-device (a Dagger
  migration is optional on Android but becomes mandatory for iOS — see below, worth weighing once).

## Part B — iOS feasibility (the genuinely hard one)

Not phased here because it's gated on unresolved blockers. iOS is plausibly the **single largest**
undertaking on the whole roadmap — bigger than Epic 1 — because the blockers *compound*:

1. **No JVM on iOS.** Apple forbids JIT and ships no JVM, so the entire Java app must be **AOT-
   compiled** to native ARM64 — realistically via **MobiVM** (the community RoboVM fork; the route
   libGDX iOS games use) or GraalVM native-image. A whole new toolchain.
2. **No maintained jME3 iOS renderer.** `jme3-ios` is abandoned and LWJGL3 doesn't apply. You would
   be reviving or writing a GLES/Metal (via ANGLE/MoltenVK) backend — an effort on the scale of
   Epic 1 by itself. **Biggest technical risk.**
3. **Reflection + runtime bytecode generation fight AOT.** **Guice** generates classes at runtime
   (`injector.getInstance`, `@Inject`, `@Singleton` are used throughout) — exactly what AOT can't do;
   this likely forces a move to compile-time DI (**Dagger**). **Narya**'s dobj streaming + generated
   marshallers are reflection-heavy and need explicit MobiVM reflection config / GraalVM reachability
   metadata.
4. **Touch input + UI redesign** — same as Android Part A Phase 2, but mandatory.
5. **Getdown's delivery model is disallowed.** iOS forbids downloading executable code; all code must
   ship inside the signed binary. Assets must be bundled or served from a code-free CDN.

If iOS ever became a goal, the realistic shape is: MobiVM AOT × decide the renderer (revive
jME3-GLES vs. seriously reconsider a libGDX mobile path vs. Metal-via-ANGLE) × Guice→Dagger ×
Narya reflection config × touch redesign × asset bundling — each a project in its own right.

## Part C — Licensing & store concerns (a non-technical blocker)

- **The media is CC Attribution-NonCommercial** (everything under `assets/rsrc/`; see CLAUDE.md). The
  App Store / Google Play with IAP is *commercial* distribution, which the asset license likely
  **prohibits**. A paid or IAP mobile release is therefore a *licensing* decision before it is an
  engineering one: relicense/replace the art, or ship strictly non-commercially.
- **In-app purchase rules.** Bang! has a virtual economy (gold/scrip, the store). Apple (and Google)
  require their IAP for digital goods (~30% cut) and forbid side-channel payment for them, so the
  existing payment/account flow would need rework regardless of the license question.
- The BSD-licensed *code* is fine to redistribute; the blocker is specifically the **art/audio**
  license interacting with a commercial store model.

## Acceptance (Part A only, if ever pursued)

The player client builds and runs on a current Android device from the jME3 mobile backend, with
touch controls, bundled assets, and a working connection to a `BangServer`; the desktop build is
unaffected. (Part B / iOS remains feasibility-only until its blockers — renderer, AOT/DI, and the
media license — are resolved.)
