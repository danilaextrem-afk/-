# Watcher articulated knee guards v1

Original procedural left/right kneecap armour for the Watcher in the Ninth Entrance scene. Mesh includes main curved shells, upper/lower flexible lames, raised circular rim, central boss, side hinge rivets and engraved beads. Separate named OBJ groups are **not** rigged bones.

**Files:** `Content/Art/Characters/WatcherKneeGuards/SM_Watcher_KneeGuards_v1.obj`, matching MTL, `Tools/Art/generate_watcher_knee_guards_v1.js`.

**Rebuild:** `node Tools/Art/generate_watcher_knee_guards_v1.js /tmp/SM_Watcher_KneeGuards_v1.obj`.

**Measured in authoring check:** 3,770 vertices and UV coordinates, 6,416 triangles, 24 named groups, zero degenerate triangles (cross-product threshold 1e-9). Z-up, metres. The UVs are parametric drafts, not non-overlapping baked texture islands. MTL is only a draft colour, not PBR.

**Readiness:** WIP visual geometry only. Fit to existing Watcher body/boots/tassets not checked; no animation, PBR, collisions, LODs, or UE5 import. **Integration blocker:** confirm leg/knee proportions and bone pivots with Astra before skinning and authoring mechanical hinge articulation. No AAA/UE5-ready claim.
