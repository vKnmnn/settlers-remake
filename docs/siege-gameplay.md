# Siege gameplay and original-game evidence

This implements the four siege workshops and units, ammunition production,
Asian gunpowder production, orders, movement, reloading and combat. The starting
implementation was adapted from BotGGias' siege work; the original executable
was then used to replace its unverified infantry splash and tower-door mechanics.

## Original evidence

Analysis uses the installed original `S3.EXE`, SHA-256
`c2977b67f42c12cb03200c3c282d7f38010e353232689137133c591ebefef2a0`.
Only findings and independently written Java are committed. Decompiled files
remain in `/tmp/settlers-original-analysis/`.

- `00500860`, shot path near `00500ff4`: searches enemy military buildings
  (original types 15, 16 and 18: small tower, large tower, castle), siege units
  (type 41), and ships (43/44). Amazon siege also accepts other buildings.
  Ordinary soldiers and settlers are not candidates.
- The same function uses a 16-bit random roll for accuracy and a second roll
  for successful projectile damage. Roman shots hit above 32000, Egyptian
  shots above 20000, Asian shots above 15000. Successful damage is respectively
  60–119, 80–159 and 100–199. The damage formula is
  `base + ((roll * base) >> 16)`. No soldier combat-strength multiplier occurs
  in this shot path.
- The Amazon roll hits above 20000. Against a building its successful damage
  is the building's remaining structural health plus one: a successful gong
  strike destroys the targeted building. It is not an infantry area attack.
- `004ff320` computes hex distance. The shot path rejects distances below 10
  for the first three civilisations and below 3 for the Amazons.
- `004fefd0` queues impacts. `004e7810` handles event 5 by subtracting structural
  building health and calling destruction routine `004d9250` on a lethal hit.
  Events 6 and 7 are vehicle/ship damage. There is no tower-door health floor
  or conversion of damage into selective garrison casualties in event 5.
- `004dbc50` initializes building structural health to 100, overridden for
  small tower, large tower and castle by `00659664`, `00659668`, `0065966c`:
  **250, 500, 1000**. These values were read from the installed executable's
  data, not inferred from its infantry entrance health.
- `0050dd20` creates a siege unit plus six linked type-42 helper units on its
  hex neighbours. Original siege therefore occupies seven cells.
- `00659dc0` contains shot-delay values 24, 15, 18, 12. Their conversion to the
  remake's clock has not yet been established, so these are not presented as
  implemented milliseconds.

The local original manual `MANUAL/MILITAER/MILITAER.HTM` also identifies military
buildings as catapult targets, describes the ballista as more accurate and the
cannon as more destructive, and specifies coal plus sulfur for gunpowder.

## Implemented combat

Target selection excludes infantry and settlers. Each impact affects a single
eligible enemy target, without splash damage. All three projectile weapons use
the recovered accuracy and damage distributions. Their minimum range follows
the recovered hex-distance thresholds. The gong targets a building and uses its
remaining structural health on a successful strike.

Building structural damage is stored independently of infantry door combat and
survives saving. Empty military buildings can still be bombarded. A lethal hit
uses the existing building destruction lifecycle; siege does not change the
owner or capture territory. Allied buildings and unfinished construction sites
are protected from these impacts. Older saves have zero accumulated structural
damage by default.

The original's strictly-less-than destruction check can leave a building at
exactly zero health for another hit. The remake destroys at zero health; this
edge case is not deliberately reproduced.

## Remaining adaptations and verification work

- Gong targeting is currently restricted to buildings. Although the original
  shared target search also accepts vehicles, its gong damage branch uses a
  building-health lookup even on that path; the intended vehicle effect needs
  further verification rather than inventing a damage value.
- The current unit occupies one cell, rather than the original seven-cell
  formation. Pathfinding, collision and transport therefore differ.
- Maximum ranges, cooldowns, animations, movement speeds, vehicle health,
  workshop construction/assembly timings, and unit material costs still
  include values from the fork. They need separate original-game validation.
- Impact visuals do not scatter missed shots to the original random offsets.
  A miss is represented by a projectile with zero damage.
- Impacts resolve the eligible object at the destination at arrival; the
  original queue stores target IDs. A moving vehicle can therefore evade the
  remake projectile, and replacement targets can differ from the original.
- Workshop reloading currently uses nearby accessible ammunition stacks;
  original ammunition logistics and exact worker animations need further
  comparison.
- Destruction uses the remake's existing rubble/material/garrison lifecycle.
  Those details have not been compared instruction by instruction with the
  original destruction routine.

## Regression coverage

`SiegeBuildingDamageTest` covers saved structural damage, destruction without
conquest, empty-tower target selection, infantry exclusion, allied protection,
and original hit/damage boundaries. `SiegeWeaponsIT` exercises workshop
production and reloading, actual shots at towers, structural destruction,
infantry immunity, and gong damage to civilian buildings. `SiegeSaveTest`
checks queued/partial construction and an in-flight projectile, including
preserved hit/miss outcome and cooldown.
