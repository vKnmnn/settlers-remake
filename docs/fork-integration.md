# Fork integration

Baseline: paulwedeck/settlers-remake, `6ad8c4230`.
Integration branch: `integration/fork-improvements`.

## Integrated

- BotGGias `329fa153fe`: Gradle 8.14.3, updated plugins and CI, JDK 21 build
  with Java 11 bytecode. Desktop validation only; Android migration unverified.
- BotGGias `5ece051ea6`: production statistics and initial mission-goal support.
  Original goods goals now check current owned stacks rather than lifetime production.
  Resolved Player.java context without importing the unrelated peace-time field.
- TheRealDrNick `ea94716c6`: take stolen goods on arrival before the animation,
  preventing carriers from removing them during the animation delay.
- TheRealDrNick `464384b43`: write logs/replays under application configuration
  directory and continue with console logging if log-file creation fails.
- Selected portions of BotGGias `fbd1186681`: fog-of-war save lock, pause-state
  restoration, consistent material-priority snapshots. Replay fixtures omitted:
  their protocol/state depends on other changes not imported here.
- Selected portions of szusik `de811c2b56`: reject missing savegame timers and
  exclude shutdown from saving. The shared lock guards all clearState callers.
  Android UI changes are not included. The mage restoration fix is ported
  separately: terrain editors are created on demand after deserialization.
  A regression test saves a queued irrigation spell, reloads it and verifies
  that it changes the restored terrain (and leaves the original grid alone).
- Local GFX parser correction: accept populated header index blocks with
  validated type, size and file bounds. Synthetic fixtures cover both color
  formats and malformed headers without redistributing original assets.
- Local tests cover goods victory thresholds, fog update exclusion during
  saving, and incomplete savegame rejection. Desktop CI explicitly opts out of
  Android, uploads test reports, and runs a save/load round trip.

## Local verification (2026-11-18)

JDK 21.0.13.1, Linux. Desktop release and unit tests passed.
Common: 936 cases, 157 skipped; graphics: 9 passed; network: 74 cases,
19 skipped; logic: 78 passed (including thief, original mission/garrison import, mage save and marker-cleanup regressions). Save/load/save integration: 1 passed.
Existing skipped tests are not counted as successful coverage.

Resource initialization with the local original installation completed and
listed 210 maps (original plus bundled/additional maps). After the header-index
fix, a fresh configuration run no longer emits warnings for 07/08/09 or 99.
The remaining rejected file, Siedler3_18.f8007e01f.dat, has MZ and PE signatures
and is not a graphics archive; its purpose is unknown and it was not executed.
This does not establish that all sprites render correctly. No full interactive
match has been validated yet. The combined desktop release and save/load/save
integration test also passed after the mage and GFX fixes.

## Remaining candidates

- Run an interactive desktop smoke test with original assets, including
  rendered sprites and terrain spells after loading a savegame.
- Logging path regression tests and complete failure-path startup validation.
- Additional save failure/concurrent-shutdown tests, atomic file replacement,
  save-version compatibility policy and deterministic replay corpus.
- szusik multiplayer resume and selection groups;
  review protocol and save-format dependencies first.
- TheRealDrNick soldier-order fixes require synchronized command ownership first
  (see review below). Naval/colonization AI needs deterministic runs and balance tests.
- BotGGias spells, multiplayer synchronization, siege weapons, map decoration
  and UI additions: review separately, with feature-specific tests.
- Android and macOS validation remain separate work items.

Do not merge whole fork tips without reviewing overlapping implementations.
Preserve upstream attribution with cherry-pick -x for complete commits and
source commit IDs for partial ports.

## Proposed desktop beta acceptance criteria

These are release gates, not claims of current completeness:

1. **Playable installation:** a clean desktop package starts on each supported
   platform using documented original-data setup. Verify graphics, sound,
   controls and map loading interactively; resource enumeration alone is not
   sufficient. Decide the supported platforms before promising a release.
2. **Reliable single-player loop:** finish representative matches with each
   civilisation, including production, transport, combat, spells, victory and
   defeat. Add deterministic regression scenarios for observed failures and
   evaluate the selected AI changes against those scenarios.
