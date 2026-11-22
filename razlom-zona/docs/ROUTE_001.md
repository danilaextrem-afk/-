# ZONA route RZ_ZONA_ROUTE_001 — village / forest / checkpoint / G-4

This is **gameplay blockout**, not an approved art asset. It extends PR #137 without editing bunker geometry or the older art branches.

- Entry: spawn (-5, 0.25, 10.8), village road (8,0,2), forest turn (18,0,-6), checkpoint shoulder (18,0,-12), G-4 approach (24,0,-18), existing ramp starts (24,0,-20).
- Scene implementation: `scripts/route.gd` loaded by `scripts/game.gd`; original Godot BoxMesh source is editable and deterministic.
- Objects: road stripes, 10 low-poly forest trees, guard kiosk east of road, raised barrier, five path lights.
- Collision: Godot BoxShape3D on kiosk, posts, trees. Markings and lighting non-solid. Existing terrain/road/gallery colliders remain in place.
- New standalone art deliverables: **none**. This pass is route gameplay and placement, not the final guardhouse glTF from PR #134. Asset integration requires import and material review before use.
- QA script `tests/route_walk.gd`: physics input from village to the gallery approach, checks grounded movement and checkpoint nodes. Existing `tests/bunker_walk.gd` still covers gallery descent, pickup, save/reload and return.
- GitHub CI workflow runs Python source checks, Godot clean import, both physics scripts and player-view capture. Passing CI is **not** claimed until actual workflow logs exist.
- No Android build, device QA, FPS, manual LOD, pixel atlas, PBR approval or new glTF is claimed. Route props use procedural BoxMesh and standard colors pending replacement by reviewed original assets.
- Next: inspect CI logs; resolve route physics regressions, then integrate the guardhouse after its draft PR #134 is reviewed and create original forest road surface mesh with UV atlas and LOD.
