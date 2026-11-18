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
19 skipped; logic: 50 passed (including mage save regression). Save/load/save integration: 1 passed.
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
