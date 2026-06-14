# Effects, Emissions & Cards

The visual layer of the game splits into three families, all of which the jME3 cutover
must reproduce:

1. **Unit emissions** — `SpriteEmission` controllers wired in each unit's
   `model.properties`, fired at named animation frames (muzzle flash, smoke, dust, etc.).
2. **Particle / effect assets** — baked particle systems under `assets/rsrc/effects/<town>/*`
   (explosion, fire, dust, lightning…), played by `ParticleEffectViz` / `EffectViz`.
3. **Game effects** — the `game/data/effect/*Effect.java` classes (the rules result of an
   action) and **cards/bonuses/hindrances** that trigger them.

> **Pipeline (from `docs/fidelity-reference/README.md` + `engine-notes.md`):** particles are
> baked by `tools/j3o-converter` `ParticleConverter` (`particles.jme` → `.j3o`) and played
> by `ParticleEffectViz`. Emissions are `AbstractControl`s on the unit's geometry that
> spawn at configured frames. **This is exactly the system the recent particle-effect fix
> touched** — explosion debris, smoke plumes, gunshot flashes.

---

## Unit emissions (controllers)

Source: `client/shared/.../game/client/sprite/*Emission.java`. Bound per-unit via
`model.properties → controllers = …` with `<ctrl>.class` and `<ctrl>.animations` (and
optional `<ctrl>.<anim>.frames` / `.shot_frame`).

| Emission class | What it draws | Triggered on | Used by (units) |
|----------------|---------------|--------------|-----------------|
| `GunshotEmission` | Muzzle flash + shot streak at the gun barrel | `shooting`, `returning_fire` at configured `frames` | Gunslinger (L+R pistol), Shotgunner (L+R barrel), Cavalry (rifle), Sharpshooter (rifle), Codger (blunderbuss), Tactician, Steam Gunman (pistol+cannon), Dirigible (L+R), Revolutionary, Trickster Raven (pepperbox), Forklift/One-armed Bandit (steamgunman set) |
| `DudShotEmission` | A *dud* (no-effect) shot — used when the Dud card neutralises a shot | `shooting` at `shot_frame` | every gun_user (paired with its GunshotEmission) |
| `MisfireEmission` | *"A large puff of black smoke"* (the Misfire card backfire) | `shooting`/`returning_fire` at `shot_frame` | every gun_user |
| `SmokePlumeEmission` | *"A plume of smoke (particle system)"* — persistent engine/exhaust smoke | continuous across `standing`/`walking`/`shooting`/`reacting` | Steam Gunman, Dirigible (engine), Logging Robot ×2, Locust Robot ×4 + rocket |
| `ParticleEmission` | A generic continuous particle effect attached to a node | configured anims | Buffalo Rider `dust_trail` (charge dust) on `shooting`/`shooting_run` |
| `TransientParticleEmission` (extends `FrameEmission`) | *"Transient particle effects when the sprite reaches configured frames"* — one-shot bursts | at frames in `dying`/`reacting`/`shooting`/`flying_start` | Dog Soldier `Dust_Spawn` (dying/walking_end), Thunderbird `get_hit` (reacting), Revolutionary `death` (dying), Storm Caller `emitter`+`charge_up` (shooting/lightning), Trickster Raven `transform` (flying_start morph), Underdog Soldier |
| `FrameEmission` (abstract base) | — | — | base class for the above frame-timed emitters |
| `SpriteEmission` | — | — | base class for all emissions |

**Conversion watch-list (highest visual payoff):**
- **`GunshotEmission` frame timing** — the per-pistol `frames`/`shot_frame` offsets
  (e.g. gunslinger `L=10,18 R=4,14`) must line up with the re-baked `shooting` clip or the
  flash detaches from the barrel. Tied to fidelity §3.
- **`SmokePlumeEmission`** is *always-on* on steam units — a continuously emitting system,
  must not be one-shot.
- **`TransientParticleEmission`** one-shots (Storm Caller bolt, Raven morph, Revolutionary
  respawn flash, Dog Soldier dust) are the spectacular per-big-shot effects.

