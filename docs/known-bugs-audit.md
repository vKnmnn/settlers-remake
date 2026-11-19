# Original-game bug audit

Scope: the eight entries on the [English Settlers 3 Wiki list](https://wiki.siedler3.net/doku.php?id=en:siedler3:bekannte_bugs), checked against this fork on 2026-11-19. An original-game report is a test hypothesis, not evidence of the same defect in Java. Original bugs are not deliberately reproduced; no compatibility switch is introduced.

| Original report | Finding in this fork | Evidence and limits |
| --- | --- | --- |
| Coal transport stalls at scale | No coal-specific aggregate transport limit in `MaterialsManager`. Each material has a request queue; stack requests track their own deliveries. | `CoalDeliveryCapacityTest` holds 320 deliveries to 40 consumers in flight and checks renewed demand after completion. This is scheduling coverage, not a large-map traffic or performance benchmark. |
| Crash while building/selecting a mine | No matching crash reproduced by the worker simulations. | `IndoorWorkerCyclesTest` runs all five mine types through eleven outputs and food pickup. Mines use prepared clear footprints and deposits; normal mountain placement and interactive selection are not covered by this test. The wiki supplies no precise crash trigger. |
| Amazon bee sound | Not established as fixed or reproduced. | `BackgroundSound` uses a separate ambient sound loop, sample 117, volume 0.05. It can request one sound per hive on a sampled row; overlap still needs a listening test with many hives. No audio changes made based on an ambiguous symptom alone. |
| Egyptian bow bug | Directional shooting covered by `EgyptianBowmanTest`. | Level-three Egyptian archers must actually damage a stationary enemy in each of the six directions. Java projectile release is independent of animation metadata. Sprite appearance is outside this test. |
| Asian shield targets enemies | The team filter already selects allies. | `MageTeleportTest.divineShieldProtectsOwnAndAlliedSoldiersButNeverEnemies` issues a real spell with own, allied and enemy soldiers in range. |
| Goods disappear when priorities change | A separate, reproducible display bug found and corrected: 34 goods shared only 30 initialized slots. | `MaterialPriorityPanel` now derives its row count from the number of goods. `MaterialPriorityPanelTest` checks distinct visible slots and repeated permutations without missing or duplicated goods. This does not claim the original game's rare trigger is identical. |
| Workers stuck around construction sites | Covered by construction-worker scenarios; not a proof for all congested maps. | `ConstructionWorkerCyclesTest` exercises actual jobs and completion. Deterministic fixtures cannot rule out every traffic deadlock. |
| Workers circle between stones | Covered by a stone-obstacle scenario in `ConstructionWorkerCyclesTest`. | The fixture tests progress through an available route, not arbitrary pathological map layouts. |

The [coal article](https://wiki.siedler3.net/doku.php?id=en:siedler3:kohlebug) describes a relationship with consumers and route length. The [original patch author's discussion](https://siedler3.net/fix-fuer-bugs-t7437-3.html) also distinguishes the bee-sound patch from game logic. These reports do not justify importing transport caps or muting all bees into this implementation.

The German wiki page contains additional reports beyond the linked English page. As a related check, `DonkeyHoneyTransportTest` exercises honey pickup, travel and delivery for Roman, Egyptian and Asian donkeys using bounded test trade stores. The remaining extra German entries are not claimed audited here.

## Figure coverage

`AllMovableMovementTest` explicitly classifies all 51 current movable types. Its 81 cases execute escape steps on neutral and hostile ground for every civilian, destination orders for controllable figures, trade movement for cargo units, water routes for ships and occupancy of every siege footprint cell. Adding an unclassified enum value fails the coverage assertion.

The wider worker suite adds actual work rather than only construction of Java objects:

- `ForesterMovementTest`: three planting/return cycles and the original neutral-ground death regression.
- `OutdoorWorkerMovementTest`: eight other outdoor professions, two production/return cycles each.
- `IndoorWorkerCyclesTest`: 21 workplace variants, input consumption, output and return; miners exceed their initial ten food credits.
- `WorkerServicesTest`: injured soldier treatment and both ship orders with real material consumption.
- `ConstructionWorkerCyclesTest`: diggers, builders and carriers working on actual construction.
- Existing `SiegeProductionIT`: ammunition and ordered weapons for all four civilizations.

Prepared terrain, supplied inputs, mature plants and in some service tests explicit job assignment keep these tests short and deterministic. They do not replace full economy matches, all civilization/terrain combinations, plant growth tests, rendering checks or multiplayer validation. Specialist movement coverage is not complete coverage of every spell, scouting, theft or combat rule.

Validation: JDK 21, `:jsettlers.logic:unitTest` (316 cases, three existing inapplicable cases skipped) and `:jsettlers.graphics:test` (30 cases), no failures or errors. Tests run against compiled classes, not the JAR used by an interactive game. Do not overwrite a running game's JAR when packaging: build to a separate destination and extract the release into a separate directory before launching it.