3. **Reliable persistence:** save/load during construction, combat and spells;
   continue the restored match and compare deterministic state. Cover failed
   writes and interrupted saves, preserve the previous usable save, and define
   which historical save versions are supported.
4. **Multiplayer, if included in beta:** run repeatable two-client matches,
   detect desynchronization, and exercise disconnect/resume and saved matches.
   Protocol-changing fork features need shared replay fixtures before inclusion.
5. **Release evidence:** CI builds distributable artifacts and publishes test
   results. Record manual scenarios and known limitations for the exact release
   commit. Existing skipped tests and empty replay suites do not satisfy a gate.

Prioritize startup, crashes and save integrity first, then complete matches and
AI correctness. Siege weapons, extra spells and other new features can follow
once the corresponding gameplay and persistence checks exist.

## Original-game investigation (2026-11-18)

Compatibility target: intended gameplay, not intentional reproduction of known
original bugs. A selectable bug-compatibility mode (for example for speedruns)
is deferred; no `--buggy` flag is implemented or promised by this work.

Local static analysis succeeded with Ghidra 12.2 and JDK 25. The analyzed
`S3.EXE` is a 32-bit x86 PE without debug symbols, SHA-256
`c2977b67f42c12cb03200c3c282d7f38010e353232689137133c591ebefef2a0`.
Ghidra recognized 20,888 functions, including library code and possible
analysis artifacts. This is not a recovered original source tree. The separate
`s3xp.exe` has a different hash and was not analyzed in this pass.

The original executable was not run. Derived C and the Ghidra project remain
local under `/tmp/settlers-original-analysis/`; they are not repository assets.
`tools/research/InspectGame.java` is our analysis script. Reproduce against this
exact binary, adjusting paths and using a fresh project name for another import:

```sh
mkdir -p /tmp/settlers-original-analysis/projects
JAVA_HOME=/usr/lib/jvm/java-25-openjdk /opt/ghidra/support/analyzeHeadless \
  /tmp/settlers-original-analysis/projects settlers3 \
  -import "$HOME/.local/Siedler3/S3.EXE" \
  -analysisTimeoutPerFile 240 -max-cpu 2 \
  -scriptPath tools/research -postScript InspectGame.java \
  /tmp/settlers-original-analysis/output \
  006971b8 006a5908 006a5b04 006a6648
```

String cross-references led to these entry points (addresses are binary-specific):

| Function | Evidence and interpretation |
| --- | --- |
| `004e9670` | References `Digger trouble`; worker-state switch includes terrain work. States 5–9 reserve work positions, compare current and target heights, and change height by one subject to neighbor checks. A 31×31 work table must be empty before the building advances from state 1 to 2. |
| `004dbc50` | References `BuildingDone`; clears the construction-state byte and initializes building-specific state. |
| `00509980` | References save-section names including `diggertab`; serializes building and terrain-work data. |
| `0040c6e0` | References `BuildHelp`, but is only a small wrapper; it does not establish marker-rendering behavior. |

These are interpretations of decompiler output, not original symbol names.
The remake already has height-based worker actions in `DiggerMovable`, including
reservation, target-height comparisons and repeated work. Terrain leveling is
therefore **not absent**. Exact timing, terrain constraints and visual-marker
lifetimes still require narrower comparisons and gameplay observations.

### Confirmed gaps in the current remake

| Priority | Gap | Code evidence |
| --- | --- | --- |
| P1 | Siege workshops/units and powder production are absent from the current model. | `EOriginalMapBuildingType` maps catapult, ballista, cannon, powder and great gong entries to null; corresponding building types are absent. `EOriginalMapSettlersType` also lacks mappings for relevant units. |
| P1 | Unsupported mission building types remain unimplemented. | Active unsupported destruction/goods goals now reject import rather than silently dropping requirements. The local audit found no unsupported active goals among 34 mission-condition sections. |
| P2 | Multiplayer-save listing has a stub. | `MapList.getSavedMultiplayerMaps()` returns null. This alone does not prove that every alternative resume path is absent. |
| P2 | AI rice-economy planning is incomplete. | `PioneerAi` lacks a swamp-target strategy; `WhatToDoAi` leaves rice-farm work-area adjustment as TODO. Human rice farming exists. |

