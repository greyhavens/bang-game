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
