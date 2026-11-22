# Ninth Entrance Watcher — Ritual Censer v1

**Source:** `Content/Art/Characters/Watcher/SM_Watcher_RitualCenser_v1.obj`  
**Generator:** `Tools/Art/generate_watcher_censer.py`  
**Status:** original static geometry blockout; **NOT UE5-ready**.

## In-scene use
A handheld ritual censer for the Watcher in the ninth entrance. Nine raised ribs echo the nine floors/ritual motif. The upper vent vanes are separate geometry. Intended for a hand-held prop socket after the Watcher is rigged.

## Geometry
1684 vertices, 3002 triangular faces. Model is authored in metres with Z-up; approximate extent 0.43 m wide and 0.66 m high including suspension hoop. Material groups: Iron_Cage, Bronze_Rim, Nine_Forged_Ribs, Vent_Vanes, Suspension_Neck, Suspension_Hoop, Rivets, Inner_Clapper.

## Reproduce and validate
Run `python Tools/Art/generate_watcher_censer.py` from the repository root. The script deterministically rewrites the OBJ. Basic geometry check on generated OBJ: 1684 vertices / 3002 triangles, all face indices in range, no duplicate triangle indices. The file was also read back from GitHub and checked.

## Integration blockers
No authored UVs, no texture maps or production PBR material, no UE5 import/cook test, no dedicated collision, no LOD, no hand socket alignment, no animation. Some components may be open meshes; no manifold claim. Geometry is intentionally a blockout, not AAA hero quality. A preview may be rendered from OBJ externally; do not mistake it for an in-engine screenshot.
