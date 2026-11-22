# FIRST PLAYABLE VERTICAL SLICE — two eras, Android
Approved by owner 2026-11-22. Work in parallel. This is a scope and implementation contract, NOT a claim of an existing build.

## Connected location
**2026:** fictional Moscow-region residential block, approximately 250 x 250 meters for initial testing: inhabited apartment courtyard, street with a few moving civilian NPCs, small parking area, building entrance, convenience storefront, and one anomalous basement entrance. **1348:** same geographic anchor in mythic Rus: forest track, village houses, well, small shrine, fence and path to watch post. No global apocalypse. Both scenes use the same coordinate anchor and stable IDs.

## First 10-minute free-roam loop
1. Launch on Android directly into 2026 courtyard with touch move/look; no quest acceptance needed.
2. Explore, inspect an object, speak to one NPC, pick up and store one item; save and reload.
3. Approach anomaly; witness 3–5 second alternate-era glimpse with no permanent teleport and unchanged health/position/inventory.
4. Use deliberate portal to travel to 1348 counterpart and back; distinct from glimpse.
5. Encounter one ordinary hostile occult creature with avoid/fight options. No boss, no boss UI.
6. Optional story clue may be collected, but player can ignore it and keep roaming.

## Minimal assets
2026: modular apartment facade, entrance door, stairwell fragment, asphalt/sidewalk, parked fictional car, streetlight, trees, dumpster, bench, shop front, 2 NPC variants, one enemy, 8 props. 1348: 3 modular cottages, fence, well, shrine, forest ground, 4 trees, horse placeholder, 8 props. Shared: Kirill hands/body prototype, one tool/weapon, inventory icon set, 2 anomaly material variants, touch HUD.

## Technical order and quality gates
- Establish actual UE5 project state, Android SDK/toolchain and version; do not claim APK until built.
- Produce minimal modular art atlas and real engine screenshots, not concept art.
- Android landscape touch controls, low-cost pixel/voxel materials, scalable quality settings; 30 FPS target on a defined midrange device, measured not assumed.
- TimeGlimpse: preserve simulation state during 3–5 second overlay. Full travel: explicit loading/transition and world-state save.
- Smoke tests: free-roam without quests, era travel and return, save/load both eras, enemy avoidance, UI scale and FPS logging.
- Gate: downloadable APK, genuine gameplay capture and device test report before declaring slice playable.

## Parallel work assignments
- Astra/engineering: map streaming, era manager, touch controls, save system, APK build and profiling.
- GPT #1 art: Kirill prototype, NPC variants, one ordinary occult creature, hand-held tool and animation-ready rigs.
- GPT #2 art: both modular era environment kits and props, texture atlases and collisions.
- Integration: review independent PRs against this document; no destructive rewrite of previous assets.

## Scope guardrails
Optional approximately 40-hour story is long-term target, not implemented in this slice. Five visual anomaly moods are approved for later zones. No bosses. Two eras mandatory. Small, dense map first, then expansion after measured performance.
