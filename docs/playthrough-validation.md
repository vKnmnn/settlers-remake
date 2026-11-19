# Four-civilisation playthrough validation

`FourCivilisationsIT` runs four real AI matches, one per civilisation, using the existing `SpezialSumpf_12.map` fixture and established difficulty-test slots 9/7. Both opponents use the selected civilisation; AI_HARD fights AI_EASY with seed 1 and separate teams. This is four same-civilisation matches, not a mixed four-player match or a human UI playtest. Inputs and seed are fixed; exact tick-for-tick replay determinism is not asserted. The dedicated PausedGameStartup helper pauses before the game clock starts and waits for startup completion, eliminating uncontrolled startup ticks. AI work still uses background executors; intermediate counts and finish times are not asserted to be identical. Assertions use save invariants and actual completion rather than exact score/minute targets.

The test advances the real network clock in one-minute steps. At minute 10 it writes a normal save file, shuts the game down, starts the saved game through `JSettlersGame`, verifies civilisation/recruitment/score persistence, then continues normal AI play. It requires a genuine WON/LOST result, economic activity, recruitment and a nonzero complete score. It never spawns units, destroys enemy buildings or sets victory flags.

Run explicitly (JDK 21):

```sh
JAVA_HOME=/usr/lib/jvm/java-21-openjdk GRADLE_USER_HOME=/tmp/settlers-gradle ./gradlew -PskipAndroid --no-daemon --max-workers=2 :jsettlers.logic:fourCivilisationsIT
```

The task runs one JVM at a time, always reruns, uses at most 4 GiB heap, and is excluded from ordinary `test` and `unitTest`. Each match has a 45-minute wall-clock timeout and a default 120-minute game-time limit. A game-time limit failure writes a diagnostic save and reports its location. JVM hangs/timeouts cannot guarantee a final save; the minute-10 checkpoint is still available once created.

Optional JVM properties passed through by the task:

- `-Dplaythrough.civilisation=ROMAN` (or EGYPTIAN, ASIAN, AMAZON) selects one match.
- `-Dplaythrough.minutes=120` changes the maximum game time.
- `-Dplaythrough.checkpoint=10` changes the save/load checkpoint, which must precede the limit.
- `-Dplaythrough.report=/absolute/path/progress.tsv` changes the append-only progress report location (default `jsettlers.logic/build/reports/playthrough/progress.tsv`).

TSV columns: civilisation, game minute, resumed-after-save, stronger state, weaker state, stronger recruits, weaker recruits, stronger score. Console output includes checkpoint/diagnostic filenames. Gradle XML/HTML reports contain the assertions and failures.

This is a simulation regression check, not proof of original-game balance or full feature coverage. These existing AI agents do not automatically exercise every profession, ship, spell or new siege weapon. Original campaign scripts, graphical menus and sound require separate tests. A timeout is reported as a failed playthrough, not converted into an artificial victory or a passing smoke test.

## Recorded validation (2026-11-19)

**Latest actual Gradle result: 3 passed, 1 failed.** `:jsettlers.logic:fourCivilisationsIT` ran in 12m51s; the Asian match remained undecided at the unchanged 120-minute limit. The earlier isolated green run below is not a reproducible all-green acceptance result.

| Civilisation | Latest Gradle result | Last game minute | Recruits hard/easy | Hard score |
|---|---|---:|---:|---:|
| Roman | WON / LOST | 77 | 515 / 338 | 11367 |
| Egyptian | WON / LOST | 84 | 434 / 377 | 10558 |
| Asian | UNDECIDED / UNDECIDED — failed time bound | 120 | 989 / 831 | 19266 |
| Amazon | WON / LOST | 75 | 166 / 80 | 5761 |

The Asian checkpoint/reload passed. Its minute-120 diagnostic save contains an active economy and substantial combat: battle points hard/easy 657/672; hard controls twelve small towers plus one large tower, easy eight plus one. A separate continuation from that diagnostic save ended normally at minute 137: hard WON, easy LOST; recruits 1404/1085 and battle points 1100/882. This rules out a permanent stall in that continued run. It does not turn the original failed test into a pass: restarting also recreates AI decision state, and the original uninterrupted trajectory beyond minute 120 was not observed. The acceptance timeout remains unchanged at 120 minutes.

There is a concrete source of run-to-run variability beyond startup: `AiExecutor.timerEvent()` submits both players' decision rules together through `invokeAll`, while those rules share `MatchConstants.aiRandom()` (`WhatToDoAi.buildTower`, `PioneerAi`, `HealSoldiersModule`). Thread ordering therefore changes which player receives each random draw even with a fixed seed. Pausing startup does not eliminate this race in decision ordering. No claim is made that it alone causes long matches or that normal AI games must finish within 120 minutes.

### Earlier isolated harness validation

An earlier isolated suite with `PausedGameStartup` ran all four parameterized matches serially in one JVM: **OK (4 tests), 475.489 seconds**. Each match saved/reloaded at minute 10, continued to a genuine win/loss, and passed the score/history checks.

| Civilisation | Final result | Finish minute | Recruits hard/easy | Hard score |
|---|---|---:|---:|---:|
| Roman | WON / LOST | 65 | 331 / 197 | 8269 |
| Egyptian | WON / LOST | 71 | 254 / 240 | 8411 |
| Asian | WON / LOST | 64 | 168 / 140 | 5655 |
| Amazon | WON / LOST | 88 | 325 / 209 | 9121 |

Validation used isolated javac/JUnit against the integration build, without launching Gradle concurrently with the main integration work. The later actual Gradle invocation is reported above and did not reproduce all four passes.

### Earlier diagnostic runs and time-budget decision

Isolated JDK 21/JUnit runs against the integration build gave these results. All four successfully wrote and resumed the minute-10 checkpoint.

| Civilisation | Initial 90-minute run | Game minutes | Wall seconds | Recruits hard/easy | Hard score |
|---|---|---:|---:|---:|---:|
| Roman | WON / LOST | 70 | 133 | 408 / 221 | 9435 |
| Egyptian | UNDECIDED / UNDECIDED — failed time bound | 90 | 220 | 485 / 376 | 10904 |
| Asian | WON / LOST | 59 | 86 | 123 / 98 | 4564 |
| Amazon | WON / LOST | 90 | 176 | 359 / 223 | 9776 |

These are observed results, not hard-coded balance targets. The first run passed three full playthroughs and failed the initial Egyptian time bound; it did not provide an all-green result.

The Egyptian diagnostic save showed battle points 121/212 (hard/easy), seven defensive buildings per side, and heavily archer-weighted surviving armies: hard 245 archers, 19 swordsmen, 30 pikemen; easy 200/14/46. Economy and combat were progressing, so this observation does not establish a simulation hang. `SoldierProductionModule` targets only ten swordsmen and twenty pikemen before favouring bows; `SimpleStrategy` sends only archers when it judges the enemy archer force too large for infantry. Those existing rules are relevant to further AI investigation, not yet a proven root cause. Continuing that diagnostic save normally produced WON/LOST at minute 115 (886/589 recruits, battle points 622/475), ruling out a permanent stall in that run. The opt-in default budget was therefore increased to 120 game minutes, and an Egyptian repeat from the beginning with only the intended minute-10 checkpoint passed at minute 61 (277/165 recruits, score 7159). That substantial variation motivated removing the uncontrolled startup ticks in the old generic helper; the final harness now uses PausedGameStartup for both starts. The initial 90-minute failure is retained here as evidence; no victory is forced.