---

## Particle / effect assets (`assets/rsrc/effects/<town>/`)

Each is a baked particle system (`particles.jme` + `particles.properties` + texture PNGs,
e.g. `explosion/{explosion.png, smoke.png, particles.jme, particles.properties}`).

**`frontier_town/`:** `bonus`, `dust`, `dust_small`, **`explosion`**, **`fire`**,
`get_bonus`, `hit_flash`, `hustle`, `joker`, `lasso`, `lucky`, **`mushroom_cloud`**,
`nugget`, `repair`, `resurrection`, `sea_spray`, `shotgunner_shells`, `staredown`.

**`indian_post/`:** `ambient_drizzle`, `ambient_leaves`, `ambient_snow`, `buffalo_rider`,
`dog_soldier`, `dream_catcher`, `falling_leaves`, `fireflies`, `forgiven`, `heal_hero`,
`hero_death`, `level_up`, `magic_fire`, `mystical_ground`, `peaced_out`, `rain`,
`revolutionary`, `safe_spot`, `snare_leaves`, `storm_caller`, `talisman`, `teleporter_1/2/3`,
`thunderbird`, `totem`, `tree_bed`, `trickster_raven`, `tumbleweed_wind`, `wendigo`.

**`boom_town/`:** `barrel_explosion`, `buggy_logic`, `fireworks`.

The per-big-shot effect assets (`storm_caller`, `revolutionary`, `trickster_raven`,
`buffalo_rider`, `dog_soldier`, `dream_catcher`, `thunderbird`) are the particle
counterparts of those units' emissions — pair them when validating fidelity.

> **Fidelity targets:** `explosion` (warm additive flare + dark debris ring; **fidelity
> §5 `explosionAndDeath`**), `mushroom_cloud`, `fire` (Firestarter), `hit_flash` (impact),
> `dust`/`dust_small` (movement & charge). The fidelity README calls the explosion the
> *"headline test for `ParticleConverter` + `ParticleEffectViz`"* and asks for video
> (flash → expanding debris ring → smoke dissipation).

---

## Game effects (`game/data/effect/*Effect.java`)

The rules-side result objects. Visual-bearing ones (each has a matching emission or
particle asset and a sprite-side `*Viz`):

| Effect | Visual | Source |
|--------|--------|--------|
| `ShotEffect` / `BallisticShotEffect` | straight vs arcing shot + impact | base attacks; Artillery/Tactician (ballistic) |
| `ChainingShotEffect` + `LightningEffect` | chaining lightning bolts | Storm Caller, Lightning card |
| `NightmareShotEffect` | dream/nightmare projectile + knockback | Dream Catcher |
| `BuzzsawShotEffect` | flying saw blade | Logging/Locust Robot |
| `SpringShotEffect` / `PuntEffect` | target punted/sprung away | Forklift, Spring card |
| `ProximityShotEffect` | adjacent-AoE melee swing | Dog Soldier, Revolutionary sword |
| `MoveShootEffect` | shoot mid-move (swoop/charge) | Thunderbird, Buffalo Rider |
| `ExplodeEffect` / `AreaDamageEffect` / `RocketEffect` / `Missile` | explosion + AoE | One-armed Bandit EXPLODE, Missile card, robots |
| `CrashEffect` | unit crashes into enemy on death | Dirigible |
| `FirestarterEffect` / `OnFireEffect` | units catch fire | Firestarter card |
| `StampedeEffect` / `RockslideEffect` | buffalo stampede / sliding rocks | Stampede, Rockslide cards |
| `RepairEffect` / `AreaRepairEffect` / `HealHeroEffect` | repair/heal sparkle | Repair cards, Dream Catcher self-heal |
| `ResurrectEffect` | respawn flash | Revolutionary, Reinforcements |
| `TeleportEffect` | teleporter shimmer | Teleporter pieces |
| `WendigoEffect` | demon sweep across board | Wendigo Attack scenario |
| `SaintElmosEffect`, `PlagueEffect`, `SnakeBiteEffect`, `MonkeyWrenchEffect` | DoT / status auras | town bonuses & hindrances |
| `HoldEffect` | drop/attach carried item | Sharpshooter, Stick Up! card, carrying gold |
| `DudEffect` / `MisfireEffect` | dud / backfire smoke | Dud / Misfire cards |

