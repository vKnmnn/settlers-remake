# Balance audit

## Amazon mana economy — November 2026

The installed original manual, `MANUAL/W_GEB/GEBAEUDE.HTM`, anchors
`Imkerei` and `Metwinzerei`, describes forest honey production followed by
fermentation of honey and water into mead. `MANUAL/W_BERUF/BERUFE.HTM`,
anchor `Imkerin`, confirms the chain. These are local original-game sources;
no original assets or decompiled code are included here.

The remake already requires an adjacent adult tree when placing a hive
(`MainGrid.PathfinderGrid.isHivePlantable`). Saying that beekeeping has no
tree dependence at all was therefore incorrect. Its growth timer does not
currently respond to tree density or subsequent tree removal. The comments
in `HiveObject` suggesting an eight-minute initial delay and further density
effects remain unverified; no timing or density constants were changed on
the strength of those comments.

### Corrected AI supply planning

`BuildingListEconomyMinister` previously multiplied each planned increment
of beekeeping by three, and started distributing suppliers at brewery index
zero. With just one planned mead brewery, it planned **no beekeeping at all**.
With several breweries, it front-loaded suppliers, generally reaching the
map's beekeeping cap early. The existing `addIfPossible` cap prevented the
three calls from necessarily producing three times the total map allowance.

`MeadProductionPlan` now distributes the existing configured number of
beekeeping buildings across the configured breweries, with each cumulative
share rounded up. Honey suppliers and a waterworks precede each brewery and
temple in the plan. For five beekeeping buildings and two breweries, the
sequence supplies three before the first brewery and two before the second.
The minister still applies its existing map and enemy-strength limits to
every entry.

This fixes an internal planning defect; it does **not** claim to reproduce
the original AI or establish an original production ratio. The configured
`BEEKEEPING_TO_MEAD_BREWERY_RATIO` of 5/2 remains unchanged. The four focused
regression cases cover the single-brewery starvation, distributed suppliers,
uneven counts, and a zero-brewery plan.

### Still to verify

- Original hive growth and harvest intervals, including whether the initial
  delay differs from subsequent harvests.
- Tree density and the effect of felling nearby trees after hive placement.
- Production throughput and staffing across all four mana production chains.
- Food preferences versus actual mine output, and travel-time effects on
  advertised production ratios.

Do not change global timings or yields before obtaining executable evidence
or reproducible controlled measurements from the original.

### Corrected resource budget reduction

`AiMapInformation.calculateBuildingCounts` swapped its gem-mine and mana
producer arguments when reducing a mana budget above two producers to fit
available building space. This could invent gem mines on maps without gems
or replace a real gem budget with the mana count. The recursive call now
reduces only mana, leaving the gem budget unchanged. The pure calculation
is package-visible and static for direct regression coverage over constrained
land budgets, both with zero gems and with a nonzero gem allowance. No
resource yield or production ratio changes are involved.
