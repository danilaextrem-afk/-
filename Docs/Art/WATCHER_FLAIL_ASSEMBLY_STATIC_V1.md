# Watcher ninefold flail — static assembly v1

**Purpose:** single OBJ for layout and visual review of the Watcher's weapon in the Ninth Entrance scene. This is a static *preview assembly*, not a skeletal or physics weapon.

## Source geometry

- Head: `Content/Art/Characters/WatcherNinefoldFlailHead/SM_Watcher_NinefoldFlailHead_v1.obj` (original Y-up converted to Z-up).
- Chain and handle: `Content/Art/Characters/WatcherFlailChain/SM_Watcher_FlailChain_v1.obj` (Z-up).
- Swivel: `Content/Art/Characters/WatcherFlailSwivel/SM_Watcher_Flail_Swivel_v1.obj` (Z-up).

Output: `Content/Art/Characters/WatcherFlailAssembly/SM_Watcher_Flail_Assembly_StaticPreview_v1.obj`.

Assembly offsets in metres: chain z=0; swivel z=1.55; head z=1.78. **These offsets are provisional visual placement, not measured mechanical fit**. UV coordinates are preserved with global face-index offsets; UVs are not production-quality.

## Reproduction and QA

From repository root:

```sh
node Tools/Art/build_watcher_flail_assembly.js . /tmp/SM_Watcher_Flail_Assembly_StaticPreview_v1.obj
node Tools/Art/test_watcher_flail_assembly.js
```

Current committed combined OBJ: 4,655 vertices, 4,655 UV coordinates, 8,958 triangles. Assembly script regression tests cover indexing, Z-up conversion and invalid inputs. It does not create a rig or baked material maps.

## Integration blockers

1. Confirm the head and swivel rings align in an actual DCC/UE5 viewport and don't visibly intersect.
2. Replace static links with a skeletal or physics-chain solution; author sockets for hand grip and flail head.
3. UV unwrap, bake PBR, generate collision and LODs; validate UE5 import, animation and performance.
4. Obtain a genuine source-geometry render and owner's art approval. **Do not present AI concept art as an engine screenshot.**

No UE5-ready/AAA claim. No merge requested.
