# Siedler III AI reconstruction

Status: experimental, **partial reconstruction**, not a complete port of the original AI.
The player menu exposes `S3 reconstruction (normal, experimental)` and
`S3 reconstruction (easy, experimental)` separately from all four existing
JSettlers profiles. They are opt-in, including for imported maps; automatic
player defaults and existing saved profile names retain their old meanings.

## Evidence

Analysed locally with Ghidra: `S3.EXE`, SHA-256
`c2977b67f42c12cb03200c3c282d7f38010e353232689137133c591ebefef2a0`.
Addresses below refer to that binary's virtual addresses. Decompilations stay
outside the repository. `tools/research/InspectFunctions.java` exports selected
functions and their callers/callees from a local Ghidra project. No original
executable, assets, or decompiled function bodies are included here.

The installed README attributes the easy **campaign** option to version 1.54.
The language resource has separate campaign easy/normal entries (IDs `0x90170`
and `0x90171`); the menu at `00474010`/`00475640` reads/writes the flag through
`00414030`/`00414040`. This is evidence for two modes in this executable, not
for four original difficulty algorithms. The existing four JSettlers tiers
scale economic targets, attack ratios and healing probabilities.

### Easy campaign mode

Both gameplay call sites found for the easy getter require a nonzero campaign
ID and computer ownership:

* `004fa220`: after a successful mine extraction has depleted the deposit,
  retain the resource only if an additional unsigned 16-bit random draw is
  **<= 22000**. Thus 22001/65536 (about 33.57%) of otherwise successful
  extractions produce output. Failed extraction does not consume this extra draw.
* `0050e9f0`: damage against a computer-owned target is increased by
  `damage >> 1` before health subtraction. This is incoming damage, not increased
  human attack planning or a slower AI clock.

The new easy profile deliberately makes these handicaps available per player
in free games as well. Existing AI profiles and humans are unaffected. The
JSettlers adaptation retains floating-point damage and applies a factor of 1.5
in movable hit processing (also covering its siege movables); original integer
rounding and the exact coverage of other original damage paths are not reproduced.
Mining uses the saved game RNG and applies the handicap after depletion, before
productivity/output accounting. Normal and easy use the same planning policy.

### Economic planning

The original has specialised agents, region state and priority requests:

* `00550900`: computer-player construction. The fourth argument concerns
  one-time shared initialisation, not difficulty. A profile-table index at
  object offset `0x770` is initialised to zero; other difficulty presets have
  not been established from that table.
* `0056f6d0`: factory for eleven agents; `00567890`/`0056a1a0` run house-demand
  statistics and individual building rules.
* `00551a70`: highest-priority request among 57 original building IDs, first
  wins ties, immediate stop at priority 65000. It also gates requests on a
  regional count; interpreting that count as a construction limit is unproven.
* `00551b10`: priority ageing with per-building increments/ceilings. This
  stateful scheduling is **not implemented** in the reconstruction yet.
* `00567910`, `0059dcc0`, `0050e750`: economic population counts non-specialist
  civilians in a region. JSettlers currently supplies the player's civilian
  count; region-local statistics and the original alternate global-count mode
  remain unported.

Implemented independently in `S3ConstructionPolicy`:

| Rule | Evidence | Behaviour |
| --- | --- | --- |
| Lumberjack demand | `00567e00`, tables `0065a88c`, `0065a8a0` | Population-interpolated building/wood targets; max of capacity/stock priorities; Asian one-extra exception |
| Stonecutter demand | `00568420`, tables `0065a8b4`, `0065a8c8` | Same demand bands, first stonecutter mandatory; Egyptian one-extra exception |
| Sawmills | `00568250` | First sawmill, then ceil(lumberjacks / 3) |
| Foresters | `005682b0` | Deficit from lumberjacks, wood stock and civilisation-specific forestry capacity |

Population breakpoints are 0, 40, 100, 500, 2000 (`0065a85c`). Slopes are stored
as floats, interpolated targets truncated. Above 500 the last segment is used,
including extrapolation beyond 2000. Capacity priority bands at 10/30/50/70/100%
are 65000/55000/45000/25000/10000/0; stock bands are
65000/45000/35000/15000/5000/0. Tables use Roman/Egyptian/Asian/Amazon order.
Forester capacity per building is 250/300/200/200 against 100 per lumberjack,
minus trunks plus planks in stock. Priority divisors for deficit >80/>60/>40/
>20/>0 are 1/2/3/4/6, otherwise zero. Sawmill and forester base priority is
65000 (`007328f0`, `007328f2`).