Comments about original beekeeping rules still need independent verification.
All four civilisations, the spell enum's cases, ferries and cargo ships already
exist; their correctness cannot be inferred merely from their presence.

### Reported interactive issues

- Construction previews at other free positions are expected while the building
  tool remains active after placement. Right-click (`MOVE_TO`) or abort clears
  the active tool through `ShowConstructionMarksAction(null)`. This does not
  explain persistent marks after abort or over already occupied positions.
  A separate cleanup fix tracks partially calculated areas and clears markers
  in a `finally` block on cancellation or calculation failure. Regression
  tests cover partial calculation, cancellation while waiting, and a failed
  calculation combined with deselection; this is not a full reproduction of
  every reported visual symptom.
- Clicking “leave game” sometimes does not respond. **Not reproduced or fixed
  yet.** Added diagnostics log EXIT before dispatch, the stop request, and each
  shutdown stage. `Q` uses the same EXIT action and can distinguish mouse/menu
  handling from downstream shutdown. No active game process remained for a
  thread dump during this investigation. A suspected music-stop race was
  rejected by a passing test; no speculative music or save-lock fix was applied.

After integrating exit diagnostics and marker cleanup, all 53 logic unit tests
and the desktop release build passed. The 9 graphics tests also passed after
the exit-dispatch logging change. The three marker regression tests pass on
the combined branch. Interactive confirmation of the reported symptoms is
still required.

### Original mission and garrison import corrections

- Rice liquor (ID 33) and mead (ID 34) now retain their stacks on import.
- WIN_COND goods records contain a two-byte little-endian target followed by a
  material byte. Previously the upper amount byte was mistaken for the material.
  Synthetic encrypted-resource tests cover this layout, unused slots, truncated
  data and unsupported active requirements. The installed-map audit found eight
  active goods missions among 34 condition sections in 178 map files.
- Local original executable analysis of `FUN_005145c0` shows goods quantities
  summed from current owner-associated goods records. The remake now counts
  actual stacks on player-zero territory, including reserved goods still present,
  rather than cumulative production or unreserved transport offers. Exact cargo
  and in-transit equivalence has not been established. Existing saves containing
  previously misparsed objectives cannot reconstruct the original target; start
  the original map again to obtain corrected requirements.
- `FUN_004fd540` distinguishes building-record byte 6: mode 0 creates one level-one
  swordsman and ignores packed counts, mode 1 uses nine soldier-type/level counts,
  and other values create no soldiers. Tests exercise map versions 10 and 11 and
  reject truncated/overflowing building records. The local audit found 260 default
  and 350 explicit military-building records. With mode decoding all fit the
  original class capacities (small tower 1 infantry/2 archers, large tower 3/3,
  castle 6/4).
- Imported garrisons now initialize inside their buildings, retaining types and
  levels; explicit empty garrisons stay empty. Native maps retain their default
  swordsman. Capacity is checked before soldiers are created, and regression
  tests cover initialization and save/load. The Roman castle now has 6 infantry
  and 4 archer slots instead of 5/6. Its added infantry sprite position is derived
  from the visible window; exact original rendering coordinates remain unverified.

Decompiler function addresses refer to the locally analyzed S3.EXE identified
above. Original binaries, graphics and decompiled source are not redistributed.

After the mission/garrison changes, all 74 logic unit tests, the save/load/save
integration test and `releaseJSettlers` passed. A repeated read-only audit of the
178 installed original maps found all 610 military-building garrisons within
the corrected class capacities. This data audit is not a full gameplay test.

### Soldier-order fork review

Do not directly cherry-pick `2269db784`, `f031f9c8a` or `f0d91c9ec`.
They gate pursuit changes on `Player.getPlayerType() == HUMAN`, but `playerType`
is transient and `MultiplayerGame.determinePlayerSettings` assigns all remote
slots HUMAN on clients while the host retains AI types. This distinction keeps
AI execution on the host. Using it to change soldier simulation would let the
same command behave differently on host and client. A synchronized distinction
between manual and AI commands needs its own protocol/replay compatibility plan.

