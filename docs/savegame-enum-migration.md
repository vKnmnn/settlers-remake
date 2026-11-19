# Loading saves from before siege weapons

The five new building types and six new movable types are appended to their enums,
so existing ordinal task records and named Java enum values retain their identity.
An old save nevertheless contains arrays and material request queues sized before
those additions. Loading now upgrades the two persisted structures affected:

- `MaterialDistributionSettings` extends its `RelativeSettings` ordinal table.
  Existing preferences, including disabled consumers, are preserved. Newly added
  consumers of this material and civilization receive the normal default weight
  of one; irrelevant building types remain zero. Current saves are not renormalized.
- `MaterialsForBuildingsRequestPriorityQueue` rebuilds its consumer index and
  queue matrix by building identity. Pending requests retain order, priority,
  in-flight delivery counts and their queue back-references. Stock, harbor and
  market queues move to the new trailing indices rather than becoming workshop
  queues. Newly introduced consumers receive empty queues.

`BuildingCounts`, `SettlerInformation`, AI building counts and graphics lookup
arrays are reconstructed at runtime; they do not retain old serialized lengths.
Profession limits consist of three explicit bearer/digger/bricklayer fields,
not a movable-enum-sized array. No material enum additions are involved here.

All four regression tests passed under JDK 21 / JUnit 4.12. They serialize objects with the pre-siege array/list shape and
load them through the normal Java object stream. They cover user weights across
all civilizations/materials, queue identity and delivery preservation, new
workshop/powder requests, repeated saves, and merging a migrated queue into a new
partition. This supplements the full save/load replay checks; it does not claim
compatibility with arbitrary historical forks or changed enum ordering.