`S3EconomyMinister` suppresses new requests for an industry type with unfinished
construction, ranks these requests in original ID order on ties, and replaces
those four building types in the old plan. **All other economic planning still
uses BuildingListEconomyMinister as an explicit adapter.** Industry requests
currently precede that fallback rather than competing with all original agent
requests. Housing, placement, pioneers, expansion, resource exploration,
construction concurrency and endgame land-use policy also remain JSettlers.
Consequently neither pacing nor complete build orders are original-equivalent.

### Military planning

`0058f990` coordinates military updates; `00594080` evaluates attacks.
The implemented sufficient count-based conditions are at least 15 available
soldiers and either enemy count < own count * foreign combat strength * 0.75,
or own count > enemy count * 4. The normal and easy variants share these gates;
the legacy requirement for five economic building types is not applied to them.

The original also has a weighted-strength comparison with factor 2.2, local
threat checks (radius 60, factor 1.5), connected-region and human-target filters,
up to nine attack groups, regrouping/delays, campaign-specific exceptions and
fallback attacks. These are **not yet ported**. The current reconstruction uses
JSettlers weakest-enemy/tower targeting, movement, defence, recruitment, healing
and upgrades. Count gates alone are not a complete reconstruction of combat AI.

## Determinism and compatibility

Existing enum values 0..4 stay in place; new profiles are appended. Player
settings save the enum names. Desktop and Android player selectors expose the
new values; LAN uses the same type IDs. All participants need a matching build.
Campaign selection also exposes all six profiles and preserves the former JSettlers hard default.
Saving/loading retains the selected profile. Clients and replay playback now retain the actual profile rather than disguising
computer slots as HUMAN. A local `PlayerSetting.withoutAiExecution()` policy
suppresses decision generation on those instances; it is deliberately not
written into save/replay settings. Mining and damage therefore see the same
profile on every participant, and a client save can later run AI locally.
No extra transient plan state is
saved; it is rebuilt from statistics, as with the existing AI.

AI decision callbacks now run in player order on the simulation thread because
they share the saved AI random stream. Statistics can still use the worker pool.
This removes a concrete source of scheduler-dependent random draws, not a claim
that every existing replay/save determinism issue has been resolved. Exceptions
in a decision callback are now logged instead of hidden in unobserved Futures.

## Validation and remaining work

`S3RulesTest` checks numerical boundaries, all 65536 mining draws, isolation of
other profiles, legacy wire IDs and player-setting round trips.
`AiDecisionOrderTest` checks callback/RNG order and interruption.
`s3AiIT` runs both reconstructed profiles plus a legacy profile on a real map,
for each civilisation, with a save/load checkpoint and economic/recruitment
assertions. It is separate from the normal unit-test suite.

Before calling this the original AI without an experimental qualifier:

1. Reconstruct remaining economic request agents, their priorities, ageing,
   cooldowns and per-region bookkeeping instead of the legacy fallback.
2. Decode and port weighted enemy strength, target filtering, defence,
   attack-group lifecycle and campaign-specific instructions.
3. Compare recorded decisions against the original on controlled maps, not
   just successful JSettlers simulations; establish original clock cadence.
4. Persist any newly introduced state and verify deterministic save/resume
   and LAN/replay behaviour with those agents.

No third, newly invented strategy is added in this change. A separate adaptive
AI can follow once the original-derived baseline is sufficiently complete for
meaningful comparisons.

Validation on 2026-11-26: four 60-game-minute simulations (both profiles in each,
plus a legacy AI) passed with a checkpoint at minute 10. At 30 minutes, three
civilisations had not recruited yet; the longer runs established that these
settlements continued developing rather than remaining stuck. These allied
smoke matches exercise economy/save-load, not victory balance or original-game
parity. Combat decision boundaries and actual incoming damage are covered
separately. Fog of war is disabled in these simulation tests; an initial run
with it enabled also logged a background FogOfWar exception during reload,
which is outside this AI change and remains to be investigated.

The full logic unit run completed with 340 cases (3 skipped), desktop with 24
cases, common with 979 cases (162 skipped), without failures. Network tests were
already up to date (74 cases, 19 skipped). An additional
`AiExecutionOwnershipTest` then passed on a real map: the remote execution policy
emits no AI commands, and the same profile emits commands when run by the host.
`releaseJSettlers` builds successfully. Android selectors were updated but the
Android application was not built in this local validation. A live two-machine
LAN/replay equivalence run remains outstanding.