Influences (timed buffs/debuffs) live as `Influence` / `Hindrance` / `HeroInfluence` on a
piece; `RandomInfluenceEffect` is the One-armed Bandit's slot-machine roll.

---

## Cards & bonuses

### Pickup bonuses on the board (`assets/rsrc/bonuses/<town>/`)

Each is a small `model.mxml` + textures sitting on the board to be picked up.

- **`frontier_town/`:** `bonus_point`, `card`, `defect`, `duplicate`, `fools_nugget`,
  `hustle`, `iron_plate`, `joker`, `lady_luck`, `missile`, **`nugget`** (gold — has
  `holding.png` for the carried state), `power_up`, `ramblin`, `repair`, `spring`.
- **`indian_post/`:** `fetish_bear`, `fetish_fox`, `fetish_frog`, `fetish_turtle`,
  `firestarter`, `forgiven`, `heal_hero`, `snare`, `spirit_walk`, `talisman`,
  `totem_crown`, `totem_large/medium/small` (Totem Building stack pieces), `trap`.
- **`boom_town/`:** `saint_elmo`.
- **`ghost_town/`:** `plague` (next town, art present).

**Carrying gold:** the `bonuses/<town>/nugget` model is the prop that `UnitSprite`
attaches to a unit when `unit.holding` is set (its `holding.png` is the carried skin).
This is the **fidelity §6 `GreenCharactersCarryingGold`** reference.

### Playable cards (`game/data/card/*.java`, copy in `i18n/cards.properties`)

Full list with effect (the steam-targeting ones are boom_town-flavoured):

| Card | Effect | Card | Effect |
|------|--------|------|--------|
| Barricade | blocks units (placed obstacle) | Joker | placed card that explodes when stepped on |
| Dud | next shot has no effect | Lasso | turns a board bonus/trap into a card |
| Fool's Gold | fake nugget decoy | Lucky Horseshoe | 50% chance of 2× damage |
| Giddy Up / Half Giddy Up | move again now / 2 ticks sooner | Misfire | next shot backfires (damage to self) |
| High Noon | global heat — limits all movement | Missile | AoE damage in a radius |
| Hollow Point | +30% attack | Ramblin' | +1 move until it acts |
| Hustle | move every 3 ticks instead of 4 | Reinforcements | spawn an extra Gunslinger |
| Iron Plate | immune to attacks for 7 ticks | Repair / Half Repair | heal full / half |
| Engineer | choose train destination | Staredown | reset target's move counter |
| Stick Up! (`drop_nugget`) | target drops carried item | Bear Trap (`trap`) | board trap snaps shut |
| Spring | hidden launch trap | Discard (`cat_ballou`) | remove a random opponent card |
| Eagle Eye | +range buff to long-range gun users | Firestarter | sets targets on fire |
| Forgiven | heals all living units | Jack Rabbit | +3 move for the turn |
| Lightning | chaining lightning damage | Peace Pipe | target can't attack for a while |
| Rain | clears all influences | Rockslide | rocks slide downhill, damaging units |
| Snake Bite | DoT on a human unit | Snare | board snare immobilises |
| Spirit Walk | walk over any terrain | Stampede | buffalo stampede across board |
| Tumbleweed Wind | clears all wreckage | Underdog Soldier | spawn an unaffiliated Dog Soldier |
| **Blown Gasket** | steam unit immobile after 6 steps | **Buggy Logic** | order an opponent's steam unit |
| **Dust Devil** | resurrect steam wreckage into a unit | **Monkey Wrench** | steam unit takes damage each tick |

(Sources: `i18n/cards.properties` tips + the `card/*.java` class list.)
