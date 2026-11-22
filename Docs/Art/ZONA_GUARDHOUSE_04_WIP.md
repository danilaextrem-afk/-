# ЗОНА — Сторожка КПП «04» · original 3D asset WIP

- Issue: #128. Purpose: recognisable abandoned checkpoint at approach to industrial base in forest, reusable modular 2026-era kit.
- Real output: **GLTF 2.0 with embedded buffer**, full 3D geometry, Y-up metres, mesh nodes per material, fixed-window frame, movable door leaf grouping.
- Source: `Tools/Art/build_zona_guardhouse.js`, run `node ... <output_directory>`; deterministic, no third-party asset packs.
- Size: approximately 6.85m wide, 5.28m deep, highest antenna 4.57m; wall geometry contains actual window and door apertures.
- Visible details: corrugated roof, recessed windows with metal mesh and boards, frame/handle/vent on door, numbered plaque, concrete steps, power box, conduit, side ventilation, downspout, roof antenna, chipped plaster and rust.
- Measured procedural output: **273 box components**, **3276 triangles**, **6552 quad-unique vertices**, **20 material slots**. This is a **draft not optimised** — many material slots and double-overlap surfaces need batching, winding review, occlusion pruning before mobile.
- Materials: GLTF metallic/roughness base-color parameters, a reproducible embedded 64×64 PNG pixel-grunge color texture is present on every material; no full PBR normal/ORM yet. UV0 present on every face but no production texture atlas (blocker). Treat it as visually authored pixel/voxel draft, not approved production quality.
- Source preview: Two SVG views (front-right and front-left) are direct projected mesh faces with flat shading (not AI generated and NOT a Godot screenshot).
- Important warning: some overlay meshes are physically intersecting on purpose for damage/graffiti; runtime collision should be separate simplified shapes. Collision source to be authored; none claimed.
- Import QA outstanding: must run `node` generator independently on CI, Godot 4 asset import and material inspection, adjust winding/pivots for door, collision, texture atlas, and profile on Android. No FPS claim.
- No merge; design remains open for owner review; no changes to other contributors' assets.
