# Fork integration

Baseline: paulwedeck/settlers-remake, `6ad8c4230`.
Integration branch: `integration/fork-improvements`.

## Integrated

- BotGGias `329fa153fe`: Gradle 8.14.3, updated plugins and CI, JDK 21 build
  with Java 11 bytecode. Desktop validation only; Android migration unverified.
- BotGGias `5ece051ea6`: production statistics and production mission goals.
  Resolved Player.java context without importing the unrelated peace-time field.
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
- Local tests cover production victory thresholds, fog update exclusion during
  saving, and incomplete savegame rejection. Desktop CI explicitly opts out of
  Android, uploads test reports, and runs a save/load round trip.

## Local verification (2026-11-18)

JDK 21.0.13.1, Linux. Desktop release and unit tests passed.
Common: 936 cases, 157 skipped; graphics: 9 passed; network: 74 cases,
19 skipped; logic: 53 passed (including mage save and marker-cleanup regressions). Save/load/save integration: 1 passed.
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
- TheRealDrNick soldier-order and thief fixes, then naval/colonization AI with
  deterministic runs and balance tests.
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
| P1 | Original-map garrison counts are discarded. | `OriginalMapFileContentReader` decodes nine soldier counts and passes them to `OriginalMapFileContent.setBuilding`, which ignores the counts. |
| P1 | Imported rice liquor and mead stacks disappear. | `EOriginalMapStackType` maps both to null; `OriginalMapFileContent.setStack` skips them despite existing `LIQUOR`/`MEAD` materials. This is an import defect, not missing alcohol production. |
| P1 | Unsupported mission objectives can be silently omitted. | Building-destruction import and `OriginalSinglePlayerWinCondition.checkProduceCondition` skip null-mapped types. Which installed missions are affected needs a data audit. |
| P2 | Multiplayer-save listing has a stub. | `MapList.getSavedMultiplayerMaps()` returns null. This alone does not prove that every alternative resume path is absent. |
| P2 | AI rice-economy planning is incomplete. | `PioneerAi` lacks a swamp-target strategy; `WhatToDoAi` leaves rice-farm work-area adjustment as TODO. Human rice farming exists. |

Unverified: production-goal parsing reads overlapping type/amount bytes in
`OriginalMapFileContentReader`; verify the map format before changing offsets.
Comments about original beekeeping rules also need independent verification.
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
