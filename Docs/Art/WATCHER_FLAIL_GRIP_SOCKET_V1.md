# Watcher forged grip socket cuff v1

**Actual geometry:** `Content/Art/Characters/WatcherFlailGripSocket/SM_Watcher_Flail_GripSocket_v1.obj` (Z-up, metres). 2,448 vertices / UV entries, 4,512 triangles, 21 named groups. Includes a forged wrist/handle cuff, beaded rims, eye loop and 16 iron rivets. Original procedural mesh; no asset packs.

**Scene role:** additional hand-end attachment for the Watcher's ninefold flail, to support the weapon silhouette and eventual hand-grip socket.

**Reproduction:** `node Tools/Art/generate_watcher_flail_grip_socket.js Content/Art/Characters/WatcherFlailGripSocket/SM_Watcher_Flail_GripSocket_v1.obj`.

**Status:** WIP mesh and simple MTL only. UVs are procedural cylindrical coordinates with overlap, not final unwrap. No baked PBR. Does not include hand socket transform, skinning, collision, LOD or physics.

**Integration blocker:** Astra #1 must establish exact handle-end coordinate and attachment orientation against the existing flail assembly, and test in UE5; cuff is currently standalone, not inserted into the static assembly. Visual mesh preview not yet rendered. No AAA or UE5-ready claim.
