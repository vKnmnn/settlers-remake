# Siege costs, range and timing

The values below were recovered from the installed S3.EXE with Ghidra. Addresses
refer to that executable. No executable code, decompiler output or original assets
are distributed with this repository. Civilisation order is Roman, Egyptian,
Asian, Amazon.

## Implemented values

| Weapon | Weapon inputs, including initial ammunition | Range (inclusive) | Recovery | HP | Standard-ground step |
|---|---|---|---|---|---|
| Catapult | 6 planks, 3 iron, 6 stone | 10–27 | 1,536 ms | 250 | 768 ms |
| Ballista | 5 planks, 6 iron, 6 trunks | 10–31 | 960 ms | 250 | 768 ms |
| Cannon | 3 planks, 8 iron, 6 gunpowder | 10–35 | 1,152 ms | 250 | 768 ms |
| Gong | 8 planks, 4 iron, 6 gold | 3–16 | 3,072 ms | 250 | 768 ms |

`004e05e0` initializes production recipes at `004e2b82`, `004e2d1c`,
`004e2eb6`, `004e3066`. Their three quantities are respectively `(6,3,6)`,
`(5,6,6)`, `(3,8,6)`, `(8,4,6)`. Worker states in `004e9670` consume
material IDs 1 (planks), 11 (iron), and respectively 2 (stone), 3 (trunk),
30 (gunpowder), 10 (gold). `0050dd20` initializes a new vehicle with six
ammunition units. Thus charging these inputs without the initial load would be
incorrect. The gong's field is also used for animation; it does not consume
ammunition in the remake.

A separate ammunition order uses `(0,0,6)`, `(0,3,6)` or `(0,3,6)` of those
same three materials. Completion states `0x137`, `0x138`, `0x139` add **six**
shots, not one. The remake now tracks partial inputs and partial output delivery
on the workshop, so replacing its worker or saving cannot erase a paid batch or
produce it twice. Old two-material weapon-progress arrays are expanded; the
old gong's gold progress is moved to its new third slot.

`00510470` reads unit-type 41's health from `006590e4`: 250. Its Amazon health
reduction applies only to soldier classes 1–9; siege belongs to class 40.

`00536f5d` reads attack-command stopping ranges from `00659db0`:
`28,32,36,17`. `00500860` requires distance **less than** that value, hence the
inclusive limits in the table. `004ff320` is the matching hex-grid distance.
The original scans for targets near the commanded point; the remake applies this
range directly to the selected target. Minimum distances 10/3 are checked in
`00500860` before launching the attack.

`00500860` initializes the post-shot state `0x151` with the counter from
`00659dc0` minus one: `24,15,18,12`. `004e9670` decrements the counter once
per simulation tick. The gong repeats its 12-tick period four times, while its
animation field decreases from 6 to 3. At 64 ms per original simulation tick this
gives the recovery durations above. The remake launches the projectile first,
then plays its firing animation while the cooldown elapses. It no longer adds
an invented 4–7 second cooldown after a preceding animation. The existing firing
behaviour-node layout is retained for suspended savegame coroutines.

This establishes the shot recovery, **not the complete original automatic
retargeting cadence**: original state `0x151` returns to idle `0x37`; the remake
keeps its own automatic acquisition and timer scheduling. Saved old pending
firing animations finish without an extra legacy-endpoint shot.

For movement, the siege branch of `00500860` adds the terrain byte from
`00658c1c` to the type adjustment from `006591a4`. Type 41's adjustment is 3;
standard terrain contributes 9, giving 12 ticks or 768 ms for all four weapons.
The enum's constructor has a general 0.75 speed multiplier, so its supplied
1.024 seconds yields an actual 768 ms step. Original terrain-specific speed
penalties remain unported; this change establishes the common standard-ground
speed, not every terrain's speed.

## Further original rules identified

`004dd6f0` allows a new ammunition batch while internal workshop ammunition is
less than 25. Six-shot batches therefore fill an internal **30-shot store**.
`00535990`, command case `0x1a` around `00538b97`, searches own workshops within
radius 15 and immediately transfers their stored ammunition until the vehicle
reaches six. This is different from the remake's visible six-item output stack,
short reload radius and one-second-per-shot automatic loading. Those aspects
must not be claimed as matching the original until separately implemented.

Workshop work time is animation-driven: `0050e2c0` resolves animation IDs from
`006a8060`, and `005fad20` reads the animation length from loaded sprite data.
The per-input animation is state `0x113`. The animation IDs are `0x000f0209`, `0x0019020b`, `0x00230219`,
`0x002d0212`. Direct inspection of the installed DAT animation tables (type
`0x21702`, table offset `0x94`) gives 80 entries in every sequence; each occupies
4 + 80 * 24 bytes. Consequently the remake now processes **each consumed input
for 5,120 ms**, then finalizes the vehicle or six-shot batch without an invented
extra assembly/end-processing delay. Its visible worker paths, pickup/drop and
entry/exit choreography still add remake-specific time.

Old siege-engineer saves discard obsolete behaviour-node IDs while retaining
workshop progress and carried material. Other movable coroutine states are
unchanged. Already consumed inputs of an unfinished legacy single-shot recipe
cannot be reconstructed; they do not grant unpaid batches.

## Verification

`OriginalSiegeValuesTest` checks range, recovery, health, movement and material
costs. `SiegeAmmoBatchTest` checks all three ammunition recipes, incomplete input
rejection, save/load of a partial recipe, bounded partial delivery and prevention
of duplicate output. The existing workshop-stack test checks that all required
materials have XML request stacks. Full worker production is exercised by the
four-civilisation production integration tests.

## Target identity and occupied space

Original impact events in `004e7810` use the target record ID queued by
`004fefd0` from `00500860`, rather than looking up whoever occupies the impact
coordinates. Remake projectiles now retain their original target across movement
and save/load. The visual flight destination stays fixed. Dead or no-longer-hostile
targets do not transfer a hit to another unit. Projectiles from older experimental
saves, which lack target identity, dissipate without damage.

`0050dd20` creates a central type-41 vehicle and six surrounding type-42 records.
The remake represents this as one unit reserving seven hex cells, keeping unit
counts and rendered/serialized unit arrays unchanged. Movement checks the whole
footprint before changing occupancy; pathfinding rejects narrow passages. Melee
soldiers can attack its edge, and ferries only unload where the full footprint
fits. Weapons wait or reroute rather than swapping places with other units.

Reservations are reconstructed after deserialization. Conflicting positions in
older experimental one-cell saves are relocated deterministically to a suitable
nearby position; if none exists on the map, loading reports an error rather than
overwriting units. New reinforcements blocked by a weapon wait for a free spawn
position. `SiegeFootprintTest` exercises movement, occupied edges, terrain,
boarding/unloading, melee reach, save migration and delayed spawns.
`SiegeSaveTest` additionally checks moving projectile targets and replacement
units, including a save during flight.
