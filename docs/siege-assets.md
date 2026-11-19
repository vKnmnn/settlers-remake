# Siege graphics verification

The siege and powder-maker fork contributions were checked against the installed
original RGB565 `Siedler3_*.f8007e01f.dat` files on 2026-11-19. No original images
or decompiled code are included in this repository.

## Verified sequences

| Asset | DAT file / sequence | Frames |
| --- | --- | --- |
| Catapult body, wheels | 12 / 0, 1 | 6; 216 (36 per direction) |
| Catapult firing | 12 / 2–7 | 24 per direction |
| Ballista body, wheels | 22 / 0, 1 | 6; 216 |
| Ballista firing | 22 / 2–7 | 15 per direction |
| Ballista bolt | 22 / 8–13 | 3 per direction |
| Cannon body, wheels | 32 / 0, 1 | 6; 216 |
| Cannon firing | 32 / 2–7 | 18 per direction |
| Cannon ball | 32 / 8 | 12 |
| Gong standing/walking | 42 / 0 | 72 (12 per direction, wheels included) |
| Gong striking | 42 / 1–6 | 39 per direction |

Direction order is southwest, west, northwest, northeast, east, southeast.
Firing sprites contain the entire weapon; they replace the standing body and
wheel layers instead of being drawn over them.

| Building | Final/build sequence | GUI file / icon | Planks / stones |
| --- | --- | --- | --- |
| Catapult workshop | 13 / 27 | 14 / 96 | 5 / 7 |
| Ballista workshop | 23 / 31 | 24 / 102 | 3 / 9 |
| Cannon workshop | 33 / 34 | 34 / 111 | 9 / 3 |
| Gong workshop | 43 / 32 | 44 / 102 | 8 / 10 |
| Powder maker | 33 / 28 | 34 / 105 | 5 / 3 |

Construction costs are documented in the installed original manual,
`MANUAL/W_GEB/GEBAEUDE.HTM`, building-cost tables. Costs exceeding eight units
are divided between construction stacks. Final images use frame 0, construction
images frame 1. Collision footprints and worker/stack positions were derived
from the native sprites on the remake's 16-by-9 projected grid. They are
conservative remake layouts, **not recovered original collision tables**.

Active weapon icons are `14/234`, `24/228`, `34/243`, and `44/237`.
The fork initially used disabled icons for the first three weapons, the gem
mine icon for the ballista workshop, the powder house for the cannon workshop,
and a rotating windmill sequence for the powder maker.

The Asian powder worker uses file 31: powder bowl 163/164, coal 165/166,
sulfur 167/168, empty hands 169, and empty bending 170/171. The loaded walking
sequences have 72 frames. The corresponding loaded handling sequences have
8, 9 and 9 frames. The original fork mixed walking and handling sequences,
which could select frames outside the sequence.

Siege engineers currently reuse dock-worker working and material-carrying
animations. These are intentional visual substitutions; an exact original
siege-builder work cycle has not been reconstructed.

## Validation

`SiegeSpriteMappingTest` checks parsed mappings against verified frame metadata
without requiring proprietary assets: all weapon firing directions, gong
walking direction blocks, powder-worker material identities, and engineer
material/handling mappings for every civilization.

A separate local harness called the actual `SettlerImageMap` and original DAT
reader for 73,232 mapped frames, sampling animation progress from 0 through 1
in steps of 0.01. It checked siege standing, walking, firing and wheel layers;
engineer empty/plank/iron/stone/gold walking, standing, raising and bending for
all civilizations, engineer work animations and Asian powder handling; and the powder worker's used animations. No out-of-range
frames, missing images or fallback bearers were found. A contact sheet of
all four weapons in all six directions and three action states was rendered
through those mappings and visually inspected. This is asset validation, not
a substitute for a full interactive gameplay test.

Desktop build-menu wiring includes the workshops in the military category and
the powder maker in the material category. Workshop selection creates the
weapon-order action with the corresponding active icon; the soldier-selection
panel includes all four siege types. These paths were inspected in source.
