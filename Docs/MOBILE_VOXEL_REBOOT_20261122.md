# РАЗЛОМ: ЧЁРНЫЙ ЗВОН — MOBILE 3D PIXEL/VOXEL REBOOT
Decision date: 2026-11-22. Owner-approved direction. This document supersedes platform/visual requirements in older prompts and GDD until they are migrated; story canon remains intact.

## Platform and visual direction
- Primary release target: Android mobile, landscape, offline single-player. Windows is development/editor platform, not release priority. iOS is not yet approved.
- Full 3D gameplay with stylized pixelated/voxel-like geometry, crisp pixel textures, controlled lighting, fog, and dramatic anomaly effects. Not 2D sprites and not photorealistic AAA.
- Five approved visual mood references (ALL part of one cohesive game, NOT five different rendering pipelines): (1) flooded subway and cyan rift; (2) abandoned city and teal sky fracture; (3) crimson village and purple lightning; (4) industrial red rift; (5) foggy forest and violet anomaly.
- Concept images are visual targets, NOT verified gameplay screenshots or existing assets. Reproduce style through actual geometry/materials and capture from the running build.
- First-person view in concept images is a provisional prototype camera, not an irreversible change to original third-person story gameplay. Prototype both or obtain explicit camera decision before locking.

## Preserve narrative canon
- Kirill Vorontsov, the two epochs (mythic Rus 1348 and living Russia 2026), Order of the Veil, Seven Bells, occult monsters, and 3–5-second time glimpses are retained.
- Modern Russia is mostly populated and ordinary; ruined/abandoned locations are isolated anomaly zones, not permanent global apocalypse.
- Story scale remains an aspiration, not a deliverable promise.

## Technology and performance
- Continue auditing existing UE5 project; do not blindly delete assets or switch engine. Android Vulkan/compatibility and packaging must be proven on real hardware before committing to UE5.
- Baseline design target: 30 FPS on a representative midrange Android device, scalable to 60 FPS on capable devices; these are targets, not measurements. Device RAM, thermal and package-size budgets must be established via profiling.
- Mobile-forward materials: compact texture atlases, low-cost shaders, baked/static lighting where appropriate, limited dynamic lights, LOD, occlusion/culling, low overdraw, capped particles. Avoid relying on Lumen/Nanite desktop assumptions.
- Touch controls: left movement stick, right look, interact, attack, dodge, inventory, accessibility and adjustable UI.
- Preserve source originals in Git history. Previous blockout OBJ assets are WIP, not approved production art.

## Acceptance gate for every art deliverable
1. Real editable 3D source (blend or procedural source), exported glTF/FBX/OBJ as appropriate, stable scale/pivot/names.
2. Pixel/voxel art direction compliance, UV/atlas, mobile material setup, LOD and collision where relevant.
3. At least front/side/in-scene screenshots from actual source geometry; label any concept separately.
4. Engine import and on-device test status reported truthfully; no claims of success without logs/captures.
5. Measured polycount, draw calls/material slots, texture memory and FPS once integrated. Polygon count alone is NOT quality.

## Work division
GPT #1: hero, NPCs, occult creatures, weapons, character rigging and animation-ready assets.
GPT #2: environment, buildings, terrain, interiors, set dressing, modular kits.
Astra: integration, game systems, Android packaging, performance, QA. Separate branches and PRs; no merge without integration checks.

## First vertical slice
One 2026 urban block with a normal lived-in street and a localized anomalous entrance; one short 1348 village/forest path; one interactable NPC, one occult enemy, touch movement/combat, 3–5-second time glimpse, save/load and Android build. Visual reference includes all five palettes as later biomes; do not attempt five full levels at once.

## Immediate migration checklist
- Update 04_ASTRA_MASTER_PROMPT.md, Docs/01_CANON_GDD.md, RAZLOM_ALL_IN_ONE.md, roadmap, decisions log and progress file to point to this decision.
- Mark old Windows-only/4K/photorealism language superseded, not as still-current requirements.
- Audit existing art PRs; retain usable geometry as source WIP; no bulk destructive replacement.
- Create separate art and engineering tasks with acceptance gates above.
- Build a reproducible pixel/voxel style test scene and obtain genuine engine screenshots before mass production.

## Owner decision update — 2026-11-22: open world, NO BOSSES, parallel production
- **Remove bosses entirely.** No boss fights, boss arenas, boss health bars, mandatory boss encounters, or boss asset production. Existing references to 13 bosses are superseded and must be removed from active scope, prompts, bestiary, roadmap and task trackers. Historical files can retain old text only if explicitly marked obsolete.
- **Open-world sandbox with optional storyline.** Player may explore, survive, investigate, trade/interact and engage with the world without starting or completing the main story. Story quests are opt-in; free-roam progression must not be blocked by a story boss or required linear chapter. Preserve lore, two epochs and time glimpses, but redesign gating to support non-linear exploration.
- **Parallel development.** Gameplay, world streaming, Android performance, and assets are built concurrently via independent branches and integration checkpoints; never wait for all art to be complete before playable tests.
- Replace boss-centered milestones with emergent encounters, AI ecology, points of interest, exploration activities, optional quests and world events. Ordinary hostile creatures may still be dangerous without becoming bosses.
- Reconcile the previous 14-act/112-mission plan as optional narrative content, not mandatory linear progression; avoid promising full scale before validation.

## Owner decision — 2026-11-22: two epochs, reduced campaign scope
- Retain **both playable eras**: mythic Rus 1348 and Russia 2026, connected through time-glimpses and designed transition points.
- Replace old **110–140-hour main-story** target with **approximately 40 hours (+/-)** of optional narrative content. This is a content design target, not verified playtime.
- Remove 14 acts / 112 mandatory missions as binding production quotas; restructure into a smaller, coherent non-linear campaign with side stories and optional exploration. Do not promise exact act or mission counts before pacing tests.
- Keep the open world at a smaller, denser scale than the old 36 km² per era concept. Map area must be set after a streaming/performance prototype, not invented in advance.
- Bosses remain fully removed; story progression must never depend on boss fights.
- Free-roam mode remains accessible independently of story. Assets and engineering progress in parallel.
