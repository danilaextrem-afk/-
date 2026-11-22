# RAZLOM Act II: procedural PBR texture draft, art/integration handoff

Updated 2026-11-21. Branch `art/forged-key-hero-prop-20261121`, PR #13 (draft). **Actual source assets are committed**. UV/textures are first-pass materials, not release/AAA approved.

## Directly committed material bundle

Location: `Content/Art/HeroProps/PBR_Draft/`.

Each of the following **nine** maps is a true binary uncompressed **256×256 24-bit TGA** (196,626 bytes, including 18-byte header). Names:

| Asset | BaseColor (sRGB on) | Normal (sRGB off) | ORM (sRGB off) |
|---|---|---|---|
| GateWardenKey_1348_v1 | `T_GateWardenKey_1348_v1_BaseColor.tga` | `T_GateWardenKey_1348_v1_Normal.tga` | `T_GateWardenKey_1348_v1_ORM.tga` |
| GateWardenChest_Base_1348_v1 | `T_GateWardenChest_Base_1348_v1_BaseColor.tga` | `T_GateWardenChest_Base_1348_v1_Normal.tga` | `T_GateWardenChest_Base_1348_v1_ORM.tga` |
| GateWardenChest_Lid_1348_v1 | `T_GateWardenChest_Lid_1348_v1_BaseColor.tga` | `T_GateWardenChest_Lid_1348_v1_Normal.tga` | `T_GateWardenChest_Lid_1348_v1_ORM.tga` |

**PBR packing:** BaseColor = sRGB; normal = tangent-space-like simulated procedural surface, **NOT baked**; ORM: R=AO, G=roughness, B=metallic, linear. No opacity/emission. Authored preview palette uses dark iron, peened steel, worn bronze, rust/oxide, oak/old wood. **Material import in UE5 has not been tested.**

`material_bundle_v1.json` is the machine-readable binding contract. Shared atlas tiling is per material: Key=2×2, Chest Base=3×2, Chest Lid=2×2. Colour data is packed into each tile so each object's named material uses the same atlas; UV coordinates choose their tile.

The new UVdraft meshes and their MTL files reference the *actual* BaseColor TGA. The two other TGA maps **must be wired manually** in UE5 (or by Astra 2's importer). MTL is not a UE5 PBR material graph. On import, verify the green-channel orientation and tangent basis, roughness (not gloss), and whether normal texture sampling uses the normal compression mode.

## Geometry and hinged lid

- Key: `SM_GateWardenKey_1348_v1_UVdraft.obj` — 9,032 vertices, 9,032 `vt`, **no explicit `vn`** (UE import must compute/check normals), 17,272 triangles.
- Base: `SM_GateWardenChest_Base_1348_v1_UVdraft.obj` — 3,968 `v`, 7,087 `vt`, 2,818 `vn`, 7,540 triangles.
- Lid: `SM_GateWardenChest_Lid_1348_v1_UVdraft.obj` — 3,144 `v`, 5,432 `vt`, 2,405 `vn`, 6,020 triangles.
- Lid local origin is pivot; to close the chest, parent offset **`[0, +0.211, +0.381]` metres** relative to Base; probable open rotation **-95° about local X**, to be verified in UE5.
- Simple collision hull proxies: `COL_GateWardenChest_Base_1348_v1.obj` and `COL_GateWardenChest_Lid_1348_v1.obj`; separate **convex 8-vertex boxes**. Must be bound as UE collision explicitly; `UCX_` naming alone for OBJ does not prove automatic binding.

## Verification performed (specific evidence, no inflated claims)

1. All 3 source/split meshes were re-read from GitHub. Key: 9,032 vertices / 17,272 triangles; chest split: 7,540+6,020 triangles, exact original count 13,560; no degenerate source triangles or invalid indices.
2. UVdraft files re-read: all `f` indices valid and UV within [0,1], key/base/lid counts as above. UV layouts are projected atlas **drafts**, not hand-unwrapped final UV.
3. Six chest TGAs were read back via GitHub blob access and verified: uncompressed TGA type 2, 256×256, 24-bit, top-left row origin, decoded length 196,626 bytes each.
4. Three key TGAs were verified for accessible blob SHA/size. All nine files exist as binary Git blobs in this branch.
5. `Tools/Art/test_build_uv_pbr.py`: four synthetic-geometry Python tests passed locally (valid OBJ→UV/TGA+ORM; invalid image size, invalid face index, missing material failures).

`Tools/Art/build_uv_pbr.py` can create **new** independent procedural TGA drafts (64–4096px); its formulas are not bit-identical to the already committed nine 256px preview TGAs, and the source mesh UV projection can differ from the current manually exported draft. Do **not** claim reproducible byte-for-byte regeneration of the committed atlas files. A consolidated deterministic baker still needs a future art pass.

## Acceptance required before gameplay shipping
- Real Unreal Engine 5 Editor import and true material node bindings, inspect actual neutral/night scene renders in Blender and UE, correct tangent-space normal green channel, hand-rework UV seams and per-asset texel density.
- Retopology or sculpt high-poly where necessary; better wrought metal, timber shaping and unique wear; physically credible 2K/4K source art. **256px draft is not 4K.**
- Simple/complex collision policies, proper lid pivot motion/camera inspection, LOD/Nanite decision and profiling; owner visual acceptance vs Black Bell v3.
- Current acceptance: `materials_wip`, **NOT `accepted`**, and not a UE5 screenshot/render claim.

## Team responsibilities
- Art (ChatGPT): geometry, UV, texture iterations, material metadata, no other agents' gameplay/CI paths.
- Astra №1 PR #11: Act I gameplay; future Act II inventory references only by asset IDs.
- Astra №2 PR #12: coordinate true UE5 build/asset checks via issue #14; review not assume source MTL works.
