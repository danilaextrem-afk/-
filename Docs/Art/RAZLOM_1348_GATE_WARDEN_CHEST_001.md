# Gate Warden chest (1348) — verified independent lid / frame handoff

**Asset ID:** `RAZLOM-1348-GATE-WARDEN-CHEST-HERO-001`. Companion key: `RAZLOM-1348-GATE-WARDEN-KEY-HERO-001`.

## Deliverable source meshes
- `SM_GateWardenChest_1348_v1.obj` — original complete static source: **7,112 vertices / 13,560 tris**.
- `SM_GateWardenChest_Base_1348_v1.obj` — separate chest/frame: **3,968 vertices / 7,540 tris**, origin remains at source location.
- `SM_GateWardenChest_Lid_1348_v1.obj` — independently movable lid: **3,144 vertices / 6,020 tris**, re-centred to hinge pivot.
- `SM_GateWardenChest_Base_1348_v1_UVdraft.obj` — base initial atlas UV: **7,087 `vt` / 2,818 `vn`**.
- `SM_GateWardenChest_Lid_1348_v1_UVdraft.obj` — lid initial atlas UV: **5,432 `vt` / 2,405 `vn`**.
- `COL_GateWardenChest_Base_1348_v1.obj` and `COL_GateWardenChest_Lid_1348_v1.obj` — minimal **8-vertex convex-box** collision proxy source meshes. Their UCX-compatible object names are only a naming convention until confirmed by actual engine import.
- `SM_GateWardenChest_1348_v1.mtl` — six **legacy preview colors**, not production PBR textures.
- `SM_GateWardenChest_1348_v1.json` — versioned manifest, status and explicit pivot.

All OBJ files are original hand-constructed geometry, not internet asset packs or concept images.

## Movement contract for Astra 1 + Astra 2

1. Source meshes use **metres and +Z up**. Unreal uses centimetres: **1.0 OBJ metre → 100 UE centimetres** if the importer does not already convert; confirm actual dimensions before applying scale.
2. Scene/root origin of fixed Base: `[0, 0, 0]` metres.
3. Hinge pivot for the Lid (in original Base reference frame): **`[0, +0.211, +0.381]` metres**, at back of chest.
4. Lid vertices are stored relative to the above hinge pivot. To align the closed state, spawn the lid mesh component at **`[0, 21.1, 38.1]` centimetres** relative to a Base at origin, after correct scale conversion.
5. Candidate opening movement: **rotation -95° about local X** (artistic intent only; adapt to UE's handedness and actor hierarchy after visual test). Do not rotate the entire chest actor.
6. Import and bind each collision proxy separately; do not assume `UCX_` automatically works with OBJ. Proxies intentionally approximate the wooden volume instead of matching every metal fitting.
7. The lock is still a **visual recess**; lock/key interaction and hinge animation are game-engine tasks, not implemented here.

## UV/PBR prototype (not sign-off)
Two draft UV geometry exports preserve each source triangle and include explicit UVs and face normals. Atlas layout is auto-projected per material and is **not a handcrafted unwrap**; seams and stretching need real artistic inspection.

`Tools/Art/build_uv_pbr.py` writes optional TGA BaseColor, Normal and ORM drafts from source OBJ using only Python 3 stdlib:
```sh
python Tools/Art/build_uv_pbr.py Content/Art/HeroProps/SM_GateWardenChest_Base_1348_v1.obj --out build/art/chest-base --size 512
python Tools/Art/build_uv_pbr.py Content/Art/HeroProps/SM_GateWardenChest_Lid_1348_v1.obj --out build/art/chest-lid --size 512
python -m unittest discover -s Tools/Art -p 'test_build_uv_pbr.py' -v
```
**Nine 256×256 TGA texture drafts are already committed** under `Content/Art/HeroProps/PBR_Draft/`: three for the key, three for the chest base, three for the lid. Running the command makes alternative procedural drafts using different noise formulas, not byte-identical replicas of these binaries. See `Docs/Art/PBR_DRAFT_HANDOFF_20261121.md`. Import Normal with sRGB **off**, ORM sRGB **off** (R=AO, G=Roughness, B=Metallic); BaseColor sRGB **on**. The generator's normal map is **procedural height texture**, not high-poly sculpt-baked normal detail. Verify tangent-space orientation and green channel in UE5. Assign the textures explicitly in an actual Unreal material, not from MTL alone.

## Checks / outstanding gates

**Confirmed in the art session via GitHub file reread:** complete source tri counts preserved on split (7,540+6,020=13,560), both UV OBJ files have valid `v/vt/vn` references and UV values in [0,1], collision proxy sources authored. Four generator unit tests passed on a tiny synthetic object in the local Python environment. UV/PBR generator was not run on the complete game assets during this session.

**Not confirmed:** production UV seams/texel density, final 2K/4K hand-authored texture atlases/bakes (the committed 256px maps are drafts), quality floor vs approved Black Bell v3, hinge animation, in-engine collision, Unreal Editor import, Nanite/LODs, FPS, packaged Windows EXE or final art approval. Asset status is `materials_wip` (drafts only), **not `accepted`**.

**Ownership:** Art agent changes `Content/Art/`, `Tools/Art/`, `Docs/Art/`. Astra 2 performs UE5 integration/QA via issue #14. Astra 1 wires gameplay inventory/interaction using asset IDs, without editing art files.
