# RAZLOM 1348: Gate Warden forged key — actual asset handoff

**ID**: `RAZLOM-1348-GATE-WARDEN-KEY-HERO-001`.
**Artist-owned source**: `Content/Art/HeroProps/SM_GateWardenKey_1348_v1.obj`.
**Geometry**: original wrought-iron hero key (forged bow, hammered rosettes, central sigil, chased shaft, ancient bit); **9,032 vertices / 17,272 triangles**. Four material regions: bronze, iron, oxide, steel.

**First-pass render asset**: `SM_GateWardenKey_1348_v1_UVdraft.obj`, same 9,032 source vertices and 17,272 triangles, 9,032 projected UVs. Important: this file has **no explicit vertex normals**. UE must build normals and tangents; facet quality / UV seams must be checked.

**Real committed PBR draft maps** (256×256 binary TGA):
- `PBR_Draft/T_GateWardenKey_1348_v1_BaseColor.tga` (sRGB ON)
- `PBR_Draft/T_GateWardenKey_1348_v1_Normal.tga` (sRGB OFF, procedural bump, not sculpt bake)
- `PBR_Draft/T_GateWardenKey_1348_v1_ORM.tga` (linear; R AO, G Roughness, B Metallic)
- `SM_GateWardenKey_1348_v1_PBRdraft.mtl` binds **BaseColor only** for generic OBJ viewers; UE5 requires an actual material graph for Normal/ORM.

**Checks actually done:** Original OBJ re-read from GitHub; 0 invalid face indices, 0 zero-area triangles, 0 non-finite vertices. The exported UVdraft OBJ re-read: all indices valid, UV values in [0,1] and triangle count unchanged. Three TGA blobs re-read and sizes checked. Four UV/PBR generator unit tests passed locally on a small synthetic model.

**Not done:** UE5 import, real in-engine render/screenshot, artistic approval, actual 4K PBR sculpt bake, topological smoothing, collision, Nanite/LOD, Windows packaged EXE, FPS. `materials_wip` only; model should not be called production-ready or meet owner's Black Bell v3 quality floor yet.

**Coordination:** Astra №1 registers the stable ID in future Act II story data when needed; Astra №2 checks size (source OBJ metres vs UE centimetres), materials and shading via issue #14. Do not change another agent's files without PR review.

For full atlas/tangent caveats see `Docs/Art/PBR_DRAFT_HANDOFF_20261121.md`.
