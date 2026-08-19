# Boss Fight Log

A RuneLite side panel that keeps a per-kill record of your boss fights.

Where the Loot Tracker shows you *what you got*, Boss Fight Log shows you *how you did*: kill time,
DPS, damage taken and accuracy for every kill, with each kill compared against your own previous
kills of the same boss.

## What it records

Every kill stores the time taken, damage dealt, damage taken, hits and misses, and the largest
single hit. From those it derives DPS, accuracy, and how the kill compares with your others.

- **All kills** lists every fight, most recent first, with a coloured stripe showing whether it beat
  your average for that boss.
- **By boss** collapses to one row per boss carrying its own averages and best time, expanding on
  click to reveal the kills behind it.

History is kept per character under `.runelite/boss-fight-log/`, so it survives restarts and
accounts do not pool their kills.

## How the numbers are measured

- **Time is counted in game ticks**, not wall clock, so it does not drift with frame rate or
  latency.
- **Gaps longer than the idle timeout are deducted**, so banking or walking away mid-fight does not
  count against your DPS. The timeout is configurable.
- **A kill landed inside a single tick reads as "instant kill"** rather than an enormous rate: there
  is no duration to divide by, so no rate is reported and it is left out of every average.
- **The comparison baseline is your other kills of that same boss**, measured as total damage over
  total time, so one very short fight cannot skew it. It is hidden below one percent, where the
  sign is rounding rather than performance.
- **Damage over time you apply is not counted.** Poison, venom and burn hitsplats are not
  attributed to a player, so there is no way to tell yours from anyone else's.

## Scope

This plugin is **panel-only and post-kill**. It draws nothing in the game world, has no overlay, and
surfaces no information while a fight is in progress — a kill appears only once it has already
resolved. It is a record of what happened, not an aid to what is happening.

## Bosses

Covers single-target bosses matched by name, including God Wars Dungeon, the Desert Treasure II
bosses, the Wilderness bosses, Dagannoth Kings, Barrows, Perilous Moons and the common Slayer
bosses. Grotesque Guardians and the Royal Titans are recorded as one kill covering both halves.

Raids and wave minigames are not covered, other than their final boss where that boss is a single
NPC.

## Examples:

<img width="230" height="559" alt="image" src="https://github.com/user-attachments/assets/5008d719-d55f-47e4-859d-9a31c2c66d6e" />


## Licence

BSD 2-Clause. See [LICENSE](LICENSE).
