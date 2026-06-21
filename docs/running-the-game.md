# Running Bang! Howdy for testing (jME3 / LWJGL3)

This is the standard run sheet for launching the migrated client/server to test or screenshot a
change. **Use `bin/devtest`** — it codifies the setup so you don't re-derive (and mis-handle) it.
Most automated run failures have come from skipping one of these steps; the script does them in
order and always tears down.

## `bin/devtest` — the one command

```sh
bin/devtest [--board "<name>"] [--node frontier_town|indian_post] \
            [--shot <path>] [--hold <secs>] [--no-deploy] [--mute] [--no-client]
```

What it does, in order: **(1)** kills stale Bang processes and frees port `47624`; **(2)** verifies
MySQL is up and `bang`/`yeehaw` connects; **(3)** `./gradlew deploy` (skip with `--no-deploy`),
aborting on failure; **(4)** starts the server and **waits until it logs `Server listening`** before
going on; **(5)** launches the client with the standard flags (`-test`/`-autoplay`, creds; audio
plays by default, `--mute` to silence — see below); **(6)** with `--shot`, holds `--hold` seconds
then captures **only the Bang window** (via `bin/shot`) to the path — never a full-display grab;
**(7)** always tears everything down on exit (even Ctrl-C), leaving `47624` free.

Examples:
```sh
bin/devtest --shot /tmp/town.png --hold 35                          # smoke test → capture → teardown
bin/devtest --board "Thunderbird Rock" --node indian_post --shot /tmp/water.png
bin/devtest --no-deploy                                             # quick interactive run on current build
bin/devtest --no-client                                            # server only
```

- **Agents: always use `--shot` mode.** It auto-tears-down and exits, so it is deterministic and
  never hangs (plain mode blocks on the client until Ctrl-C). Logs: `/tmp/devtest-server.log`,
  `/tmp/devtest-client.log`.
- `DISPLAY` defaults to `:1` (override via the env var).

## Gotchas the script handles for you (and a couple it can't)

- **Audio plays by default.** Pass `--mute` (→ `-Dmute=true`, sets `BangPrefs.SILENT`) to silence
  music + effects for a dev session. It does not persist to the stored volume prefs.
- **MySQL must be running** (`bang`/`ooouser` DBs, `bang`/`yeehaw` user — see README). The script
  verifies and aborts with instructions if not; it does **not** `sudo systemctl start mysql` for you
  (that's interactive).
- **`indian_post` node** only serves its boards if `etc/test/server.properties` has
  `indian_post.town_id = indian_post` (otherwise `ServerConfig.townId` falls back to
  `frontier_town` and the indian_post boards are skipped → `getBoard()` NPE). `etc/test/` is local
  (gitignored); set this if you use `--node indian_post`.
- **Don't launch the client before the server is listening** — the script polls the log so you
  can't; doing it by hand races into `BindException`/"connection refused".

## Screenshot convention — `bin/shot` (window-only, never full-display)

**Screenshots capture only the Bang window, never a full-display grab.** This is for privacy (a
full-display grab leaks whatever else is on the multi-monitor desktop — other apps, the other
monitor — into shared/committed images) and relevance (no lossy AI crop afterward; the PNG is
exactly the window).

```sh
bin/shot /tmp/shot.png            # captures the live Bang client OR editor window to the PNG
bin/shot /tmp/shot.png :1         # explicit DISPLAY (defaults to $DISPLAY, else :1)
```

`bin/devtest --shot` calls `bin/shot` for you. `bin/shot` matches the window **precisely** to avoid
false positives (a loose `'bang! howdy' in title` substring once grabbed a *Firefox* tab titled
with the repo description): it requires the title to be exactly `Bang Editor` or to start with
`Bang! Howdy`, **and** the window's WM_CLASS to not be a browser/terminal/editor (explicit
denylist), preferring a WM_CLASS that looks like the jME/Java/LWJGL3-GLFW app. It then grabs only
that window's pixmap (python-xlib `get_image`, with an ImageMagick `import -window <id>` fallback) —
pixel-exact and independent of stacking/occlusion. Where a live window isn't needed, prefer the
offscreen render harnesses below (already window-only and deterministic).

## Deterministic alternatives (no live client needed)

For per-change *visual* verification without the flaky live path, prefer the offscreen
render-to-PNG harnesses (Phase-5 harness #1 — they render through the real load/resolve path with
no window, on an isolated LWJGL3 classpath, framed/lit like the board in its Z-up convention).
Run `./gradlew deploy` once first to bake the staged `model.j3o` (under `assets/build/staging/rsrc`).

```sh
# one isolated model (static prop/building), through the real Model facade + texture resolve + clone:
./gradlew :tools:j3o-converter:renderToPng  -PmodelType=props/frontier_town/buildings/saloon -Pout=/tmp/saloon.png

# a unit / big-shot CHARACTER model with its skin and a posed animation frame (the mode that
# reproduces the Phase-6 shotgun-dude / mounted-big-shot defects deterministically):
./gradlew :tools:j3o-converter:renderModelToPng -PmodelType=units/frontier_town/shotgunner -Panim=standing -Ptime=0 -Pout=/tmp/shotgunner.png
./gradlew :tools:j3o-converter:renderModelToPng -PmodelType=units/frontier_town/shotgunner -Panim=list   # print anim names; renders the bind pose

# a multi-model scene on a ground grid (units + a building), each spec is modelType[@anim[:time]]:
./gradlew :tools:j3o-converter:renderSceneToPng -Pout=/tmp/scene.png \
    -Pspecs=units/frontier_town/shotgunner@standing,units/frontier_town/cavalry@standing,props/frontier_town/buildings/saloon

# the water surface (BangWater.j3md) reproduced headlessly (-Pframe advances the waves):
./gradlew :tools:j3o-converter:renderWaterToPng -Pout=/tmp/water.png

# the regression gate: diff a render vs a baseline PNG, write a heat-map diff, exit non-zero past tolerance:
./gradlew :tools:j3o-converter:snapshotDiff \
    -Prendered=/tmp/shotgunner.png -Pbaseline=baseline/fork-before/shotgunner.png \
    -Pdiff=/tmp/shotgunner-diff.png -Ptolerance=0.015
```

`baseline/fork-before/` (pre-cutover reference shots) is gitignored and lives only in the canonical
checkout; `snapshotDiff` takes the baseline as a `-Pbaseline=` path, it is not hardcoded.

## Known live-render defects (see docs/jme3-cutover-plan.md "Known live-render defects")

- **WASD camera panning doesn't work** — Phase-3 input gap (`GodViewHandler.registerWith(InputManager)`
  not wired). You can't pan to compose shots yet; pick a board whose default camera frames the subject.
- Sideways claim-counter numbers (`BillboardControl` Y-up vs Z-up board) and the wrong horse model
  are queued there too.
