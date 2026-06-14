# Fidelity-reference video catalog

Short gameplay clips the user filmed to give the jME3-cutover fidelity work
**authoritative motion references** — the temporal shape (wind-up → fire → effect →
reaction → death; flash → debris → smoke fade) that a single still cannot convey. Pair
these with the stills in [`README.md`](README.md) and the unit/effect tables in
[`../game-reference/`](../game-reference/).

> **PROVENANCE — CONFIRMED 2026-06-14: all ten are from the OLD FORK build**
> (pre-cutover `bang-game`/master, LWJGL2/gdx). They are **correct golden motion
> references** — the target the jME3 conversion must match — **not** current-jME3 defect
> evidence. Same status as the stills.

> **The `.mp4` files are gitignored.** They live in `docs/fidelity-reference/video/*.mp4`
> in the working checkout but are **not committed** (this catalog is). The relative links
> below resolve only locally until the user confirms the clips are public-safe to commit.
> Frames cited were extracted to `/tmp` with `ffmpeg` for analysis and are **not** stored
> in the repo.

## Source specs

All ten: **1920×1080, 60 fps**, H.264. Durations 2.7 s – 7.6 s. Captured as full
application-window grabs (the OS taskbar/title bar is visible on several — minor, but if
re-filmed, `bin/shot`-style window-only crops would be cleaner). Camera is the standard
isometric board angle; most clips are at a **zoomed-out board scale**, so individual unit
skinning detail is near the resolution floor — composition, poses, gang colours, effect
location/colour/lifetime read clearly; per-glyph and per-bone detail does not. See
"Re-filming notes" at the end.

## How to extract frames (for re-inspection)

```sh
# even sweep
ffmpeg -i <clip>.mp4 -vf "fps=3,scale=900:-1" /tmp/<name>_%02d.png
# zoom + brighten a region (crop=W:H:X:Y is top-left origin in source px)
ffmpeg -ss <t0> -to <t1> -i <clip>.mp4 \
  -vf "fps=8,crop=560:520:620:280,eq=brightness=0.1,scale=840:-1" /tmp/z_%02d.png
```

## Offscreen compare harness (no live game)

```sh
./gradlew :tools:j3o-converter:renderModelToPng \
    -PmodelType=units/frontier_town/<unit> -Panim=<clip> -Ptime=-1 -Pout=/tmp/x.png
# -Panim=list lists a model's clip names; -Ptime=-1 poses the clip midpoint
```
All ten clips are frontier_town units: `artillery`, `gunslinger`, `shotgunner`,
`steamgunman`, `tactician`, `cavalry` (see
[`../game-reference/units-and-big-shots.md`](../game-reference/units-and-big-shots.md)).

---

## What each clip validates (index)

