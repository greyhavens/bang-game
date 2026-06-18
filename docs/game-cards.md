# Bang! Howdy — Card Reference

## What cards are

Cards are collectible, single-use power-ups in Bang! Howdy. They are not the
units you field on the board — they are consumable abilities you spend during a
round to swing the fight: heal a unit, buff its attack or movement, drop a trap,
call down a stampede, damage an area, and so on.

How you get them:

- **Awarded** at the end of rounds (the more common a card, the more likely it
  is to drop — see "rarity weight" below).
- **Purchased** from the General Store. Cards are sold as a *pack of three* of a
  given type; the price below is the per-pack scrip cost. Cards are **scrip-only** —
  there is no coin price for any card (the store always lists them at 0 coins).
  Some cards (scrip cost 0 in the tables) are **not sold at all** and can only be
  earned/awarded.
- **Picked up on the board** during a game (some bonuses can be lassoed into a
  playable card; cards left lying around can be grabbed).

How you use them:

- You hold a **hand of up to 3 cards** during a round.
- You **play one by targeting** something. Each card has a target/placement mode:
  - `VS_PIECE` — aim the crosshair at one unit/piece.
  - `VS_AREA` — aim at a board location (affects that tile / a radius around it,
    or drops a piece there).
  - `VS_PLAYER` — click an opposing player.
  - `VS_BOARD` — affects the whole board / all units; no aiming needed.
  - `VS_CARD` — click one of your cards (no current card uses this).

**Rarity weight.** Every card has a `weight`. Higher weight = more common = more
likely to be awarded (and these tend to be the cheaper, more basic cards). Low
weight = rare/powerful. Weight is a relative draw chance, not a percentage. A
weight of `0` means the card is currently **disabled** (cannot be drawn/awarded).

**Art.** Each card's icon lives at `assets/rsrc/cards/<town>/<type>/icon.png`
(with a larger `card.png` inventory image alongside it).

## Cards vs. Bonuses (board pickups)

It is easy to confuse **cards** with **bonuses**, because several of them share a
name (Repair, Hustle, Iron Plate, Spring, Trap, …). They are different systems:

- A **card** lives in your **hand** (max 3) and is **played by you** when you
  choose, by targeting something (see above).
- A **bonus** is an item that **spawns on the board** during a game. A unit
  moving onto its tile **picks it up**, and *that pickup* triggers the effect —
  you don't choose when, you just step on it.

**Is the user's "a Card goes to your hand, everything else is instant" model
correct?** *Mostly, with two clear exceptions.* When a unit picks up a bonus,
one of **three** things happens:

1. **Instant effect** (the common case): the effect fires immediately on pickup
   and the bonus is consumed — e.g. Repair heals the unit to full, Iron Plate
   grants damage immunity, Hollow Point boosts attack, Ramblin' boosts movement.
   This matches the user's "instant" intuition.
2. **Adds a card to your hand**: the **"Pick a Card"** bonus (internal `card`,
   and the unused `joker`) does *not* apply a board effect — it grants a **random
   card** to the picking player's hand, subject to the **3-card maximum** (if the
   hand is already full, the pickup is wasted; see `GrantCardEffect`). This is the
   one bonus that behaves like the user's "goes into your hand" idea.
3. **Carried item** (held until deposited or knocked loose): bonuses flagged
   `holdable = true` are *not* consumed on pickup — the unit **carries** them and
   gains an ongoing effect or must deliver them somewhere. These are the **gold
   nugget** / **fool's nugget** (carried to your claim to score), the Indian-Post
   **fetishes** (passive buff while held), the **protective talisman** (wendigo
   protection while held), and the **totem pieces** (carried to a totem base to
   build it). A carried bonus can be **dropped** when the unit is killed or
   bumped (see `HoldEffect.dropBonus`).

So: **card bonus → hand; holdable bonuses → carried; everything else → instant.**