The latter two commits also introduce a pursuit radius of 20, a fork design
choice not verified against the original. Required tests include retreat under
pursuit, host/client equality for AI commands, tower-entry priority, distant
attack targets, patrol, and save/load. No soldier-order behavior was changed by
this review.

### Thief regression validation

The arrival tests use the actual thief behavior and map stacks, isolating arrival
from target pathfinding. They verify that goods are held when the animation
starts, cannot also be taken by another carrier/thief, survive save/load exactly
once, and cannot be stolen from the thief's own territory. The two original
regressions fail against the pre-fix code with GOLD expected but NO_MATERIAL held;
they pass after the upstream fix. Interactive delivery over a full route remains
a separate gameplay check.

After the thief fix, all 78 logic unit tests and `releaseJSettlers` passed.

### Original end-of-game and production statistics

Local Ghidra analysis of the same S3.EXE established the following behavior:

- `00491820` builds the endgame dialog with resource IDs `0x900c2..0x900cb`.
  The installed language resource resolves these to Player, Points, Settlers,
  Buildings, Food, Mines, Gold, Mana, Soldiers, Battles. Buildings counts consumed
  construction materials, Food counts meals consumed by mines, and Mines counts
  extracted resources. The UI explains these meanings in column tooltips.
- `004e05e0` and state 100 in `004e9670` count births from houses and priests.
  Recruitment and combat are separate counters; initial units and transformations
  do not count as births. `0050e9f0` credits kills (one point per normal unit;
  the original's five-point catapult has no implemented counterpart here).
- `0053e720` computes points as `5*battles + 2*(soldiers + settlers) + mana +
  minedResources + mineFood + constructionMaterials + weightedGold`, then halves
  weighted gold for display. Gold production contributes one weight point for
  Egyptians and two for other civilizations; `004fa220` gives Egyptians four
  additional weight points per mined gem. The remake keeps the stored gold counter intact so
  reopening results does not alter it. New counters use long integers. Older
  saves cannot reconstruct missing lifetime events: they show a notice and no
  aggregate score, while known legacy totals remain available.
- `004ff470` shifts 256 production samples for each good; `004e7810` invokes it
  every 220 ticks. The simulation tick is 64 ms, giving 14.08 seconds per sample
  and approximately one hour of history. Worker completion calls to `004ff510`
  establish that this records production, not current warehouse inventory.
- `004ff4c0` supplies a centered 13-sample smoothing kernel
  `[3,6,8,9,9,10,10,10,9,9,8,6,3]`. Unlike the original `004ff550`, the remake
  divides by the actual participating weights (110 in the interior) and does not
  reproduce the original 100-item sample cap or biased normalization.

A local victory/defeat now opens results automatically once, with options to
continue observing or leave directly. This does not stop the shared simulation.
The regular exit screen also displays the expanded results. Selecting a material
in production statistics opens its history with 15/30/60-minute windows. History
uses simulation time, survives saves, and is bounded to 256 samples per material.
Missing pre-load history is shaded rather than reconstructed from totals; the
latest interval is explicitly marked as incomplete. Opening or changing graphs
has no simulation or network side effects. Original assets and decompiled source
remain outside the repository.

Validation after integration: 94 logic unit tests, 16 graphics tests and six
Swing tests passed, as did the save/load/save integration test and the remaining
non-integration test tasks. `releaseJSettlers` produced the playable archive.
Headless rendered previews of both statistics views were inspected. A complete
interactive match and multiplayer session have not been played through for this
change.

### Responsive measured speed display

The top-right measured game-speed indicator previously averaged 19 stale,
roughly one-second measurements. These overlapping windows kept the F12 burst
visible long after simulation returned to normal. It now measures elapsed game
versus monotonic real time in non-overlapping 250 ms intervals. Rendering delays
use their actual elapsed duration; clock resets cannot produce negative speeds.
Deterministic tests cover fast-forward followed by normal speed, pause/resume,
50 ms simulation ticks, delayed rendering and clock resets. The speed setting
and F12 simulation behavior are unchanged.

### Original numbered selection groups

Desktop now supports **Alt+1..9** to assign the current own controllable units,
**1..9** to recall them, and the currently selected group's digit again to center
on its members. There is no double-click time limit. Saving a nonempty selection
also makes that group current; saving an empty selection leaves existing groups
intact. Assigning a unit to another group removes it from its previous group.
Priests, specialists and ships are supported as well as soldiers, subject to the
remake's existing selection-type rules. The original's fixed 100-ID array limit
is not imposed on the remake's collections.

Evidence: installed `MANUAL/MILITAER/MILITAER.HTM` and
`MANUAL/E_TAST/EINLEI_3.HTM`, verified with Ghidra functions `00542660` (recall and
arithmetic-mean centering), `00542820` (assignment), `005428e0` (exclusive group
membership) and `00542990` (selection types), in the S3.EXE identified above.

The persistence design is a partial adaptation of szusik's `335b260d78589998d1096d71397b60ca03458882`
(`save and restore selected settlers groups`), extended with independent deep
snapshots and desktop keyboard support. IDs live in the local UI state and old
saves without groups load empty. Recall resolves IDs against the current map,
rejecting dead, foreign, non-controllable, garrisoned or embarked units. It uses
local selection actions and does not add network commands. Modifier key presses
are now preserved by Swing and GLFW without blocking the following digit.

Validation: 100 logic unit tests, 19 graphics tests, four keyboard-event tests,
and the common-module suite passed (157 existing common cases remain skipped).
Both the group-specific serialization scenario and the save/load/save integration
test passed. Desktop release built successfully. No interactive multiplayer
session or Android build was performed for this change.

### Save completion feedback

Saving now displays “Spiel gespeichert.” / “Game saved.” only after the save
operation returns successfully, including stream closure. I/O errors, runtime
serialization failures and interruptions display a failure message instead.
Repeated status messages replace their previous occurrence and receive a fresh
visible lifetime. Status messages have no map position and do not center the
camera. Save output streams are closed on failure as well as success.

Validation: save feedback is covered by success, repeated saves, I/O and runtime
failure, interruption, lost-player saves, message expiry and stream cleanup tests.

### Priest teleportation and divine gifts

`SEND_FOES` and `CALL_HELP` now choose free, traversable landing cells instead of
inverting the pathfinder predicate and overwriting occupants. Units remain at
source when no landing cell exists. `CALL_HELP` no longer falls through to
`CURSE_BOWMAN`. The installed military manual describes these as separate Asian
and Egyptian spells. Actual-grid regression tests cover both teleport directions,
occupied and blocked destinations, exhausted landing space and the unwanted curse.

Divine gifts now draw four positive packages from civilization-specific material
weights and quantity ranges. Evidence: Ghidra `005499b0`, material table
`0065a37c`, cumulative thresholds `0065a3fc`, quantity parameters `0065a47c`, in
the S3.EXE identified above. Each package uses two deterministic simulation RNG
draws in [0, 65536); quantity is `draw % ((n + 1) / 2) + n / 2`.
The previous uniform draw over every material enum could produce unusable goods
or empty gifts. The unsupported own-team territory restriction is removed.

Placement now follows `005046b0`: up to 10,000 offsets are examined, including
out-of-bounds positions in the budget. Existing compatible ordinary stacks have
a 37-offset head start over empty-cell placement. Full stacks retain their eight
units, and excess goods continue to subsequent destinations. Building areas,
blocked cells, incompatible stacks and protected empty cells are excluded using
the remake's map rules. New stacks also avoid occupied unit positions and the
six neighbors of existing stacks. Each deposited unit is registered as a transport offer
at its actual destination. Four packages need not produce four distinct stacks.

The offset order is reconstructed from initializer `00516530`: 5,000 wedge
points are stably ordered by `2500 * (2*x-y)^2 + 7569*y^2`, then expanded in the
original six rotations, with the origin first. The gift search uses the first
10,000 entries. This reproduces the original search order and stack preference;
the original's map occupancy flags are mapped to the remake's own terrain and
building protections, rather than copying its memory layout. No original assets
or decompiled source are distributed.

Random draws still occur for all four packages when no space is available.
Stack merging and displaced placement do not consume additional random numbers.

Validation: all 118 logic unit tests and 23 graphics tests pass, including the
new teleport, gift distribution and actual-grid gift scenarios.

Follow-up placement validation: all 132 logic unit tests pass. Dedicated cases
cover the offset order, stack merging/overflow, building stores, occupied cells,
stack spacing, blocked neighborhoods, map corners and transport offers at the
actual destinations. The lightweight movable test-map does not model gift stacks;
its gift operation is a no-op, like its other unsupported priest terrain actions.


### Siege workshops, campaigns and economy follow-up

The siege implementation adapts BotGGias's powder-maker contribution
`2ee994a75ac9c668ed1bce16a62d1160504517f5` and siege contribution
`b175d5ca4112e11e111dc7ad2638b75192aa18da`. They are integrated as a reviewed
port because this fork has different movement, statistics and savegame code.
The upstream binary replay fixtures are not replaced.

All four civilizations now have their own workshop and controllable siege
weapon; Asians also have a powder maker using coal and sulfur. Workshops accept
queued weapon orders and manufacture ammunition. Weapons reload from nearby
ammunition stacks. Selecting a single weapon shows its live ammunition count;
mixed infantry/siege selections keep their rows above the action buttons.
Selection panels, build menus, map-editor types and original
map import are wired up. Original map type 41 resolves to the authored player's
civilization-specific weapon; type 42 footprint helpers are not separate settlers.

Original combat evidence and remaining differences are documented in
[siege-gameplay.md](siege-gameplay.md). Native graphics mappings, building costs and intentional visual/layout
adaptations are documented in [siege-assets.md](siege-assets.md). Old saves'
material settings and request queues are migrated by building identity, retaining
existing priorities and deliveries; see [savegame-enum-migration.md](savegame-enum-migration.md).

Desktop campaign selection, missing-file handling, sequential mission progress
and save associations are described in [campaigns.md](campaigns.md). This is an
experimental framework: original mission scripts, alliances and original AI are
not reproduced, and the local installation lacks campaign maps.

The economy fixes preserve the configured Amazon honey/mead ratio while planning
small brewery groups correctly, and keep gem and mana budgets in the right order
when the AI recursively reduces its building plan. See [balance.md](balance.md).


Validation for this integration: 166 logic cases (three inapplicable powder-house
parameter combinations skipped), 28 graphics cases, 977 common cases (162 existing
skips), 16 desktop cases, and the save/load/save replay check passed under JDK 21.
Actual worker cycles produce each of the four weapons and load the three
ammunition types; the Asian powder worker also completes two production cycles.
These tests start from finished buildings with supplied inputs, not a complete
mining-to-construction economy. A blocked weapon exit preserves its finished
order until an unprotected spawn cell becomes available. The worker-replacement
regression preserves the order and renews ammunition supply requests.
No interactive multiplayer session or Android build was performed.

## Siege original-parity follow-up and full matches (2026-11-19)

Siege units now reserve seven hex cells while remaining one selectable/countable
unit. Pathfinding, movement, melee reach, ferry unloading and saved occupancy
are covered by targeted regressions. Projectiles retain the original target's
identity instead of damaging a replacement at the old coordinate. Values and
production recipes are based on further original executable/animation inspection:
250 HP, six-shot initial loads and ammunition batches, corrected ranges, recovery,
standard-ground movement and per-material workshop times. See
[siege-timing.md](siege-timing.md) for evidence, compatibility behavior and remaining
original differences (notably workshop ammunition storage/reloading).

The optional `:jsettlers.logic:fourCivilisationsIT` runs four complete AI matches
with a real file save/load and ordinary victory/defeat. It is separate from short
unit tests; scope and measured outcomes are in
[playthrough-validation.md](playthrough-validation.md).
