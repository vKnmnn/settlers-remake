# Original campaign flow

The desktop main menu now has a campaign selector with eight separate campaigns
and 72 numbered missions. Victory in the local player-0 mission unlocks the next
mission; completed missions remain replayable. Defeat, quitting, multiplayer and
victory as another player do not advance the campaign.

This is an **experimental campaign framework**, not complete reproduction of the
original campaigns. The existing original-map importer handles its supported map
objectives. Campaign-specific scripted events, alliances, cutscenes and original
AI behaviour have not been implemented or verified. Enemy slots currently use the
remake's hard AI and separate teams. Missions depending on these missing systems
may not be completable or balanced like the original.

## Original evidence and files

The installed `S3.EXE` contains these explicit campaign file sequences:

| Campaign group | Paths | Missions |
| --- | --- | --- |
| Base game, Romans | `S3/Mis_M/R/r1.map` through `r8.map` | 8 |
| Base game, Egyptians | `S3/Mis_M/E/e1.map` through `e8.map` | 8 |
| Base game, Asians | `S3/Mis_M/A/j1.map` through `j8.map` | 8 |
| Mission CD | `S3MCD/Map/Mis_M/{R,E,A}/` with the same filenames | 8 each |
| Amazon expansion | `S3Qota/Mis_M/Z/z01.map` through `z12.map` | 12 |
| Amazon expansion | `S3Qota/Mis_M/G/g01.map` through `g12.map` | 12 |

The Amazon assignment is verified in the executable, not inferred from the folder
initials: at `0x52652f`, the `Z` prefix selects mission IDs 49–60, which the
range check at `0x5265d6` identifies as Amazon versus others. The `G` prefix
selects IDs 61–72, identified at `0x526751` as others versus Amazons.

The installed solution pages under `SOLUTION/strategy/missions/` independently
list the three base campaigns, three mission-CD campaigns, and the Amazon and
other-nations campaigns, with these mission counts. Numeric ordering is preserved
even when files are missing. The new sequential unlock policy is the remake's
initial policy; exact original branching/unlock conditions remain unverified.

Only paths and format facts are included in the repository. No original maps,
briefings, movies or binary/decompiled program contents are redistributed.

Copy the original campaign directory trees from owned game media into the
configured Settlers III directory, or point the **additional maps directory** in
Settings at a directory containing these trees. The base game's `Mis_M` directory
can also be placed directly in the Settlers III directory. Resolution is
case-insensitive for Windows media copied to Linux. Full campaign paths distinguish
base and expansion missions with identical filenames. Missing missions are shown
with their expected paths and cannot be launched.

The installation inspected at `/home/omicron/.local/Siedler3` contains only
`Map/TUTORIAL`, `Map/SINGLE`, `Map/MULTI` and `Map/User`; its campaign map trees are
absent. Consequently, full original-campaign playthrough validation is blocked by
missing original map data. The selector honestly displays that absence.

## Launch, savegames and progress

Launching reads the map outside the Swing event thread, preserves authored player
civilizations, assigns human control to player 0, and does not add skirmish starting
towers, settlers or goods. If present, the map's own mission description is shown
before launch. Unknown/free-choice civilizations are rejected instead of silently
randomized. Invalid/unsupported map data retains the existing import errors.

Progress and map identity associations are stored in
`campaign-progress.properties` in the existing JSettlers configuration directory.
Updates use a temporary file and atomic replacement where supported; failed writes
are reported and do not mark the mission completed in memory. This is local UI
metadata, with no simulation/network serialization changes.

The existing save header retains the base map ID. Loading a save after restarting
the application restores a known mission association using that ID, rather than
its display name. Reloading a previously won mission does not advance multiple
levels. Campaign launches, normal games and multiplayer are explicitly separated
so an earlier campaign selection cannot accidentally unlock a later normal game.
Copying only a save to another machine does not copy the local campaign metadata;
copy the progress file as well to retain its association and unlocks.

## Verification

All 10 targeted campaign tests passed under JDK 21 / JUnit 4.12. They cover the catalog, case-insensitive file discovery, separation of
identically named base/expansion files, missing files, progression after restart,
save base-map identity, failure-safe persistence, human/AI civilization setup,
cancelled launch handling, and isolation from unrelated games and multiplayer.
Original campaign events and interactive full playthroughs remain untested for
the reasons above.
