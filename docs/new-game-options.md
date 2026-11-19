# New-game settings

The desktop setup dialog now separates map-authored starting goods from an
explicit low/medium/high override. Existing buildings and supplies in scenarios
are preserved. Remake maps without a goods setting retain the historical high
starting stock. `MAP_DEFAULT` is appended to the enum so old replay ordinals keep
their meaning.

Team presets configure the actual player teams: free-for-all, two teams, or the
human against the computer players. Individual team assignments remain possible;
fixed map teams take precedence. These convenience presets are not a recreation
of the original online lobby or ranked matchmaking.

The Amazon option restricts both explicit choices and random civilisations.
Disabling it resolves random choices from the other three civilisations using
the match seed; the resolved choices are recorded in the replay. Authored Amazon
slots cannot be silently converted to another civilisation.

Military victory remains the default. Free play suppresses automatic victory
when only one team remains; losing the last military building still defeats a
player. This makes an ordinary single-player sandbox possible. Scenario victory
conditions and rules restored from saved games take precedence over the dialog.

## Original evidence

Local manual: `MANUAL/E_ALLG/VORBER.HTM`, sections about creating a game and
additional settings. It documents map/default starting goods, teams, free play,
the Amazon option, and economic mode. These are separate from the original
online league, readiness, and lag-tolerance controls.

Ghidra analysis of the local `S3.EXE` with SHA-256
`c2977b67f42c12cb03200c3c282d7f38010e353232689137133c591ebefef2a0`:

| Address | Observation |
| --- | --- |
| `00488ac0` | Setup dialog transfers selected values into game configuration. |
| `0048abd0` | Setup validates player/team totals against map capacity. |
| `00437450`, `0053c9e0` | Amazon permission controls random civilisation selection from three or four possibilities. |
| `00437460`, `0053c9e0` | Economic-mode flag is copied from setup to match state. |
| `004ffbc0` | Economic mode initializes a timeout and seven distinct permitted goods. |
| `004ffc80` | At evaluation, each goods category compares the local team's counter total with the other teams combined; category majorities determine the result. |
| `00511c50` | Evaluation is triggered when the configured simulation-time limit expires. |
| `004e7810` | Economic mode scales outside-territory combat strength by 50 percent. |

Decompiled files and the proprietary executable stay outside the repository.
The current evidence does not yet establish every economic counter update or
the complete original dialog control mapping. Economic mode is therefore not
exposed as a nonfunctional checkbox. Original random-map generation, online
league matchmaking, mutable alliances, and lag tolerance remain separate work.

## Persistence

New rule settings travel with the initial game state and saved grid. Legacy
replay headers retain the military default; cloning an initial state retains its
rules. Loading a saved match must use its stored rules, never replace them with
the current new-game defaults. Existing enum ordinals and Java serialization
identities are preserved.

## Economic-mode research (not yet implemented)

The original compares **current goods holdings**, not cumulative production.
The shared per-player array starts at match offset `+0x11944b8`, has unsigned
16-bit entries indexed by original material ID, and uses a `0x148`-byte player
stride. The following additional Ghidra evidence distinguishes holdings from
production totals:

| Address | Counter evidence |
| --- | --- |
| `004ffa50` | Clears all 20 players' 36 counters, then rebuilds them from existing material objects and the territory owner at their coordinates. It filters object states before counting. |
| `004ffb00` | Adds a quantity to a player's material counter; ignores player `0xff` and suppresses updates while the flag at `+0x2d250` is set. |
| `004ffb40` | Subtracts a quantity from the same counter, saturating at zero. This rules out a cumulative production interpretation. |
| `004e9670`, worker states `0x31`/`0x34` and `0x32` | Consuming material ID 3 subtracts one through `004ffb40`; the following production state adds material ID 1 through `004ffb00`. A separate call to `004ff510` records production. |
| `005046b0` | Magic-created goods also increase the holdings counter, using the actual quantity added to a stack. |

`004ffbc0` draws seven distinct goods with `(random & 0xffff) % 34 + 1` and
rejects IDs 21, 25, 27–31, 33 and 34. Its additional rejection of 35 is redundant
for that draw range. It stores the selected IDs at `+0x1195dcd` and the mode flag
at `+0x1195dcc`; it also initializes `+0x2d24c` to 10. That last value is **not**
evidence of a requirement to produce ten of each good: the result evaluator
never uses it as a victory threshold.

At the configured deadline, `004ffc80` sums each selected good for the local
team and separately for all other participating teams combined. Each category
with a strictly greater team total counts as a category win; a strictly smaller
total counts as a category loss; tied categories count for neither side. More
category wins than losses returns 1, fewer returns 0, and equality returns 2.
This is not a comparison against only the strongest opponent or a requirement
to win four categories regardless of ties. `00511c50` converts the configured
minutes to original simulation ticks using `(minutes * 60000) >> 6`.

A complete implementation still needs explicit handling of carried goods,
transport, conquest and other ownership changes, plus the result presentation
for ties and matches containing more than two teams. The object-state filter in
`004ffa50` has not yet been fully decoded. Existing cumulative production
statistics and available-offer counts are unsuitable substitutes for this
holdings counter. The new-game dialog therefore does not offer economic mode
until these rules, persistence, and the visible outcome are implemented together.

## Validation (2026-11-19)

- Logic unit suite: 328 cases, three existing skips, no failures.
- Desktop Swing suite: 23 cases, no failures, including six setup-option tests.
- Save/load/save replay integration: passed.
- Original map-info fixtures exercise all three authored starting-stock values;
  scenario objects survive every override. Replay v1/v2 and the saved grid retain
  their intended rule semantics; a real tower-loss check distinguishes military
  victory from free play.
- Rendered the actual dialog headlessly with the installed original graphics in
  German. Exercised decreasing/increasing player count, team presets, manual
  override and the Amazon selector. This checks the Swing setup flow, not a full
  interactive match.
- Desktop release built with an isolated fat-JAR destination so the package
  build does not replace a running game's JAR.