**Shared names, different behavior.** Many bonuses reuse the name and effect of a
same-named card (Repair, Hustle, Iron Plate, Hollow Point/Power-Up, Ramblin',
Lucky Horseshoe/Lady Luck, Reinforcements/Duplicate, Firestarter, Forgiven,
Spirit Walk, Spring, Snare, Trap, Fool's Gold). The **bonus** version applies the
effect **instantly on pickup**; the **card** version is the same effect **played
from your hand**. The `card = <type>` line in a bonus's `bonus.properties` names
the matching card. (Note the bonus internal name and the card internal name don't
always agree — e.g. bonus `power_up` ↔ card `hollow_point`, bonus `lady_luck` ↔
card `lucky_horseshoe`, bonus `duplicate` ↔ card `reinforcements`.)

See the **Board Bonuses** section at the end of this document for the full
per-town list.

---

## Frontier Town

| Display Name | internal type | What it does | Target | Scrip | Coin | Rarity (weight) |
|---|---|---|---|---|---|---|
| Half Repair | `half_repair` | Repairs a unit by half its maximum health. | VS_PIECE | 15 | 0 | 75 |
| Barricade | `barricade` | Drops a barricade on the board that blocks units for 6 ticks. | VS_AREA | 30 | 0 | 60 |
| Half Giddy Up | `half_giddy_up` | Lets the affected unit move again two ticks sooner. | VS_PIECE | 30 | 0 | 60 |
| Hollow Point | `hollow_point` | +30% attack strength for the affected unit until it dies (replaces any other influence). | VS_PIECE | 25 | 0 | 60 |
| Lasso | `lasso` | Turns a bonus or trap on the board into a card you can play. | VS_AREA | 20 | 0 | 60 |
| Staredown | `staredown` | Resets the affected unit's movement counter (it can act immediately). | VS_PIECE | 15 | 0 | 60 |
| Dud | `dud` | Makes the affected gun unit's next shot have no effect. | VS_PIECE | 20 | 0 | 50 |
| Hustle | `hustle` | Affected unit moves every 3 ticks instead of 4 until it dies (replaces any other influence). | VS_PIECE | 40 | 0 | 50 |
| Lucky Horseshoe | `lucky_horseshoe` | Affected unit gets a 50% chance to deal 2x damage when attacking, until it dies (replaces any other influence). | VS_PIECE | 40 | 0 | 50 |
| Repair | `repair` | Repairs a unit to full health. | VS_PIECE | 30 | 0 | 50 |
| Stick Up! | `drop_nugget` | Makes the affected unit drop any carried item. | VS_PIECE | 20 | 0 | 50 |
| Misfire | `misfire` | The affected gun unit's next shot causes self-inflicted damage. | VS_PIECE | 30 | 0 | 40 |
| Engineer | `engineer` | Selects a destination for the train along the tracks. | VS_AREA | 20 | 0 | 60 |
| Fool's Gold | `fools_gold` | Drops a fake nugget that looks like gold but pops out and vanishes when collected. | VS_AREA | 30 | 0 | 35 |
| Joker | `joker` | Drops a card on the board that explodes when a unit tries to grab it. | VS_AREA | 30 | 0 | 35 |
| Giddy Up | `giddy_up` | Lets the affected unit move again immediately. | VS_PIECE | 40 | 0 | 30 |
| Iron Plate | `iron_plate` | Makes the affected unit immune to enemy attacks for 7 ticks (replaces any other influence). | VS_PIECE | 0 | 0 | 30 |
| Missile | `missile` | Damages all units within a 1-tile radius (power 60). | VS_AREA | 40 | 0 | 30 |
| High Noon | `high_noon` | Temporarily turns up the heat and limits the movement of all units. | VS_BOARD | 0 | 0 | 25 |
| Ramblin' | `ramblin` | Gives the affected unit +1 movement until it dies (replaces any other influence). | VS_PIECE | 0 | 0 | 25 |
| Spring | `spring` | Drops a hidden trap that bounces a unit to a random location and damages it when stepped on. | VS_AREA | 0 | 0 | 25 |
| Reinforcements | `reinforcements` | Calls in an extra Gunslinger with the same health as the selected unit (does not respawn when killed). | VS_PIECE | 0 | 0 | 10 |

## Indian Trading Post

| Display Name | internal type | What it does | Target | Scrip | Coin | Rarity (weight) |
|---|---|---|---|---|---|---|
| Bear Trap | `trap` | Drops a trap that snaps shut on and damages any unit that steps on it. | VS_AREA | 20 | 0 | 60 |
| Snare | `snare` | Drops a snare that stops a unit from moving when stepped on. | VS_AREA | 20 | 0 | 60 |
| Discard | `cat_ballou` | Removes a card at random from one of your opponents' hands. | VS_PLAYER | 20 | 0 | 50 |
| Jack Rabbit | `jack_rabbit` | +3 movement for the affected unit for its next four ticks (replaces any other influence). | VS_PIECE | 30 | 0 | 50 |
| Peace Pipe | `peace_pipe` | Stops the affected unit from attacking until it is attacked or 12 ticks pass. | VS_PIECE | 20 | 0 | 50 |
| Lightning | `lightning` | Damages a unit with lightning that chains through adjacent steam units. | VS_PIECE | 30 | 0 | 45 |
| Snake Bite | `snake_bite` | Affected human unit takes damage every tick for eight ticks. | VS_PIECE | 30 | 0 | 40 |
| Tumbleweed Wind | `tumbleweed_wind` | Clears all wreckage from the board. | VS_BOARD | 20 | 0 | 40 |
| Eagle Eye | `eagle_eye` | Gives gun users (max shot distance > 1) a shot-distance bonus. | VS_PIECE | 0 | 0 | 30 |
| Firestarter | `firestarter` | Affected unit sets targets on fire; burning units take damage every tick for four ticks. | VS_PIECE | 30 | 0 | 30 |
| Rain | `rain` | Clears all active influences from all units in the game. | VS_BOARD | 40 | 0 | 30 |
| Spirit Walk | `spirit_walk` | Affected unit can walk over any terrain and through any object (replaces any other influence). | VS_PIECE | 40 | 0 | 30 |
| Stampede | `stampede` | Sends a buffalo stampede across the board, damaging all units in its path (radius 1). | VS_AREA | 40 | 0 | 30 |
| Underdog Soldier | `underdog_soldier` | Calls in an unaffiliated Dog Soldier that roams and attacks the player in the lead. | VS_AREA | 0 | 0 | 15 |
| Forgiven | `forgiven` | Heals all living units in the game. | VS_BOARD | 0 | 0 | 10 |
| Rockslide | `rockslide` | Causes rocks to slide downhill, damaging units in their path. **(disabled — weight 0)** | VS_AREA | 40 | 0 | 0 |

## Boom Town

| Display Name | internal type | What it does | Target | Scrip | Coin | Rarity (weight) |
|---|---|---|---|---|---|---|
| Blown Gasket | `blown_gasket` | Renders the affected steam unit immobile after six steps. | VS_PIECE | 40 | 0 | 40 |
| Buggy Logic | `buggy_logic` | Lets you give an order to an opponent's steam unit. | VS_PIECE | 20 | 0 | 40 |
| Monkey Wrench | `monkey_wrench` | Affected steam unit takes damage at every step. | VS_PIECE | 40 | 0 | 40 |
| Dust Devil | `dust_devil` | Resurrects steam-unit wreckage into an operational unit (can hijack an opponent's wreck). | VS_PIECE | 0 | 0 | 10 |

## Ghost Town

No cards. The `cards.properties` file has a `# Ghost Town cards` section marked
`# TODO`, and no card class declares `BangCodes.GHOST_TOWN`.

## City of Gold

No cards. The `cards.properties` file has a `# City of Gold cards` section marked
`# TODO`, and no card class declares `BangCodes.CITY_OF_GOLD`.

---

## Notes, gaps, and the "bullet"/icon mapping

**Class ↔ properties coverage.** Every concrete card class has a matching
`m.<type>` / `m.<type>_tip` entry in `cards.properties`, and vice versa — there
are **no orphaned classes and no orphaned text entries**. (The three abstract
base classes — `Card`, `AreaCard`, `AddPieceCard` — are API scaffolding, not
cards, so they have no entries.)

**Notable items / things that look like anomalies:**

- **Rockslide** (`rockslide`, Indian Trading Post) is implemented and has text/art,
  but is **disabled**: its `getWeight()` returns `0` with the source comment
  `// DISABLED: 35;`. It cannot be drawn or awarded in its current state, though
  it still lists a 40-scrip store price.
- **"Stick Up!"** is the display name for the internal type `drop_nugget` — the
  name and the id don't match, which can be confusing when cross-referencing.
- **Coin cost is always 0.** Cards have no coin price anywhere in the code; the
  store sells card packs for scrip only.
- **Scrip 0 = not for sale.** Cards listed at 0 scrip (High Noon, Ramblin',
  Spring, Iron Plate, Reinforcements, Eagle Eye, Rain*, Forgiven, Underdog
  Soldier, Dust Devil) are filtered out of the store and can only be earned.
  (*Rain actually costs 40; it is for sale.)

**Matching what you saw on screen ("the Bullet", the Horseshoe, etc.):** there is
no card literally named "Bullet" — those are the **gun/bullet-themed** cards,
distinguished by their icon art:

- **Dud** — a bullet that fizzles (next shot does nothing).
- **Misfire** — a backfiring shot (next shot hits the shooter).
- **Hollow Point** — a high-damage round (+30% attack).
- These three are the gun/ammunition-iconography cards in Frontier Town.

The **horseshoe** card is **Lucky Horseshoe** (Frontier Town) — a luck buff
(50% chance of double damage). If you saw a horseshoe icon, that's this card.

---

## Board Bonuses

Bonuses are the items that randomly spawn on the board to be picked up by units.
Each is defined by `assets/rsrc/bonuses/<town>/<name>/bonus.properties`, which
names its **effect class** (`com.threerings.bang.game.data.effect.*`), the
matching **card** type if any, and its **spawn config**. Display names and
descriptions below come from the `m.help_bonus_*` keys in
`assets/rsrc/i18n/game.properties` (or are derived from the effect class).

### How spawn weight / rarity works

Spawning is **weighted random** (`Bonus.selectBonus` → `BonusConfig.getWeight`).
For each candidate bonus a weight is computed:

```
weight = base_weight + average(active affinity contributions)
```

The **affinities** (all in `bonus.properties`) nudge the weight based on the
current game state of the player(s) able to reach the spawn spot:
`damage_affinity` (their total unit damage), `low_power_affinity` (they're
behind on power), `many_pieces_affinity` / `few_pieces_affinity` (unit count),
and `early_game_affinity` / `late_game_affinity` (scaled by game tick, capped at
tick 60). A bonus is then drawn in proportion to its computed weight.

Gating that can zero-out a bonus entirely (`getWeight` returns 0):

- **`base_weight = 0`** → never randomly spawned. Used for bonuses placed
  *only by scenarios/abilities* (nuggets, totems, fetishes, talisman, traps,
  springs, heal-hero, joker, fool's nugget). `Bonus.isScenarioBonus()` is true
  whenever `base_weight <= 0`, so these don't count toward "bonuses collected"
  stats. There is also a per-game floor `minCardBonusWeight`: any bonus whose
  `base_weight` is below it is skipped for that game.
- **`min_point_diff`** → only spawns once the reaching player is at least that
  many points behind (a catch-up mechanic). Only `bonus_point` uses it (`50`).
- **`scenario`** → only spawns in the named scenario. *No shipped bonus sets
  this*, but the mechanism exists.
- **`players_only = true`** → only player-aligned units may activate it
  (`card`, `bonus_point`, `duplicate`).
- **`ground_only = true`** → airborne units pass over without triggering
  (`spring`, `snare`, `trap`).
- **`hidden = true`** → not shown on the board / no pickup animation
  (`spring`, `snare`, `trap` — the buried surprises).

**On-pickup behavior is determined by the effect class:** `holdable = true`
bonuses use a `HoldEffect` subclass and are **carried**; `GrantCardEffect`
**adds a card to the hand**; everything else applies an **instant effect**.

### Frontier Town (`towns = frontier_town+`)

| Display Name | internal name | Town | On pickup | Effect | Spawn weight | Conditions |
|---|---|---|---|---|---|---|
| Pick a Card | `card` | frontier_town+ | **Adds card to hand** | Grants a random card (`GrantCardEffect`); wasted if hand already at 3 (`MAX_CARDS`). *(also a card-granting bonus)* | 100 (+ low-power / few-pieces / late-game affinity) | players only |
| Bonus Points | `bonus_point` | frontier_town+ | Instant | Awards bonus points to the picking player (`BonusPointEffect`). | 25 | players only; only when ≥ 50 points behind (`min_point_diff`) |
| Reinforcements | `duplicate` | frontier_town+ | Instant | Duplicates the picking unit; the clone is weaker (`DuplicateEffect`). *(also a card — see Cards table, `reinforcements`)* | 15 | players only |
| Hustle | `hustle` | frontier_town+ | Instant | Unit may move every 3 ticks instead of every 4 (`HustleEffect`). *(also a card)* | 75 | — |
| Iron Plate | `iron_plate` | frontier_town+ | Instant | Unit becomes impervious to damage for a time (`IronPlateEffect`). *(also a card)* | 25 | — |
| Lucky Horseshoe | `lady_luck` | frontier_town+ | Instant | 50% chance of 2× damage when attacking (`LadyLuckEffect`). *(also a card — `lucky_horseshoe`)* | 60 | — |
| Hollow Point Bullets | `power_up` | frontier_town+ | Instant | +30% attack strength; expires after a while (`PowerUpEffect`). *(also a card — `hollow_point`)* | 75 | — |
| Ramblin' | `ramblin` | frontier_town+ | Instant | Increases the unit's movement distance (`RamblinEffect`). *(also a card)* | 75 | — |
| Repair | `repair` | frontier_town+ | Instant | Restores the picking unit to full health (`RepairEffect`). *(also a card)* | 100 (+ high-damage / low-power affinity) | — |
| A Gold Nugget! | `nugget` | frontier_town+ | **Carried item** | Unit carries the nugget; deliver it to your claim to score (`NuggetEffect` → `HoldEffect`). Dropped if the carrier dies. | 0 | scenario/ability placement only |
| Fool's Gold | `fools_nugget` | frontier_town+ | **Carried item** | Looks like a nugget but is worthless when delivered (`FoolsNuggetEffect`, `class = Nugget`). | 0 | scenario/ability placement only |
| ??? (Spring) | `spring` | frontier_town+ | Instant (trap) | Buried trap: bounces the unit back to its previous position (`ReboundEffect`). *(also a card — `spring`)* | 0 | ground only; hidden; scenario/ability placement |
| Pick a Card | `joker` | frontier_town+ | **Adds card to hand** | Grants a random card (`TrapEffect`-routed in props but help text = "awards a card"); unused/no-spawn. *(also a card — `joker`)* | 0 | not randomly spawned |

### Indian Trading Post (`towns = indian_post+` / `indian_town+`)

| Display Name | internal name | Town | On pickup | Effect | Spawn weight | Conditions |
|---|---|---|---|---|---|---|
| Firestarter | `firestarter` | indian_post+ | Instant | Attacks set struck pieces on fire (damage each tick) (`FirestarterEffect`). *(also a card)* | 75 | — |
| Forgiven | `forgiven` | indian_post+ | Instant | Heals **all** living units on the board (`RepairAllEffect`). *(also a card)* | 40 | — |
| Spirit Walk | `spirit_walk` | indian_post+ | Instant | Unit becomes noncorporeal — can walk through/over anything (`NoncorporealEffect`). *(also a card)* | 75 | — |
| Healing Herbs | `heal_hero` | indian_post+ | Instant | Gives a small amount of health to your Big Shot / hero (`HealHeroEffect`). | 0 | scenario/ability placement only |
| ??? (Snare) | `snare` | indian_post+ | Instant (trap) | Buried trap: stops the unit from moving for some ticks (`SnareEffect`). *(also a card)* | 0 | ground only; hidden; placement only |
| ??? (Trap) | `trap` | indian_post+ | Instant (trap) | Buried trap (`TrapEffect`). *(also a card)* | 0 | ground only; hidden; placement only |
| Frog Fetish | `fetish_frog` | indian_town+ | **Carried item** | Held: doubles the unit's growing power (`FetishEffect`). | 0 | placement only |
| Turtle Fetish | `fetish_turtle` | indian_town+ | **Carried item** | Held: halves attacks against the unit (`FetishEffect`). | 0 | placement only |
| Bear Fetish | `fetish_bear` | indian_town+ | **Carried item** | Held: doubles the unit's attack power (`FetishEffect`). | 0 | placement only |
| Fox Fetish | `fetish_fox` | indian_town+ | **Carried item** | Held: makes the unit invisible to logging robots (`FetishEffect`). | 0 | placement only |
| Protective Talisman | `talisman` | indian_town+ | **Carried item** | Held: protects the unit from the wendigos (`TalismanEffect` → `HoldEffect`). | 0 | placement only |
| Small Totem Piece | `totem_small` | indian_town+ | **Carried item** | Carried to a totem base to build the totem (`TotemEffect`, `class = TotemBonus`). | 0 | placement only |
| Medium Totem Piece | `totem_medium` | indian_town+ | **Carried item** | Carried totem piece (`TotemEffect`). | 0 | placement only |
| Large Totem Piece | `totem_large` | indian_town+ | **Carried item** | Carried totem piece (`TotemEffect`). | 0 | placement only |
| Totem Crown | `totem_crown` | indian_town+ | **Carried item** | Carried totem-crown piece (`TotemEffect`). | 0 | placement only |

*(`indian_post+` and `indian_town+` both resolve to the Indian Trading Post town
and onward; the two spellings appear interchangeably in the bonus files.)*

### Boom Town (`towns = boom_town+`)

| Display Name | internal name | Town | On pickup | Effect | Spawn weight | Conditions |
|---|---|---|---|---|---|---|
| St. Elmo's Fire | `saint_elmo` | boom_town+ | Instant | Replaces all **dead** units with 60%-health windup gunslingers owned by the picking player (`SaintElmosEffect`). | 10 (+ damage / few-pieces / late-game affinity) | — |

### Ghost Town

| Display Name | internal name | Town | On pickup | Effect | Spawn weight | Conditions |
|---|---|---|---|---|---|---|
| Plague | `plague` | boom_town+ | Instant | Replaces a player's units in excess of the per-player average with 40%-health windup gunslingers (`PlagueEffect`). | 10 (+ low-power / few-pieces / late-game affinity) | — |

*(The `plague` bonus lives under `assets/rsrc/bonuses/ghost_town/` but its
`bonus.properties` declares `towns = boom_town+`, so it is available from Boom
Town onward — i.e. in Boom Town **and** Ghost Town.)*

### Anomalies / notes

- **`base_weight = 0` for many bonuses is intentional**, not a disable bug like
  Rockslide. A zero base weight just means the bonus is never *randomly* placed —
  it is spawned only by a scenario or unit ability (all the carried items, the
  buried traps, heal-hero, joker, fool's gold).
- **`joker`** has `effect = TrapEffect` in its properties, but its help text and
  intended behavior are "awards a card" (a duplicate of the `card` bonus). It has
  `base_weight = 0` and is not used for random spawns.
- **`heal_hero` and the totems/fetishes/talisman** never appear in standard
  random play; they belong to the Indian-Post hero/totem scenarios.
- **Display names for hidden bonuses** (`spring`, `snare`, `trap`) are
  intentionally `???` ("What's that thing in the ground?") so the player can't
  tell what they stepped on until it triggers.
