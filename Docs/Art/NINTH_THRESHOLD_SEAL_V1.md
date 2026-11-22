# Ninth Entrance — ninefold ritual threshold seal (geometry WIP)

Original authored environment mesh for the Ninth Entrance floor in the modern Moscow act. Intended as a visible, story-significant occult threshold marking; placement, lore wording and size need Astra approval before canonical use.

**Geometry:** 5,073 vertices, 5,073 UV coordinates, 9,320 triangles, 9 named geometry groups. Mesh includes concentric stepped basalt and bronze rings, nine separated outer bronze arcs, nine radial spokes, nine distinct engraved glyph clusters, a central nine-point star, nine iron anchor bosses and four branching fracture fills with stone-edge highlights. OBJ in metres, Z-up, approximately 1.74m diameter. All geometry is original procedural work; no asset pack.

**Source:** `Tools/Art/generate_ninth_threshold_seal.js`; reproduce from repository root with:
```bash
node Tools/Art/generate_ninth_threshold_seal.js Content/Art/Environment/NinthThresholdSeal/SM_NinthThresholdSeal_v1.obj
```
**Preview:** `Preview_SourceGeometry_Top.svg` is an orthographic top projection made by reading the committed OBJ triangle positions and drawing the upward-facing polygons. It is **not** an AI concept, Blender render, UE5 screenshot or lighting/material validation.

**Material status:** MTL color slots are draft viewport colors, not PBR. UVs are deterministic planar projection, not an atlas with production texel density.

**QA performed:** independent GitHub reread confirmed OBJ vertex/UV/triangle counts and source-derived SVG generation (2,308 upward-facing triangles). Actual Node runtime generator execution, normal orientation, manifoldness, UV overlap, PBR maps, collision, UE5 import, and in-scene visibility remain unverified.

**Blocking integration:** Astra must confirm the ninth entrance floor footprint, whether a 1.74m occult symbol is canonically appropriate, collision policy (probably non-colliding overlay), and actual lighting. Visual acceptance required; the user's previous art quality feedback means this must remain WIP until reviewed.

**Next:** improve raised relief bevels, hand-authored roughness/normal/AO and real oblique 3D render from source mesh.
