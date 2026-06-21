# Bang! Howdy — Game Reference

A navigable reference for the game's **units, big-shot skills, effects, and mechanics**,
written to support the jME3 sprite / animation / particle-effect conversion. Every unit
entry ties together the **wiki/lore** (role, special power), the **real repo data**
(`unit.properties`, `model.properties`, the per-unit `*.mxml` animation clips, held props),
and a one-line **"what to watch for in conversion"** note.

> **Scope & provenance.** Stats, animation clips, model paths, emission/particle wiring,
> card lists, team colours and scenario class names are read **directly from this repo**
> (`assets/rsrc/`, `client/shared/.../game/data/...`) — these are authoritative for what we
> render. Flavour text and power descriptions are quoted from the in-game i18n
> (`assets/rsrc/i18n/units.properties`, `cards.properties`) which is the same copy the
> wikis paraphrase. External wiki corroboration came from web search; the dedicated
> `banghowdy.wiki` and the Fandom mirrors **blocked/timed-out direct fetch**, so a few
> lore-only claims are marked *uncertain* — see [Sources](#sources).

## Contents

| File | Covers |
|------|--------|
| [units-and-big-shots.md](units-and-big-shots.md) | Every unit & big shot per town: stats, special power, model path, animation clips, render command, visual-effect emissions, conversion notes |
| [effects-and-cards.md](effects-and-cards.md) | The effect/emission types (gunshot, dud, misfire, smoke, particle, transient particle), the particle asset folders, and the full card/bonus/hindrance list with their visual effects |
| [mechanics.md](mechanics.md) | Towns, scenarios, the tick/initiative combat model, gold/nuggets/claims, gangs & **team colours** (incl. the "green gang"), unit make/mode interactions |

## Quick orientation

- **Towns** (`BangCodes.TOWN_IDS`): `frontier_town`, `indian_post`, `boom_town`, then
  the not-yet-shipped `ghost_town` / `city_of_gold`. Each town adds new units, big shots,
  bonuses and scenarios. The repo ships unit art for `frontier_town`, `indian_post`,
  `boom_town`, plus a `wreckage` prop set (destroyed-unit debris).
- **Unit ranks** (`unit.properties → rank`): `normal` (recruitable), `bigshot` (the one
  premium hero you bring per match), `special` (scenario-spawned, e.g. logging robots,
  underdog soldier), and badge-unlockable normals (`sharpshooter`, `steamgunman`,
  `dogsoldier`).
- **Make × Mode**: `make ∈ {human, steam, spirit}`, `mode ∈ {ground, air, range}`.
  Damage/defense tables in each `unit.properties` give per-make and per-mode modifiers —
  this is why e.g. the Gunslinger is bad vs steam and the Dirigible (air) dodges Artillery.
- **Visual-effect plumbing**: a unit's `model.properties` lists `controllers` — named
  `SpriteEmission` subclasses bound to animation frames. These are exactly the
  gunshot-flash / smoke-plume / dust / lightning particle effects the jME3 cutover
  re-bakes (`ParticleConverter`) and plays (`ParticleEffectViz`). See
  [effects-and-cards.md](effects-and-cards.md).

## Offscreen render harness (no live game needed)

```sh
./gradlew :tools:j3o-converter:renderModelToPng \
    -PmodelType=units/<town>/<unit> -Panim=<clip> -Ptime=<t> -Pout=/tmp/x.png
```

`-Panim=list` prints a model's clip names; negative `-Ptime` poses the clip midpoint.
`renderSceneToPng -Pspecs=type@anim:time,...` composes several models. (From
`docs/fidelity-reference/README.md`.)

## Related docs

- **[../fidelity-reference/README.md](../fidelity-reference/README.md)** — six fork-golden
  screenshots (attack/death anim, explosion+debris, claim/damage numbers, green units
  carrying gold) that are the **visual targets** for several units/effects here. Per-unit
  entries cross-link to the relevant screenshot.
- `../jme3-model-loader.md`, `../jme3-model-pipeline.md` — how `.mxml`/`model.properties`
  bake to `.j3o`.
- `../engine-notes.md` — model/particle pipeline internals.

## Sources

- In-repo (authoritative): `assets/rsrc/units/*/*/unit.properties` & `model.properties`,
  the `*.mxml` clips, `assets/rsrc/i18n/units.properties` & `cards.properties`,
  `assets/rsrc/bonuses/*`, `assets/rsrc/effects/*`,
  `client/shared/.../game/data/piece/*.java`, `.../game/data/effect/*.java`,
  `.../game/client/sprite/*Emission.java`, `.../game/data/scenario/*Info.java`,
  `.../bang/client/BangMetrics.java`, `.../bang/data/BangCodes.java`.
- Web (corroboration): [Gamezebo review](https://www.gamezebo.com/2009/12/31/bang-howdy-review/);
  `banghowdy.wiki` ([Unit](https://banghowdy.wiki/wiki/Unit), [Cattle Rustling](https://banghowdy.wiki/wiki/Cattle_Rustling), [Rough Housin'](https://banghowdy.wiki/wiki/Rough_Housin')) — *direct fetch timed out; titles/snippets from search only*;
  Fandom mirrors ([gamia-archive](https://gamia-archive.fandom.com/wiki/Bang!_Howdy), [gamicus](https://gamicus.fandom.com/wiki/Bang!_Howdy)) — *403/blocked to fetch*.
