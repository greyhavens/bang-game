# Units & Big Shots

Per-town tables of every unit shipped in `assets/rsrc/units/`. Each entry gives the
**display name** (from `i18n/units.properties`), the **repo identifier / model path**,
the **rank/make/mode** and key stats (`unit.properties`), the **special power** (i18n
`*_power` text + the `piece/*.java` mechanic), the **animation clips** (the `*.mxml` files
present), the **visual-effect emissions** (`model.properties → controllers`), and a
**conversion note**.

- **Model path** for the render harness is `units/<town>/<id>`, e.g.
  `units/frontier_town/gunslinger`.
- **Render a clip**:
  `./gradlew :tools:j3o-converter:renderModelToPng -PmodelType=units/<town>/<id> -Panim=<clip> -Ptime=-1 -Pout=/tmp/x.png`
  (`-Panim=list` lists clips; `-Ptime=-1` poses the midpoint).
- **Standard clip set** (almost every humanoid): `standing`, `walking_start`,
  `walking_cycle`, `walking_end` (the `walking` sequence), `shooting`, `reacting`,
  `dying`. The animated **`dead`** state is a separate model/pose for mechanized units
  (per fidelity README); humans hold the last `dying` frame.
- **Emission classes** (the visual effects fired at animation frames) are documented in
  [effects-and-cards.md](effects-and-cards.md#unit-emissions-controllers).

> **Name mismatches (IGN/wiki ↔ repo id), call these out for the user:**
> - **Old Codger** (wiki) = repo `codger` (Tactician/Cavalry/Codger are the three
>   frontier big shots).
> - **One-armed Bandit** display name vs repo `onearmedbandit`; the class javadoc calls
>   it *"One Armed Jack"* — three names for one unit.
> - **Windup Gunslinger** = repo `windupslinger` (boom_town special; **no model files
>   present** — only `icon.png`, see gaps).
> - **Underdog Soldier** reuses the **`dogsoldier` model** (`model.properties name = dogsoldier`).
> - **Forklift** and **One-armed Bandit** both reuse the **`steamgunman` model**
>   (`model.properties name = steamgunman`) — their boom_town folders carry the steamgunman art.
> - **Super Locust / Super Logging robot** are `variant = super` re-skins of
>   `locust_robot` / `logging_robot` (`model = indian_post/locust_robot`); their own
>   folders have **no `.mxml`** — they share the base model + a `super_*` texture.
> - Repo `tricksterraven` ↔ `model.properties name = trickster` (folder vs model name differ).

---

## Frontier Town (`frontier_town`)

### Recruitable / normal units

| Unit | Repo id | Make / Mode | Move / Fire / Dmg / Init | Special power | Clips | Emissions |
|------|---------|-------------|--------------------------|---------------|-------|-----------|
| **Gunslinger** | `gunslinger` | human / ground | 3 / 2 / 35 / 4 | *"Free return shot on any unit that fires on her within range, always 50% strength."* `return_fire=100`. Balanced; −15 vs steam. | standing, walking(start/cycle/end), shooting, **returning_fire**, reacting, dying | `GunshotEmission` ×2 (L/R pistol) on shooting+returning_fire; `DudShotEmission`, `MisfireEmission` |
| **Shotgunner** | `shotgunner` | human / ground | 3 / 1 / **65** / 2 | *"Wide blast does +50% damage to units left & right of the main target"* (`Shotgunner.collateralDamage`). | standing, walking, shooting, reacting, dying | `GunshotEmission` ×2 (L/R barrel), dud, misfire |
| **Steam Gunman** | `steamgunman` | **steam** / ground | **5** / – / 35 / 2 | None (utility: big move, +vs range & human). Badge-unlock (`Cannon`). | standing, walking, shooting, reacting | `GunshotEmission` ×2 (pistol+cannon), **`SmokePlumeEmission`** (em_smoke, always-on while moving/shooting), dud ×2, misfire |
| **Sharpshooter** | `sharpshooter` | human / ground | 3 / 2–3 / 35 / 3 | *"Knocks any carried item out of the target's hands with one shot"* (`Sharpshooter.willShoot` → `HoldEffect.dropBonus`). Long range, low dmg. Badge-unlock (`Sniper Rifle`). | standing, walking, shooting, reacting, dying | `GunshotEmission` (rifle), dud, misfire |
| **Artillery** | `artillery` | steam / **range** | 2 / **2–5** / 35 / 1 | Long-range siege; **−35 vs air** (can't hit flyers). Class `BallisticUnit` → arcing `BallisticShotEffect`. | standing, walking, shooting, reacting *(no dying)* | dud, misfire *(no muzzle emitter — shell is ballistic)* |
| **Dirigible** | `dirigible` | steam / **air** | 3 / – / 35 / 3 | Flyer: ignores terrain. *"Crashes into an enemy within range when killed"* (`Dirigible.didDie` → `CrashEffect`). | standing, walking, shooting, reacting *(no dying)* | `GunshotEmission` ×2 (L/R), **`SmokePlumeEmission`** (engine plume, always-on), dud ×2, misfire |

### Big shots (`rank = bigshot`, one per match)

| Big Shot | Repo id | Make/Mode | Stats | Special power (visual!) | Clips | Emissions |
|----------|---------|-----------|-------|-------------------------|-------|-----------|
| **Tactician** | `tactician` | human / ground | move 3 / fire 3 / dmg 60 (range 25) | *"Deflects ballistic shots with his **umbrella**; an adjacent unit may be hit by the deflected shot."* `Tactician.deflect` redirects a `BallisticShotEffect`. Invulnerable to Artillery. | standing, walking, shooting, reacting, dying, **`blocking`** (umbrella-up deflect pose) | `GunshotEmission`, dud, misfire. **Prop: `umbrella.png`** (+ `tac_coat`, `tac_head`, `portrait`). |
| **Old Codger** | `codger` | human / ground | move 3 / fire 2 / dmg 50 (air 35, steam 15) | *"Resets the target's tick counter to zero with his **blunderbuss**."* (`Codger.unitShoot` → `AdjustTickEffect`). Tanky (+def ground/air); one-shots Dirigibles. | standing, walking, shooting, reacting, dying | `GunshotEmission` (blunderbuss), dud, misfire. **Prop: `blunderbuss.png`**. |
| **Cavalry** | `cavalry` | human / ground | **move 5** / fire 2 / dmg 40 (ground 20) | *"None"* — utility big shot: huge move, master of the ground (+atk/def vs ground). Mounted. | standing, walking, shooting, reacting, dying | `GunshotEmission` (rifle), dud, misfire. **Props: `horse.png`, `rider.png`, `rifle.png`** (mounted composite). |

**Conversion notes — frontier_town**
- **Gunslinger** is the headline anim-fidelity case (the known *anim-specific skinning
  defect* lives in its `shooting`/`returning_fire` clips). It is the only frontier unit
  with a dedicated `returning_fire` clip; both pistols emit on it (`emitter_L/R_pistol`).
  Cross-ref **fidelity §3 attackAndDeathAnimation** and the dual-pistol muzzle flash
  frames in `model.properties` (`L=10,18`, `R=4,14`).
- **Tactician**: the **`umbrella` prop + `blocking` clip** are the marquee thing to get
  right — verify the umbrella attaches and the deflect pose plays. Cross-ref fidelity
  README's call to film the tactician for umbrella-prop behaviour.
- **Steam Gunman / Dirigible** carry an **always-on `SmokePlumeEmission`** (engine
  smoke) across move + shoot + react — a persistent particle system, not a one-shot.
- **Artillery & Dirigible have no `dying` clip** (Artillery is steam siege, Dirigible
  flies/crashes) — they swap to wreckage/crash rather than playing a humanoid death.

---

## Indian Trading Post (`indian_post`)

### Recruitable / normal / special units

| Unit | Repo id | Make / Mode | Move / Fire / Dmg / Init | Special power | Clips | Emissions |
|------|---------|-------------|--------------------------|---------------|-------|-----------|
| **Buffalo Rider** | `buffalo_rider` | human / ground | **5** / – / 35 / 2 | *"The further she charges, the more damage; if ≥4 tiles away she pushes the target backwards"* (`BuffaloRider` → `MoveShootEffect`, push). `gun_user=false`, can't hit air. Mounted on bison. | standing, walking, **shooting_run** (charge attack), shooting, reacting, dying | **`ParticleEmission`** `dust_trail` on shooting+shooting_run (charge dust). Props: `bison.png`, `rider.png`. |
| **Dog Soldier** | `dogsoldier` | human / ground | 3 / – / 45 / 1 | *"Extra attack on every adjacent enemy every tick"* (`DogSoldier.tick` → `ProximityShotEffect`). Staked in place: immune to push. Badge-unlock (`Tomahawk`). | standing, walking, **proximity** (the per-tick AoE swing), shooting, reacting, dying | **`TransientParticleEmission`** `Dust_Spawn` on dying + walking_end. Props: `body_skin`, `weapons_skin`, `feather`, `blur`. |
| **Dream Catcher** | `dreamcatcher` | human / **range** | 2 / **4** / 5 / 1 | *"Resets target's tick counter, knocks them back with nightmares, and absorbs the damage as her own health"* (`DreamCatcher` → `NightmareShotEffect` + self-`RepairEffect`). Class extends `BallisticUnit`. | standing, walking, shooting, reacting, dying | *(none in model.properties — the nightmare visual is the `NightmareShotEffect`/`dream_catcher` particle asset)*. Props: `skin`, `alpha` (transparency). |
| **Thunderbird** | `thunderbird` | **spirit** / **air** | **5** / 0 / 40 / 3 | *"Attacks his target on the way to his destination (swoop), instead of after arriving"* (`Thunderbird.generateMoveEffect` → `MoveShootEffect`). Spirit = ignores most make modifiers. | standing, walking, shooting, reacting, dying | **`TransientParticleEmission`** `get_hit` on reacting. Props: `main`, `alpha`. |
| **Logging Robot** | `logging_robot` | steam / **range** | 3 / 3 / 35 / 2 | Scenario unit (Forest Guardians). *"Flying-blade saw attack."* Class `LoggingRobot` → `BuzzsawShotEffect`. `scrip_cost=-1` (not buyable). | standing, walking, **proximity**, **unfolding** (deploy), shooting, reacting | **`SmokePlumeEmission`** ×2 (smoke_L/R, always-on). Props: `blades`, `*_emissive` glow maps. |
| **Locust Robot** | `locust_robot` | steam / ground | **5** / 1 / 45 / 2 | *"Flies over great distances to cut trees with twin saws"* (mobile variant of LoggingRobot; **−45 vs air**). | standing, walking, **unfolding**, shooting, reacting | **`SmokePlumeEmission`** ×4 (stacks f/b, turbines L/R) + `rocket` plume. Props: `appendages`, `*_emissive`. |
| **Super Logging Robot** | `super_logging_robot` | steam / range | 4 / 5 / 50 / 3 | Stronger re-skin (`variant=super`, `model=indian_post/logging_robot`). | *(shares logging_robot)* | *(shares logging_robot)* |
| **Super Locust Robot** | `super_locust_robot` | steam / ground | 5 / 1 / 70 / 3 | Stronger re-skin (`variant=super`, `model=indian_post/locust_robot`). | *(shares locust_robot)* | *(shares locust_robot)* |
| **Underdog Soldier** | `underdog_soldier` | human / ground | 3 / – / 45 / 4 | *"Attacks the player in the lead"* (`UnderdogSoldier extends DogSoldier`). Scenario/card-spawned, unaffiliated. **Reuses dogsoldier model.** | standing, walking, proximity, shooting, reacting, dying | `TransientParticleEmission` `Dust_Spawn` (as dogsoldier). |

### Big shots

| Big Shot | Repo id | Make/Mode | Stats | Special power (visual!) | Clips | Emissions |
|----------|---------|-----------|-------|-------------------------|-------|-----------|
| **Revolutionary** | `revolutionary` | human / ground | move 3 / fire 2 / dmg 35 (range 30) | *"When knocked out, allies immediately **respawn** to avenge him; +10% atk to allied Ground units; +50% atk with sword when adjacent (`PROXIMITY` shot)."* (`Revolutionary` → `ResurrectEffect`/`HeroInfluence`). | standing, walking, shooting, **proximity** (sword), reacting, dying | `GunshotEmission`, dud, misfire, **`TransientParticleEmission`** `death` on dying (respawn-flash). Props: `weapons`, `portrait`. |
| **Storm Caller** | `stormcaller` | human / **range** | move 3 / fire 2–4 / dmg 40 | *"Calls down **lightning** that chains: 80% to units beside the main target, 50% to units beside those — hits friend and foe."* (`StormCaller` → `ChainingShotEffect` / `LightningEffect`). | standing, walking, shooting, reacting, dying | **`TransientParticleEmission`** ×2 — `emitter` + **`charge_up`** on shooting (lightning charge + bolt). Props: `sc_hair_trans`, `sc_tool_text`. |
| **Trickster Raven** | `tricksterraven` (model `trickster`) | human / ground (can fly) | **move 5** / fire 1 / dmg 50 | *"Flies over impassable terrain by **turning into a Raven**"* (`TricksterRaven.isFlyer`). +atk/def vs air. | standing, walking, shooting, reacting, dying, **`flying_start` / `flying_cycle` / `flying_end`** (the raven transform + flight) | `GunshotEmission` (pepperbox), dud, misfire, **`TransientParticleEmission`** `transform` on flying_start (the morph puff). Props: `raven`, `cape`, `feet`, `portrait`. |

**Conversion notes — indian_post**
- **Trickster Raven** is unique: it has a whole second locomotion sequence (`flying`)
  *and* a transform particle on `flying_start`. Watch the **human→raven morph**.
- **Storm Caller** = the lightning showcase: two transient emitters (`charge_up` then
  bolt). Pair with `LightningEffect`/`ChainingShotEffect` and the `storm_caller` particle
  asset.
- **Buffalo Rider**'s `shooting_run` (charge) + `dust_trail` `ParticleEmission` is a
  motion-coupled dust effect — distinct from the one-shot dusts.
- **Robots** rely heavily on always-on `SmokePlumeEmission` plumes and **emissive
  texture maps** (`*_emissive.png`) — verify glow maps survive the bake. `unfolding`/
  `proximity` are robot-specific clips. Super variants must pick up the `super_*` skin.
- **Dream Catcher** uses an `alpha.png` for partial transparency (spirit/ethereal look) —
  verify alpha-blended material in jME3.

---

## Boom Town (`boom_town`)

| Unit | Repo id | Make / Mode | Move / Dmg / Init | Special power | Clips / Model | Emissions |
|------|---------|-------------|-------------------|---------------|---------------|-----------|
| **Forklift** | `forklift` | steam / ground | 3 / 35 / 2 | `Forklift.generateShotEffect` → **`SpringShotEffect`** (punts/springs the target). i18n still `TODO`. **Reuses `steamgunman` model.** | standing, walking, shooting, reacting *(steamgunman set)* | steamgunman emissions: gunshot ×2, `SmokePlumeEmission`, dud ×2, misfire |
| **One-armed Bandit** | `onearmedbandit` (class *"One Armed Jack"*) | steam / ground | 3 / 35 / 2 | `OneArmedBandit` rolls a random `RandomInfluenceEffect.Kind` each turn (incl. an `EXPLODE` → `AreaDamageEffect`). Slot-machine gimmick. i18n `TODO`. **Reuses `steamgunman` model.** | standing, walking, shooting, reacting *(steamgunman set)* | steamgunman emissions (as above) |
| **Windup Gunslinger** | `windupslinger` | human / ground | 3 / fire 1 / 35 / 4 | `WindupSlinger` — clockwork slinger, sight 9. **`scrip_cost=-1`** special. | **NONE — only `icon.png` present** (no model/mxml/properties) | n/a |

**Conversion notes — boom_town**
- **Forklift & One-armed Bandit ship the steamgunman art** in their folders (their
  `*.png` are `steamgunman.png`, `pistol.png`); their `model.properties name = steamgunman`.
  So in conversion they render as steamgunman re-skins — confirm whether boom_town intends
  distinct art (likely a content gap — these were late/unfinished, note the `TODO` i18n).
- **Windup Gunslinger has no renderable model in-repo** — flag for the user.

---

## Wreckage props (`units/wreckage/`)

Not units — destroyed-steam-unit debris pieces referenced by death/crash effects:
`bolt_big`, `fork`, `gear_big`, `nut_big`, `propeller`, `rod`, `tube`, `wheel_big`,
`wheel_small` (+ `gear.png`, `nut_and_bolt.png`). Relevant to the **explosion/death**
effect (fidelity §5 `explosionAndDeath`): the dark scattered debris on a steam-unit kill.

## Held props (carrying gold)

Not a per-unit skin: per `UnitSprite`, when `unit.holding != null` a `bonuses/<nugget>`
**model is attached as a child node** that rides the normal `walking` cycle. This is the
**fidelity §6 `GreenCharactersCarryingGold`** case — verify the nugget attaches at the
right offset and bobs with the walk. See [effects-and-cards.md](effects-and-cards.md).
