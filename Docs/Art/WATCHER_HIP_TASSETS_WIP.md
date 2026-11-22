# Watcher segmented hip tassets — WIP

**Scene:** The Watcher character in Ninth Entrance. Eight curved overlapping-looking hip plates encircle the waist beneath the torso armor, with belt, 16 studs, and a front stamped seal. Original procedural geometry; no external pack.

**Files:** `Content/Art/Characters/WatcherHipTassets/SM_Watcher_HipTassets_v1.obj`, matching MTL, generator `Tools/Art/generate_watcher_hip_tassets.js`.

**Reproduce:** `node Tools/Art/generate_watcher_hip_tassets.js /tmp/SM_Watcher_HipTassets_v1.obj` from repository root. OBJ uses metres, Y-up. UVs are a per-vertex draft, NOT a production atlas. MTL is a simple material placeholder, NOT PBR.

**Verified:** Re-read committed OBJ via GitHub: 1,120 vertices, 1,120 UV entries, 1,288 triangles, zero out-of-range indices, zero zero-area triangles under 1e-10 threshold. Bounding box x,z ±0.38 m, y -0.172 to 0.225 m.

**Integration blockers:** Need real DCC render and fitting against Watcher torso, belt, cloak, and leg animation. Segments are static subgroups, not rigged. Need production UV unwrap, baked PBR, collision, LOD, UE5 import and visual approval. Not UE5-ready or AAA. No merge.