| Clip | One-line | Ground truth for |
|------|----------|------------------|
| [ArtilleryAttack](#1-artilleryattack) | Artillery ballistic hit on a green unit (survives) | particle: ballistic impact dust/smoke + damage readout |
| [ArtilleryAttacksGunslinger](#2-artilleryattacksgunslinger) | Artillery shell explodes on a Gunslinger (survives) | Gunslinger posed-skinning **in motion** + explosion flare lifecycle |
| [ArtilleryAttacksSteamGunman](#3-artilleryattackssteamgunman) | Artillery hits a Steam Gunman (steam unit, smoke plume) | particle impact on a steam unit + always-on `SmokePlumeEmission` |
| [ArtilleryKillShotgunner](#4-artillerykillshotgunner) | Artillery **kills** a Shotgunner — full explosion + death | **headline** explosion lifecycle + human death + dropped item |
| [CavalryPicksupGoldKillsSteamGunman](#5-cavalrypicksupgoldkillssteamgunman) | Mounted green Cavalry carries gold, kills a red Steam Gunman | held-gold prop + gang tint + mounted composite + steam-unit kill |
| [GunSlingerReceivesImmunity](#6-gunslingerreceivesimmunity) | A Gunslinger gains immunity (Iron Plate) | immunity effect appearance/lifetime |
| [SteamGunmanPickupBonus](#7-steamgunmanpickupbonus) | Steam Gunman moves onto a board bonus and collects it | `get_bonus` pickup sparkle + persistent smoke plume in motion |
| [TacticianAttackCavalry](#8-tacticianattackcavalry) | Tactician (red) shoots a Cavalry (blue) | Tactician `shooting` posed-skinning + `GunshotEmission` |
| [TacticianAttackGunslinger](#9-tacticianattackgunslinger) | Tactician (red) shoots a Gunslinger (yellow) | Tactician + Gunslinger posed-skinning, gunshot flash |
| [TacticianKillSteamGunman](#10-tacticiankillsteamgunman) | Tactician kills a Steam Gunman (steam death) | steam-unit death = white steam puff/wreckage (vs human fiery death) |

> **Marquee not captured:** none of the three Tactician clips shows the **umbrella
> `blocking` deflect pose** (the Tactician here is always the *attacker*, never deflecting
> an incoming ballistic shot). That clip is still wanted — see "Re-filming notes".

---

## 1. `ArtilleryAttack`

`video/ArtilleryAttack.mp4` — 4.35 s. Desert/rail board, zoomed in fairly tight.

**What happens.** The **Artillery** (steam siege unit, low profile, lower-left) fires on
a **green-gang** humanoid one rank away. Artillery is a `BallisticUnit` with **no muzzle
emitter** (the shell is the ballistic projectile, not a gunshot flash) — so the visible
event is the *impact*, not a barrel flash. The target **survives**.

**Temporal profile (≈60 fps).**
- **0.0–1.5 s — wind-up / shell travel.** Artillery in its `shooting` pose; targeting
  reticle on the green unit; a blue move/range arc on the ground around the target.
- **~1.6–1.9 s — impact.** A **white dust/smoke burst** erupts at the target's feet
  (ground-level puff, whitish, soft-edged — reads as a `dust`/impact-`hit_flash` system,
  not the big fiery `explosion`). A green floating damage number ("10", then a second
  "17" readout) rises.
- **~2.0–3.0 s — settle.** The puff expands then **lingers and fades**; the green number
  drifts up and fades. No death — the unit holds its `standing`/`reacting` pose.

**Conversion-relevant.** The impact effect here is the **softer dust/smoke** variant
(green-unit hit that does *not* kill), distinct from the fiery kill-explosion in clip 4 —
useful for calibrating the *non-lethal* artillery impact. The **damage number takes the
target's gang colour** (green here) — confirms the `DamageIconViz` tint mapping noted in
README §3/§4.

**Maps to:** particle/impact-effect fidelity (`ParticleConverter`/`ParticleEffectViz`,
`dust`/`hit_flash`) + damage-readout tint. Cross-ref stills `damageIndicator.png`,
`attackAndDeathAnimation.png`.

---

## 2. `ArtilleryAttacksGunslinger`

`video/ArtilleryAttacksGunslinger.mp4` — 3.13 s. Dusk **town** board (buildings, rails).

**What happens.** Artillery fires a shell that **explodes on a Gunslinger** (the target
carries a green health bar + green selection arc). The Gunslinger **survives** (takes 25).

**Temporal profile.**
- **0.0–1.7 s — aim.** Gunslinger stands with green selection arc; blue range arcs and
  gang-coloured selection arcs (green/blue/yellow) on nearby pieces.
- **~1.8–2.1 s — explosion.** A **bright warm orange/yellow flash** blooms on the
  Gunslinger (additive-looking core) with a brief white smoke streak — clearly the fiery
  `explosion` system, larger/hotter than clip 1's dust puff.
- **~2.2–3.1 s — bloom + fade.** The flash develops into a **green-tinted vertical
  smoke/spark column** rising off the unit (the lingering tail takes a gang tint) with a
  green **"25"** floating up. The Gunslinger stays standing (survives) as the column fades.

**Conversion-relevant.** This is the **Gunslinger posed-skinning in motion** under live
lighting (the headline anim-fidelity unit — its `shooting`/`returning_fire` clips host
the known anim-specific skinning defect; here it is the *target*, so it shows
`standing`→`reacting`). Plus a clean **explosion lifecycle**: warm flash → smoke/spark
column → fade, with the **tail tinted by the target's gang colour**. Note the explosion's
warm additive core is brief; the smoke tail is what lingers.

**Maps to:** posed-skinning fix (Gunslinger) **+** explosion particle fidelity.
Compare: `renderModelToPng -PmodelType=units/frontier_town/gunslinger -Panim=reacting`.
Cross-ref stills `attackAndDeathAnimation.png`, `explosionAndDeath.png`.

---

## 3. `ArtilleryAttacksSteamGunman`

`video/ArtilleryAttacksSteamGunman.mp4` — 6.00 s. Town board, wider framing.

**What happens.** Artillery fires on a **Steam Gunman** (the cream/white cylindrical
steam robot). A **red** "10" damage readout and a skull-and-crossbones death icon appear
in the target cluster. The Steam Gunman is a **steam unit** carrying the always-on
`SmokePlumeEmission` (engine smoke) throughout.

**Temporal profile.**
- **0–3 s — manoeuvre/aim.** Units reposition; yellow targeting arc + the death-skull
  icon resolve on the target cluster (lower-right).
- **~3.5–4.5 s — impact.** Yellow impact flash near the target; a **red "10"** damage
  number rises (red = the target's gang colour). A dropped gold-nugget bonus is visible
  nearby on the board.
- **~4.5–6 s — settle.**

**Conversion-relevant.** Two things: (a) **damage readout takes the *target's* gang
colour** — red here, vs green in clips 1–2 — a clean cross-clip confirmation of the tint
mapping; (b) the **Steam Gunman's persistent smoke plume** must render as a *continuous*
emitter (not one-shot) across its idle/move — verify `SmokePlumeEmission` survives the
bake as a steady particle system. The board-scale framing makes the impact effect itself
small; clip 4 is the better explosion reference.

**Maps to:** particle/impact + steam-unit `SmokePlumeEmission` continuity + damage tint.
Compare: `renderModelToPng -PmodelType=units/frontier_town/steamgunman -Panim=standing`.

---

## 4. `ArtilleryKillShotgunner`

`video/ArtilleryKillShotgunner.mp4` — 4.00 s. Desert/rail board. **Best explosion +
death reference of the set.**

**What happens.** Artillery fires a shell that **lands on and kills a Shotgunner**
(green-gang humanoid). This is the clearest start→peak→fade of the explosion *and* the
human death + dropped-item behaviour.

**Temporal profile (the key one).**
- **~0–1.6 s — Shotgunner standing**, green health bar + green selection arc.
- **~1.7 s — flash.** A **bright orange/yellow explosion core** ignites on the unit with
  **radial debris sparks ejected roughly horizontally** (streaks flying left and right at
  ground level) — additive warm core + outward debris.
- **~1.9–2.4 s — bloom.** The core rises into a **green-tinted vertical fire/smoke
  column**; a green **"50"** damage number floats up. A yellow/orange flaming streak
  trails as the unit **collapses** (death). Debris streaks persist briefly.
- **~2.5–3.4 s — fade.** Column thins to a **green-tinted lingering smoke wisp**; the
  Shotgunner is now a **dead/prone shape** beneath it.
- **~3.5–4.0 s — cleared.** Effect fully gone; the killed unit is **removed and a gold
  nugget is left dropped on its tile** (carried/awarded item drop on death).

**Conversion-relevant — this is the headline particle test.** Capture for
`ParticleConverter`/`ParticleEffectViz`:
- **Shape:** bright additive *core flash* → **horizontal radial debris streaks** → a
  *vertical* smoke/spark **column** (three distinct sub-phases, not one puff).
- **Colour:** warm orange/yellow core; the **smoke/spark tail is tinted by the victim's
  gang colour** (green). Confirm whether that tint is gameplay-driven or a coincidental
  overlay — it is consistent with clips 2 & 4 (both green victims tint green).
- **Lifetime:** core ~0.2 s, debris ~0.5 s, column/wisp lingers ~1.5 s after the flash.
- **Death:** humanoid → holds last `dying` frame as a prone shape, then is removed; a
  **dropped nugget** remains on the tile.

**Maps to:** explosion particle lifecycle **+** human death/dead-swap **+** death-drop.
Compare: `renderModelToPng -PmodelType=units/frontier_town/shotgunner -Panim=dying`;
bake-compare the `frontier_town/explosion` particle asset. Cross-ref stills
`explosionAndDeath.png`, `DeathAndDamageIndicator.png`.

---

## 5. `CavalryPicksupGoldKillsSteamGunman`

`video/CavalryPicksupGoldKillsSteamGunman.mp4` — 4.60 s. Town board (a "SALOON" building
in frame).

**What happens.** A **green-gang Cavalry** (mounted big shot — horse + rider + rifle
composite) is **carrying gold** (a gold-stash counter "**2**" floats below it) and **kills
a red-gang Steam Gunman** (red "30" damage + skull-and-crossbones → destroyed).

**Temporal profile.**
- **0–1.5 s — Cavalry holding gold**, green selection arc, big blue **"2"** stash counter
  below the unit; targets the red Steam Gunman (red arc + skull icon to the right).
- **~1.5–3.3 s — attack/kill.** Cavalry fires (rifle); a **red "30"** damage number rises
  on the Steam Gunman with the skull icon; the Steam Gunman is **destroyed**.
- **~3.3–4.6 s — aftermath.** A dropped item (yellow/orange) appears; the Cavalry, now
  facing forward, shows the **green-tinted mounted composite** (rider clothing/saddle
  tinted green over the horse).

**Conversion-relevant.**
- **Mounted composite skinning:** Cavalry = `horse.png` + `rider.png` + `rifle.png` as a
  posed composite — verify the rider sits/poses correctly on the horse through
  `standing`→`shooting` (a posed-skinning case beyond the single-mesh humanoids).
- **Gang tint:** the **green** colorization on the mounted rider — verify which body
  regions tint (rider, not horse) and that it matches the fork hue.
- **Held gold:** `unit.holding != null` attaches `bonuses/frontier_town/nugget` as a
  child node riding the move cycle (README §6). The **"2" is the gold-stash counter
  number** (`CounterSprite`, tinted the owner's gang colour), distinct from the carried
  nugget prop — both appear here.
- **Steam-unit kill** confirms the steam-death path again (red victim → red readout).

**Maps to:** posed-skinning (mounted composite) **+** held-prop / counter (effect/prop
group) **+** gang tint. Compare:
`renderModelToPng -PmodelType=units/frontier_town/cavalry -Panim=shooting`. Cross-ref
still `GreenCharactersCarryingGold.png`.

---

## 6. `GunSlingerReceivesImmunity`

`video/GunSlingerReceivesImmunity.mp4` — 3.05 s. Dim desert board (low light — extracted
frames were brightened to read it).

**What happens.** A **green-gang Gunslinger** receives an **immunity** buff (Iron Plate:
"immune to attacks for 7 ticks"). Nearby: blue-gang steam units and a dropped gold nugget.

**Temporal profile.**
- **0–~1.0 s — setup.** Gunslinger with green health bar + green selection arc.
- **~1.0–1.6 s — immunity applied.** A **circular yellow/gold ring/glow** appears around
  the recipient (a brief ground/aura ring centred on the unit) — the immunity grant
  visual. Short-lived (≈0.5 s of obvious ring).
- **~1.6–3.0 s — settle.** Ring fades; the unit remains (now buffed). No persistent
  shield/iron-plate billboard is obviously visible at this zoom/lighting — the marker is
  the **grant flash**, not a sustained overlay (verify whether a sustained icon exists at
  closer range).

**Conversion-relevant.** The immunity grant is a **brief circular aura/ring** in the
unit's gang-adjacent colour, not a big particle burst. Validate the
`IronPlate`/immunity-influence visual fires and reads at board scale. **Low fidelity
clip** — dim and brief; a closer, brighter re-film would help confirm whether there's a
*sustained* immune-state indicator (icon/sheen) for the 7-tick duration.

**Maps to:** effect/prop group (immunity influence visual). See the Iron Plate card in
[`../game-reference/effects-and-cards.md`](../game-reference/effects-and-cards.md).

---

## 7. `SteamGunmanPickupBonus`

`video/SteamGunmanPickupBonus.mp4` — 4.12 s. Desert/rail board.

**What happens.** A **Steam Gunman** moves across its (yellow) movement-range tiles onto
a **board bonus** and **collects** it; a **green "30"** readout and a green sparkle appear
(consistent with a Repair/heal or bonus-point pickup). The steam unit's **smoke plume** is
visible the whole time.

**Temporal profile.**
- **0–~1.5 s — move plan.** Yellow movement-range tiles light up; a **yellow diamond
  bonus marker** sits on the ground at the destination; blue gold-stash counters on pieces.
- **~1.6–2.3 s — pickup.** The Steam Gunman reaches the bonus tile; a **green/yellow
  sparkle pickup effect** plays at the tile (the `get_bonus` system) and a green **"30"**
  floats up.
- **~2.3–4.1 s — settle.** Sparkle fades; the unit holds the bonus/effect. The white
  **engine smoke plume** vents off the cylinder throughout (continuous emitter).

**Conversion-relevant.** Validates the **`get_bonus` pickup sparkle** (location, colour,
short lifetime) and, again, the **always-on `SmokePlumeEmission`** as a continuous system
during movement. The yellow ground diamond is the board **bonus marker** model.

**Maps to:** effect/prop group (`get_bonus` particle + bonus pickup) **+** steam smoke
continuity. See `frontier_town/get_bonus` in
[`../game-reference/effects-and-cards.md`](../game-reference/effects-and-cards.md).

---

## 8. `TacticianAttackCavalry`

`video/TacticianAttackCavalry.mp4` — 2.95 s. Canyon/mesa board.

**What happens.** The **Tactician** (red-gang big shot, with umbrella prop in its kit)
**shoots a Cavalry** (blue-gang, with blue selection arc) below it. A "P" placard (a
placed card on the board) is visible nearby.

**Temporal profile.**
- **0–~1.0 s — aim.** Tactician (red) with targeting reticle; Cavalry (blue) one rank
  below.
- **~1.1–1.5 s — fire.** A **small warm orange/yellow muzzle flash** + a short **white
  shot streak** from the Tactician toward the Cavalry (`GunshotEmission` on `shooting`).
- **~1.6–2.9 s — reaction.** Cavalry reacts; shot streak fades. (This is a normal
  `shooting` attack — **not** the umbrella deflect; the Tactician is the attacker.)

**Conversion-relevant.** Tactician **`shooting` posed-skinning in motion** + its single
`GunshotEmission` muzzle flash timing/placement at the barrel. Red gang tint on the
Tactician. **No umbrella `blocking` pose here** — that needs a separate clip (Tactician
*deflecting* an incoming Artillery shot).

**Maps to:** posed-skinning fix (Tactician) + gunshot emission timing. Compare:
`renderModelToPng -PmodelType=units/frontier_town/tactician -Panim=shooting`;
`-Panim=list` to confirm the `blocking` clip exists for the future deflect capture.

---

## 9. `TacticianAttackGunslinger`

`video/TacticianAttackGunslinger.mp4` — 2.70 s. Desert/rail board (steam units with blue
gang counters in frame).

**What happens.** The **Tactician** (red) **shoots a Gunslinger** (yellow-gang target,
upper area). Several blue-gang steam units sit nearby with blue counter circles.

**Temporal profile.**
- **0–~1.3 s — aim.** Targeting reticle on the yellow Gunslinger; Tactician lines up.
- **~1.4–1.8 s — fire.** A **white/yellow muzzle flash** at the Tactician's barrel
  (`GunshotEmission`); brief shot toward the Gunslinger.
- **~1.9–2.7 s — reaction/fade.**

**Conversion-relevant.** Two posed-skinning units of interest in one clip — Tactician
(`shooting`, red) attacking the headline **Gunslinger** (yellow, `reacting`). Confirms the
gunshot flash reads at the barrel and the **yellow gang tint** on the Gunslinger. Board
scale limits per-bone detail; use alongside the offscreen render for exact poses.

**Maps to:** posed-skinning fix (Tactician + Gunslinger) + gunshot emission. Compare:
`renderModelToPng -PmodelType=units/frontier_town/gunslinger -Panim=reacting` and
`.../tactician -Panim=shooting`.

---

## 10. `TacticianKillSteamGunman`

`video/TacticianKillSteamGunman.mp4` — 7.57 s (longest clip). Desert/rail-bridge board,
zoomed out.

**What happens.** Over a longer sequence the **Tactician kills a Steam Gunman**. The kill
event (≈4 s in, near the rail bridge on the right) reads as a **white smoke/steam puff** —
the **steam-unit death** path, which produces a **steam-vent puff + wreckage**, *not* the
warm fiery column a human gets (contrast clip 4).

**Temporal profile.**
- **0–~3.5 s — manoeuvre.** Units reposition along the rail; green/yellow movement-range
  tiles; dropped gold nuggets scattered on the board.
- **~4.0 s — kill.** A **white smoke puff** appears at the target (right side, by the
  bridge) — steam unit destroyed → wreckage. No big orange flare like the human death.
- **~4.5–7.6 s — settle.** Smoke clears; play continues.

**Conversion-relevant.** The **steam-unit death contrast** is the takeaway: Artillery
units & steam units swap to **wreckage** (`units/wreckage/*`) with a **white steam/dust
puff**, distinct from the human `dying`→prone + fiery `explosion`. Verify the jME3 steam
death uses the wreckage props + steam puff, not the humanoid death path. **Lowest-fidelity
clip of the set** — long, zoomed out, kill is small and off-centre; a tighter re-film of
just the steam death would help.

**Maps to:** death/effect fidelity (steam-unit wreckage death). Cross-ref the wreckage
props note in
[`../game-reference/units-and-big-shots.md`](../game-reference/units-and-big-shots.md#wreckage-props-unitswreckage)
and still `explosionAndDeath.png`.

---

## Re-filming notes (what would make these stronger)

Concrete asks for the next round of fork captures — these are golden references, so
tighter framing directly raises fidelity-checking power:

1. **Tactician umbrella `blocking` deflect — MISSING, high value.** All three Tactician
   clips show it *attacking*. Film the Tactician **being shot by an Artillery** so the
   `blocking` clip + umbrella-up deflect pose plays (the marquee Tactician visual).
2. **Tighter crops, fixed zoom.** Most clips are board-scale; unit skinning detail is at
   the resolution floor. For the posed-skinning clips (Gunslinger, Tactician, Cavalry),
   **zoom the camera in on the single acting unit** — a tight 1-unit framing beats 1080p
   of whole-board.
3. **`GunSlingerReceivesImmunity` (#6)** is too **dim and brief** — re-film brighter and
   closer; confirm whether a **sustained** immune-state indicator exists for the 7 ticks
   or only the grant flash.
4. **`TacticianKillSteamGunman` (#10)** — kill is small/off-centre and far into a 7.6 s
   clip; re-film a **tight ~3 s steam death** so the wreckage swap + steam puff are clear.
5. **Window-only grabs.** Several clips include the OS taskbar/title bar; a
   `bin/shot`-style window crop keeps captures clean (and avoids leaking desktop content).
6. **One effect per clip, labelled.** Keep doing the descriptive filenames — they made
   this catalog possible.

## Open question for the user

The **explosion/death smoke tail appears tinted by the *victim's* gang colour** (green in
clips 2 & 4, where both victims are green-gang). Is that intentional gameplay tinting of
the death effect, or an overlay/readout coincidence? It affects how we colour the baked
`explosion` particle system in jME3.

---

# Batch 2 (new clips)

> **PROVENANCE — CONFIRMED: all nine are from the OLD FORK build** (same status as
> batch 1) — **correct golden motion references**, the target the jME3 conversion must
> match, **not** current-jME3 defect evidence. `.mp4`s live in `video/*.mp4` and are
> **gitignored**; frames were extracted to `/tmp` for analysis and are not committed.

## Source specs (batch 2)

All nine: **1920×1080, 60 fps**, H.264. Durations 1.68 s – 5.57 s. Same full-window
isometric board grabs as batch 1 (OS taskbar visible on several). Most are at a
zoomed-out board scale, so per-bone/per-glyph detail is at the resolution floor;
composition, poses, gang colours, prop orientation, effect location/colour/lifetime read
clearly. Frames were extracted with the batch-1 commands (even `fps` sweep + zoomed,
brightened crops on the action).

This batch **fills batch 1's marquee gap** (the Tactician umbrella **deflect**, which all
three batch-1 Tactician clips lacked) and adds two **camera-control** references plus a
**structure-destruction** and an **air-unit** reference.

## What each batch-2 clip validates (index)

| Clip | One-line | Ground truth for |
|------|----------|------------------|
| [TacticianBlocksArtillery](#11-tacticianblocksartillery--marquee-umbrella-deflect) | **Tactician umbrella DEFLECT** of an Artillery shell | the missing defensive `blocking` pose + umbrella-canopy open + shell bounce-away — **bug-fix target** |
| [DustAnimation](#12-dustanimation--movement-dust) | Faint ground movement-dust near units | movement-dust effect appearance/density/colour (effect-present investigation) |
| [SteamGunmanPicksUpClock](#13-steamgunmanpicksupclock) | Steam Gunman collects a **clock (Hustle)** bonus | `get_bonus` star-sparkle pickup + Hustle bonus prop |
| [TacticianPicksUpHealth](#14-tacticianpicksuphealth) | Tactician collects a **health (Repair)** item | repair/heal pickup burst |
| [ArtilleryDestroysHomestead](#15-artillerydestroyshomestead) | Artillery shell **destroys a homestead** (structure) | structure-death explosion (distinct from unit death) |
| [DirigibleKillsArtillery](#16-dirigiblekillsartillery) | Airborne **Dirigible** kills an **Artillery** | flyer appearance/elevation + twin gunshot + steam-unit kill |
| [ScrollwheelRotatesCamera](#17-scrollwheelrotatescamera--camera-reference) | Scroll wheel **orbits/swings** the camera 90° | camera scroll-wheel rotate control |
| [StartGameAndZoomOut](#18-startgameandzoomout--camera-reference) | Game-start camera **pulls back / zooms out** | game-intro zoom-out camera move |
| [GameLoadScreen](#19-gameloadscreen--ui-reference) | Pre-game roster / scenario load screen | load-screen UI layout |

---

## 11. `TacticianBlocksArtillery` — MARQUEE umbrella deflect

`video/TacticianBlocksArtillery.mp4` — 3.35 s. Town/desert board, zoomed out (action
center-frame; analysed with a tight brightened crop on the acting Tactician).

**This is the defensive move batch 1 never captured** — the user flags the jME3 build's
`blocking`/umbrella deflect as BUGGED, so this clip is the **golden fix target**.

**What happens.** A **yellow-gang Tactician** is targeted by an enemy **Artillery** (the
blue steam siege unit one rank to its upper-right; a death-skull targeting reticle sits on
the Tactician). When the Artillery fires its ballistic shell, the Tactician **opens his
umbrella overhead** and the incoming shell **bounces up and away** off the canopy, landing
on an adjacent tile (matching the mechanic *"an adjacent unit may be hit by the deflected
shot"*). The Tactician takes no damage.

**Temporal profile (≈60 fps).**
- **0.0–~2.2 s — wind-up.** Tactician stands (`standing`) under the skull reticle;
  umbrella is **down/closed** at his side (no umbrella visible raised). Artillery aims.
- **~2.3–2.45 s — raise/open.** The Tactician snaps into the **`blocking` pose**: the
  umbrella swings up overhead and the **canopy opens into a wide yellow/gold disc** above
  him (the canopy clearly inflates from a closed cone to an open umbrella — it is a
  *skinned* canopy deformed by its own rib bones, not a static billboard).
- **~2.45–2.6 s — deflect.** A **dark grey cylindrical shell** appears **airborne just
  above-left of the open canopy** — the deflected Artillery projectile ricocheting up and
  outward off the umbrella. (A `deflect_shot.ogg` sound plays here per the handler.) No
  fiery explosion *on* the Tactician.
- **~2.7–3.0 s — settle.** Canopy **closes back down** and the umbrella lowers; the shell
  has landed to the right near the Artillery's tile. Tactician returns to `standing`,
  undamaged.

**Fidelity detail — what the *correct* deflect looks like (the spec to match).**
1. **Umbrella opens.** The headline visual is the **canopy inflating from closed→open**
   over the unit's head. It is **not** a prop swap or a particle — the umbrella is a
   **`skinMesh`** in `model.mxml` deformed by a dedicated bone rig (`bone umb base` +
   `Bone01…Bone33` umbrella ribs), and `blocking.mxml` carries the per-frame
   `umbrella` + rib-bone transforms that open it. So a correct deflect requires the
   **`blocking` clip to bake** *and* the **umbrella skinMesh + its rib bones to skin
   correctly** through that clip.
2. **Orientation.** Umbrella ends up **horizontal, overhead, canopy-up** (a shield over
   the unit), not held to the side as in idle.
3. **Deflect effect.** The visible "effect" is the **shell itself bouncing away** (a grey
   ballistic prop arcing up/out), plus the deflect sound — **no spark/flash** on the
   umbrella in the fork. Don't expect a particle burst; expect the redirected projectile.
4. **Timing.** Raise+open is fast (~0.15 s), canopy held open only through the deflect
   (~0.2 s), then closes — a brief, snappy block, not a sustained guard.

**Which fix/verification it informs.** This is the **golden target for the umbrella-prop /
`blocking` clip** the user says is bugged in jME3. Code path is intact: `Tactician.deflect`
redirects the `BallisticShotEffect`, and `BallisticShotHandler` (`getDeflectorSprite()` →
`queueAction(dsprite, "blocking")` at line ~195) plays the `blocking` action on the
deflector and plays `deflect_shot.ogg`. The assets are present:
`units/frontier_town/tactician/blocking.mxml` + `umbrella.png`, and `model.properties`
lists `blocking` in `animations`. **So the wiring + clip + texture all exist.** The
likely defect surface is therefore the **skinning of the umbrella `skinMesh` and its rib
bones through the `blocking` clip** — the body posed-skinning fix (Phase-8 model-root
identity joint + Phase-6b skin-space bake) covered the humanoid armature generally, but
the **umbrella's own bone rig (`bone umb base`/`Bone01…33`) opening the canopy needs
specific verification** — if those ribs don't skin, the canopy won't inflate (you'd get a
closed/collapsed or contorted umbrella, or none). **Action:** bake-check the tactician
`blocking` clip and confirm the umbrella canopy opens.

**Maps to:** posed-skinning fix (Tactician **umbrella skinMesh + rib bones**) + the
`blocking` deflect clip + deflected-projectile playback. Compare:
`renderModelToPng -PmodelType=units/frontier_town/tactician -Panim=blocking -Ptime=-1`
(and `-Panim=list` to confirm `blocking` baked). Cross-ref
[`../game-reference/units-and-big-shots.md`](../game-reference/units-and-big-shots.md)
(Tactician row: *"Deflects ballistic shots with his umbrella… `blocking` (umbrella-up
deflect pose)… Prop: `umbrella.png`"*) and `Tactician.deflect` /
`BallisticShotHandler`. Closes the batch-1 "Marquee not captured" / re-filming item #1.

---

## 12. `DustAnimation` — movement dust

`video/DustAnimation.mp4` — 1.68 s (shortest clip). Desert board, blue-gang steam units
clustered center-left, a red health (Repair) heart bonus on the ground at left.

**What happens.** A subtle **ground-level dust effect** near the units — small, faint,
low, soft puffs at unit feet/base (the kind a unit kicks up while moving). The clip is
deliberately short and the effect is **low-density and faint**: a frame-difference pass
shows only a small localized swirl near one unit, no large plume.

**Temporal profile.** Continuous-but-faint over the 1.7 s — a small soft dust puff that
rises slightly and dissipates near a unit's base. No flash, no burst, no colour beyond a
dusty tan tint blending with the terrain.

**Fidelity detail.** **Dust = small, soft, ground-hugging, terrain-coloured, low-density,
short-lived.** It is the *movement* dust (kicked up while a unit walks), distinct from the
explosion debris ring of batch-1 clip 4. Colour follows the **terrain dust colour**, not a
gang tint.

**Investigation — is this effect present in our jME3 build? → PRESENT, not absent
(gated/tuned), with two paths:**
- **Baked particle assets exist:** `assets/rsrc/effects/frontier_town/dust/` (with
  `dust.png`, `particles.jme`, `particles.properties`) and `…/dust_small/` — and both are
  **baked to `.j3o`** (`assets/build/staging/rsrc/effects/frontier_town/dust*/particles.j3o`).
  Texture `assets/rsrc/textures/effects/dust.png` is present and baked.
- **Movement dust is wired in code** (`client/shared/.../game/client/sprite/MobileSprite.java`):
  `createDustManager()` builds a jME3 `ParticleEmitter` (`new ParticleEmitter("dust",
  Type.Triangle, 32)`, `dust.png` material, `LowLife 0.5 / HighLife 1.5`, start/end size
  ~`TILE_SIZE/5 → /3`), and the move start/stop logic toggles `setParticlesPerSec(32)` ↔
  `0` while `isMoving()`, recolouring per-frame from `BoardView.getDustColor()` (terrain
  colour). **So the "kick up dust while moving" behaviour is implemented.**
- **A second dust path exists** for impacts: `PieceSprite.displayDustRing()` →
  `ParticlePool.getDustRing()` (used by `ExplosionViz` / `EffectHandler`).

> **Finding (flagged):** the dust effect is **NOT genuinely missing** at the asset/class
> level — assets bake and the emitter is wired. **But two gates make it easy to see
> "nothing":** `createDustManager()` early-returns when **`!BangPrefs.isHighDetail()`**
> *and* for **flyers** (`_piece.isFlyer()`) — so on anything below high detail there is
> **no movement dust at all**. On top of that the jME3 emitter is an **approximate
> re-implementation** (a code comment notes the fork's ms lifetimes/velocity-cone/spin
> were only roughly ported, with *"fine tuning… deferred to the Phase-4 effects pass"*).
> So if the user perceives dust as missing in jME3, the most likely causes are **(a)
> detail level not set to High**, or **(b)** the Phase-4-deferred emitter tuning making it
> too faint/short vs this golden. Recommend: verify on High detail first, then tune the
> `MobileSprite` emitter against this clip — **not** treat it as a missing effect.

**Maps to:** movement-dust emitter (`MobileSprite.createDustManager`) tuning + detail-level
gate; the baked `frontier_town/dust` / `dust_small` assets. Cross-ref
[`../game-reference/effects-and-cards.md`](../game-reference/effects-and-cards.md)
(`dust`/`dust_small` in the frontier_town effect list; `ParticleEmission` movement dust).

---

## 13. `SteamGunmanPicksUpClock`

`video/SteamGunmanPicksUpClock.mp4` — 2.33 s. Desert board with a circular claim/ring of
posts; a gold pocket-watch bonus sits on the ground.

**What happens.** A **Steam Gunman** moves onto a tile holding a **clock bonus** and
**collects** it; a **burst of yellow/gold star sparkles** plays at the unit on pickup.

**Temporal profile.**
- **0–~1.5 s — approach.** The gold **pocket-watch** bonus sits on the board; the steam
  unit (blue selection arc) moves toward it across the claim ring.
- **~1.6–2.0 s — pickup.** A **spray of yellow/gold star particles** bursts around the
  unit as it lands on the bonus tile (the `get_bonus` sparkle).
- **~2.0–2.3 s — settle.** Stars fade; the bonus is consumed.

**Fidelity detail.** Pickup effect = **bright yellow/gold star-shaped sparkles** radiating
from the unit, short-lived (~0.3 s). The bonus prop is a **gold pocket-watch / clock**.

**Which bonus.** The "clock" is the **Hustle bonus** — its board model is textured with
`assets/rsrc/bonuses/frontier_town/hustle/pocketwatch.png` (Hustle = *"move every 3 ticks
instead of 4"*; the clock/watch imagery = faster ticking). Same `get_bonus` star-sparkle
family as batch-1 clip 7 (`SteamGunmanPickupBonus`).

**Maps to:** `get_bonus` pickup particle + the **Hustle** bonus prop. Cross-ref
[`../game-reference/effects-and-cards.md`](../game-reference/effects-and-cards.md)
(`frontier_town/get_bonus`; Hustle bonus/card). Compare unit:
`renderModelToPng -PmodelType=units/frontier_town/steamgunman -Panim=walking`.

---

## 14. `TacticianPicksUpHealth`

`video/TacticianPicksUpHealth.mp4` — 4.22 s. Canyon/mesa board (rocks, green-grass
patches); a red **health (Repair) heart** bonus on the ground.

**What happens.** A **Tactician** moves across its (green) movement-range tiles onto a
**red heart health item** and **picks it up**, triggering a **heal burst**.

**Temporal profile.**
- **0–~2.0 s — move plan.** Green movement-range tiles light up; the **red heart** bonus
  sits on a central tile; Tactician (blue selection arc) at lower-right.
- **~2.0–2.7 s — move onto item.** Tactician walks onto the heart tile.
- **~2.7–3.2 s — heal.** A **bright white/blue radial sparkle burst** blooms centred on
  the Tactician as the heart is consumed (the repair/heal pickup visual).
- **~3.2–4.2 s — settle.** Burst fades; unit healed.

**Fidelity detail.** Heal pickup = a **white/blue (cool-toned) radial sparkle burst**
around the unit — distinct in colour from the **warm yellow/gold** `get_bonus` star spray
of the clock pickup (clip 13). The item prop is the **red heart** (Repair bonus).

**Which fix/verification it informs.** Validates the **repair/heal pickup effect** (cool
white/blue burst) and the Repair bonus prop. Note the **colour contrast vs the gold
`get_bonus` sparkle** — verify the heal effect uses the cool palette, not the gold one.

**Maps to:** repair/heal pickup particle (`RepairEffect` / repair effect asset) + the
Repair bonus prop. Cross-ref
[`../game-reference/effects-and-cards.md`](../game-reference/effects-and-cards.md)
(`RepairEffect`/`AreaRepairEffect`; `frontier_town/repair` effect; Repair bonus/card).
Compare unit: `renderModelToPng -PmodelType=units/frontier_town/tactician -Panim=walking`.

---

## 15. `ArtilleryDestroysHomestead`

`video/ArtilleryDestroysHomestead.mp4` — 2.40 s. Town board (fenced lots, crates, a
blue-roofed building) — a **Land Grab** scenario with **homestead** structures.

**What happens.** An **Artillery** fires a ballistic shell that **destroys a homestead**
(a board structure, not a unit). A death-skull targeting reticle marks the structure; the
shell impacts and the building is reduced to a damaged/rubble state.

**Temporal profile.**
- **0–~0.8 s — aim.** Death-skull reticle on the homestead; a faint smoke wisp as the
  shell arcs in.
- **~0.9–1.3 s — impact/explosion.** A **bright orange/yellow explosion flash** with a
  **radial fan of debris sparks** erupts at the structure (warm additive core + outward
  debris — the same explosion family as a unit kill, but on a building).
- **~1.4–2.4 s — aftermath.** Flash/debris fade; the structure is left as a **darkened /
  damaged shape** and its ownership/selection arc changes (the claimed homestead is
  knocked out). **No humanoid `dying`, no dropped nugget** — this is a *structure* death.

**Fidelity detail — structure death vs unit death.** Visually similar warm explosion
(flash → radial debris), but the death *result* differs: a building swaps to a
**destroyed/rubble state** rather than playing a unit `dying`→prone or a steam wreckage
swap, and **drops no carried item**. Capture: explosion flash colour/blend, debris fan
extent, and the building's destroyed-state swap timing.

**Which fix/verification it informs.** Confirms the **homestead/structure-destruction
effect** path. In code this is the **Land Grab** `Homestead` piece (`HomesteadSprite` →
`TargetableActiveSprite`; `HomesteadEffect`); the destruction explosion comes through the
effect/`EffectHandler` path (same `ParticleEffectViz`/explosion family as unit kills).
Verify the homestead plays the explosion + swaps to its destroyed model in jME3.

**Maps to:** explosion particle lifecycle (structure variant) + structure-death model
swap. Cross-ref
[`../game-reference/effects-and-cards.md`](../game-reference/effects-and-cards.md)
(`explosion` effect; `HomesteadEffect`) and the batch-1 explosion reference (clip 4).

---

## 16. `DirigibleKillsArtillery`

`video/DirigibleKillsArtillery.mp4` — 3.95 s. Town board (fenced lots), multiple steam
units (Artillery ×2) and a tower/structure in frame.

**What happens.** An **airborne Dirigible** (flying steam unit) attacks and **kills an
Artillery** (steam siege unit). Damage readouts ("25", red "50") and a death-skull resolve
on the target; the Artillery is destroyed.

**Temporal profile.**
- **0–~1.5 s — manoeuvre/aim.** Units reposition; the death-skull reticle resolves on the
  target Artillery; gang selection arcs (green/blue/yellow) on pieces.
- **~1.6–2.2 s — fire.** The Dirigible fires (a small warm muzzle flash / gunshot burst);
  a green effect + a "25", then a red **"50"** damage number rise on the target.
- **~2.2–3.9 s — kill/settle.** The Artillery is destroyed (steam-unit kill — wreckage
  swap, no humanoid death); readouts fade.

**Fidelity detail — the Dirigible unit.** The Dirigible is a **flying/airborne** unit: it
sits **elevated above the board with an offset ground shadow** (not grounded like the
Artillery). It carries **twin `GunshotEmission`** muzzles (L/R) and an **always-on
`SmokePlumeEmission` engine plume**. Board scale here keeps per-bone detail low; the
takeaways are **(a) the flyer's elevation/shadow**, **(b) twin gunshot flash on
`shooting`**, and **(c) the steam-unit (Artillery) kill = wreckage swap**, consistent with
batch-1 clip 10's steam death (no fiery humanoid death; Artillery has no `dying` clip).

**Which fix/verification it informs.** Reference for the **Dirigible** appearance
(elevated flyer + engine plume + dual muzzle) and the steam-unit kill path. Note the
Dirigible's death special (`Dirigible.didDie` → `CrashEffect`, *"crashes into an enemy
when killed"*) is **not** shown here (the Dirigible is the killer); its assets confirm a
**`dead` model** (mechanized death swap) and **no `dying.mxml`**.

**Maps to:** Dirigible posed-skinning + flyer elevation/shadow + twin `GunshotEmission` +
`SmokePlumeEmission` continuity; steam-unit (Artillery) wreckage kill. Compare:
`renderModelToPng -PmodelType=units/frontier_town/dirigible -Panim=shooting` (and
`-Panim=standing`). Cross-ref
[`../game-reference/units-and-big-shots.md`](../game-reference/units-and-big-shots.md)
(Dirigible + Artillery rows; *"Artillery & Dirigible have no `dying` clip"*) and
[`../game-reference/effects-and-cards.md`](../game-reference/effects-and-cards.md)
(`SmokePlumeEmission`, `CrashEffect`).

---

## 17. `ScrollwheelRotatesCamera` — camera reference

`video/ScrollwheelRotatesCamera.mp4` — 4.33 s. Desert/rail board overview.

**What happens.** The **scroll wheel rotates (orbits/swings) the camera around the board**:
the whole board yaws around a pivot — the rail line and units sweep around while the pitch
and zoom stay constant — returning toward the start orientation over the clip (a full
swing sweep). It is an **azimuth/orbit rotation**, not a pan or zoom.

**Temporal profile.** Discrete-feeling **~90° swing steps** as the wheel is rolled; the
board orientation changes in increments, each a smooth swing to the next quarter-turn.

**Which fix/verification it informs.** Documents the **scroll-wheel-rotate** camera control
(a control absent from batch 1, which only had board-action clips). **Present in our jME3
build:** `GameInputHandler` installs a BUI `MouseWheelListener` (`_swingListener`) whose
`mouseWheeled` calls **`swingCamera(±FastMath.PI/2)`** — i.e. **scroll wheel = ±90°
camera swing**, exactly this clip's behaviour. **Verify** the BUI wheel event is actually
delivered through the jME3 input backend so `swingCamera` fires (relevant to our input
work). Note the jME3 keyboard `GodViewHandler` *also* binds **Left/Right arrows = orbit**,
Up/Down = zoom, WASD/arrows = pan, Home/End = tilt — so rotate has both a wheel and a
keyboard path.

**Maps to:** camera input — scroll-wheel swing (`GameInputHandler._swingListener` →
`CameraHandler.swingCamera`/`orbitCamera`). Relevant to the input/camera work alongside the
WASD-pan fix. (No unit involved — no offscreen render.)

---

## 18. `StartGameAndZoomOut` — camera reference

`video/StartGameAndZoomOut.mp4` — 2.22 s. Desert board at game start.

**What happens.** At game start the **camera pulls back / zooms out**: it begins **zoomed
in** close on the starting units and **dollies/zooms out** to reveal the full board
overview (the playing distance).

**Temporal profile.** Smooth continuous **zoom-out** over ~2 s from a close start to the
board overview — a single distance change (no rotation).

**Which fix/verification it informs.** Documents the **game-intro zoom-out** camera move
(distance change, distinct from the swing/orbit of clip 17). Camera distance/zoom in jME3
goes through `CameraHandler.zoomCamera` (keyboard Up/Down map to `ZOOM_IN`/`ZOOM_OUT` in
`GodViewHandler`); `GameInputHandler` also holds preset `CAMERA_ZOOMS = {150, 112.5, 75}`
and `CAMERA_ANGLES`. **Verify** the game-start sequence performs this zoom-out (a scripted
camera path to the playing distance) and that the resulting distance matches this golden.
Relevant to the input/camera work (the *other* expected camera behaviours to verify in
jME3, beyond the WASD pan we fixed).

**Maps to:** camera zoom/distance (`CameraHandler.zoomCamera`, `CAMERA_ZOOMS` presets) +
the game-intro camera path. (No unit involved — no offscreen render.)

---

## 19. `GameLoadScreen` — UI reference (lower priority)

`video/GameLoadScreen.mp4` — 5.57 s. The pre-game load/roster screen over a board
background.

**What happens.** The **game load screen**: a **row of unit/big-shot portrait cards** (the
team roster) with **name labels** and small stat readouts beneath each, a **scenario title
banner** at top (e.g. *"Round 1 of 1 — … Wendigo…"*), shown over the board as it is
prepared, with loading progress.

**Temporal profile.** Mostly static UI held for the load duration; portrait cards and the
scenario title resolve in, board readies behind.

**Fidelity detail.** Layout reference: horizontal roster of portrait cards (icon +
name + stats) + a centred scenario title/round banner, over the board backdrop. Font /
panel styling is the BUI in-game UI family.

**Which fix/verification it informs.** **UI reference only** (lower priority): the
pre-game roster/scenario load layout — useful when verifying the jME3 BUI load screen
matches (card row composition, scenario banner, name/stat labels). No unit anim, effect,
or camera content. (No offscreen render.)

---

## Re-filming notes (batch 2)

The batch-1 marquee gap (Tactician umbrella deflect) is now **filled by clip 11**. Remaining
asks, in batch-1's spirit:

1. **Tighter crop on the umbrella deflect (clip 11).** It's the bug-fix target but filmed
   board-scale; a **tight 1-unit framing of just the Tactician** through the open→deflect→
   close would pin the exact canopy shape/rib deformation we must match.
2. **Dirigible at closer range (clip 16).** Confirm the flyer elevation/shadow offset and
   the twin muzzle flashes; board scale here is at the detail floor.
3. **A Dirigible *death* (`CrashEffect`)** — clip 16 shows it killing, not dying; the
   crash-into-enemy death is still uncaptured.
4. **Dust at a fixed close zoom (clip 12)** — the effect is faint; a tight, slow walk on
   one unit on **High detail** would give a cleaner density/colour/lifetime reference to
   tune the `MobileSprite` emitter against.
