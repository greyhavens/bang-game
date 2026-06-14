# Mechanics & Context

Enough of the game model to make sense of what's on screen for the sprite/effect work.

## Towns

`BangCodes.TOWN_IDS = { frontier_town, indian_post, boom_town }` (with `ghost_town`,
`city_of_gold` defined but commented out / not yet shipped). A town gates which units,
big shots, bonuses and scenarios are available; asset jars and avatar bundles are built
per-town, and the server runs one cluster node per town. The repo ships unit art for
`frontier_town`, `indian_post`, `boom_town` plus `wreckage` debris and `ghost_town`
bonus art.

| Town | Theme | New units (see units-and-big-shots.md) |
|------|-------|----------------------------------------|
| **Frontier Town** | classic wild-west desert/rail | gunslinger, shotgunner, steam gunman, sharpshooter, artillery, dirigible; big shots Tactician / Old Codger / Cavalry |
| **Indian Trading Post** | forests, spirits, robots | buffalo rider, dog soldier, dream catcher, thunderbird, logging/locust robots, underdog soldier; big shots Revolutionary / Storm Caller / Trickster Raven |
| **Boom Town** | steam/industrial, gold mining | forklift, one-armed bandit, windup gunslinger (mostly steam re-skins; some `TODO` content) |

## Combat / turn model

Bang! Howdy is a real-time-with-ticks tactics game (not strictly turn-based):

- **Ticks.** The board advances on a steady tick. A unit may act (move/shoot) once its
  personal cooldown elapses; **`move`** (tiles per move) and **`initiative`** in
  `unit.properties` set how often. Cards like *Hustle* (act every 3 ticks vs 4),
  *Giddy Up*, *Staredown* and the *Dream Catcher / Old Codger* tick-reset powers all
  manipulate this counter — hence the floating **tick/initiative counters** on pieces.
- **Make × Mode damage table.** Every `unit.properties` carries `damage.{range,ground,air}`,
  `damage.{human,steam,spirit}` and the matching `defense.*` modifiers. `make ∈ {human,
  steam, spirit}`, `mode ∈ {ground, air, range}`. Examples grounded in the data:
  Gunslinger `damage.steam = -15` (weak vs steam); Artillery `damage.air = -35` (can't
  hurt flyers); Dirigible/Trickster fly (`mode=air` / `isFlyer`) and ignore terrain;
  Thunderbird is `make=spirit` so it sidesteps most human/steam modifiers.
- **Attack resolution** produces a `ShotEffect` subclass (straight, ballistic-arc,
  chaining, proximity, nightmare, buzzsaw…) → damage + a particle/emission visual + a
  floating **damage number** (`DamageIconViz`, see fidelity §2–4).
- **Death.** Humans play `dying` and hold the last pose; mechanized/steam units swap to a
  **`dead` model** and scatter **wreckage** props + an **explosion** particle (fidelity §5).
- **Big-shot powers are passive/triggered** — players never manually activate them
  (corroborated by the Gamezebo review): the Gunslinger's return fire triggers on being
  shot, the Tactician's umbrella deflect triggers on incoming ballistic fire, etc.

## Gold, nuggets, claims, homesteads

- **Nuggets/gold** (`bonuses/<town>/nugget`, piece `Nugget`/`Claim`/`Homestead`/`Cow`):
  objective items units pick up and carry. Carrying is rendered by attaching the nugget
  **model as a child prop** to the unit (`UnitSprite`, `unit.holding`) — it rides the
  `walking` cycle (fidelity §6). The `Sharpshooter` and *Stick Up!* card knock carried
  items loose; *Fool's Gold* places decoys.
- **Claims / gold stashes** show a floating **counter number** (`GenericCounterNode` /
  `CounterSprite`, "Old Town" bold 72, tinted to owner gang colour) — fidelity §1.
- **Homesteads** (Land Grab) are claimable, attackable board pieces.

## Scenarios (`game/data/scenario/*Info.java`)

