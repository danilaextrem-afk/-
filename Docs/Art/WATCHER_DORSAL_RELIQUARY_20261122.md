# Watcher dorsal reliquary — geometry draft v1 (2026-11-22)

**Original mesh:** `Content/Art/Characters/Watcher/SM_Watcher_DorsalReliquary_v1.obj`

**Generator:** `python Tools/Art/generate_watcher_spine.py` (Python standard library only).

**Narrative/scene purpose:** rigid ritual iron spine and suspended charms on the *back* of the Ninth Entrance Watcher, visible during retreat / turn-around shots in Act I. This is not a second character or a duplicate of Astra's Подъездник.

**Source specification:** 280 vertices, 420 triangles, 35 named disconnected rigid components. Seven articulated rib bands, 14 rivets, six spine links, iron reliquary casing, two anchors, three talismans. Metres, Z-up; intended back side at negative Y. No bones, UVs, materials or collision yet. Provisional attachment space only: final body fit must be checked in Blender/UE.

**Checks performed:** generated source locally with Python; parsed 280 vertices / 420 triangles / 35 groups; validated every face index is within vertex range. Rendered a local preview from that OBJ with matplotlib Poly3DCollection, *not* an Unreal screenshot. GitHub import/engine cook not tested.

**Next:** check body interpenetration against actual Watcher body mesh; add proper UV islands, PBR material and per-rib attachment or skinning; test UE5 import, orientation and scale. This is blockout geometry, **not** AAA/UE5-ready.