| Scenario (IDENT) | Class | Objective |
|------------------|-------|-----------|
| **Cattle Rustling** (`cr`) | `CattleRustlingInfo` | brand & herd cattle (the `Cow` piece) into your area |
| **Claim Jumping** (`cj`) | `ClaimJumpingInfo` | seize/hold claims; steal from opponents' claims |
| **Gold Rush** (`gr`) | `GoldRushInfo` | collect loose gold nuggets, return them to your mine |
| **Land Grab** (`lg`) | `LandGrabInfo` | claim homesteads scattered on the board (attackable) |
| **Forest Guardians** (`fg`) | `ForestGuardiansInfo` | co-op: protect growing tree seeds, fight off **logging/locust robots** |
| **Totem Building** (`tb`) | `TotemBuildingInfo` | stack totem elements (`totem_small/medium/large/crown`) on a base for points |
| **Hero Building** (`hb`) | `HeroBuildingInfo` | level up a hero unit (`HeroInfluence`, `heal_hero`, `level_up` fx) |
| **Wendigo Attack** (`wa`) | `WendigoAttackInfo` | survive periodic demon (`Wendigo`) sweeps; reach safe zones / get protective items |
| **Practice / Tutorial** (`pr` / `tu`) | `PracticeInfo` / `TutorialInfo` | training |

`NuggetScenarioInfo` is the shared base for the gold-collecting scenarios. (Scenario
display names live in compiled config, not a plain i18n file in-repo; the names above are
the standard ones corroborated by `banghowdy.wiki` search snippets.)

## Gangs & team colours — *load-bearing for sprite tinting*

Source: `client/shared/.../bang/client/BangMetrics.java`. Units are **colorized by their
owning player's gang colour** (the same `_zations` colour-swap the avatar system uses).

**Per-player palette** (`PIECE_COLORS` / `JPIECE_COLORS`, index = colour slot):

| Slot | Colour | Slot | Colour |
|------|--------|------|--------|
| 0 | **Gray** (unowned / neutral) | 4 | **Yellow** |
| 1 | **Blue** `(0.08,0.33,1)` | 5 | **Orange** `(1,0.47,0)` |
| 2 | **Red** | 6 | **Cyan** `(0.08,0.73,1)` |
| 3 | **Green** | 7 | **Violet** `(0.43,0.12,1)` |

- **Default free-for-all lookup** is `{gray, blue, red, green, yellow}` → players 1–4 are
  **blue, red, green, yellow**. So the **"green gang"** in fidelity §6
  (`GreenCharactersCarryingGold`) = **player slot 3** in a standard match — a normal
  gang-coloured team, not a special faction.
- **Owner −1 = unowned** → slot 0 **gray** (neutral pieces, scenario units, the underdog
  soldier). The One-armed Bandit / scenario robots / Wendigo / Underdog Soldier are
  unaffiliated and render gray.
- **Team play** uses `TEAM_COLOR = {{1,6,7},{2,5},{4}}`: team A draws from the
  blue/cyan/violet family, team B from red/orange, team C yellow — so allied players are
  recognisably the same hue.
- **Derived tints:** `DARKER_COLORS` (gang colour × DarkGray) is used for **outlines** on
  the billboarded counter/damage numbers (`getDarkerPieceColor`). Damage numbers take the
  **target's** gang colour; counter numbers take the **owner's**.

**Conversion note:** verify the colorization plumbing applies the right slot colour to the
right body regions and that the darker-outline derivation matches the fork — this is the
single most visible "whose unit is this" cue and is called out in fidelity §6 (green tint)
and §1–4 (number tints).

## Cross-references

- Per-unit special powers, clips, props, and emissions: [units-and-big-shots.md](units-and-big-shots.md).
- Effect/particle/emission types and the full card list: [effects-and-cards.md](effects-and-cards.md).
- Visual targets (golden screenshots): [../fidelity-reference/README.md](../fidelity-reference/README.md).
