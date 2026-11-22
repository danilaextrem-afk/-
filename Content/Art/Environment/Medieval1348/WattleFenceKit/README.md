# RAZLOM — 1348 village wattle fencing and hinged gate kit

Author-made procedural geometry for historically inspired **alternative mythological Rus, 1348**. **WIP, not AAA final**. Built for a lived-in VelegRad village: curved woven withies, carved vertical stakes, twine lashings, corner and gate posts, hinged 1.08 m gate leaf with diagonal brace and iron latch.

Four modular mesh families (each with LOD0 and LOD1):

- **Fence2m** 10064 / 1155 triangles
- **CornerPost** 1022 / 441 triangles
- **EndPost** 974 / 445 triangles
- **GateLeaf1m** 3412 / 960 triangles

Geometry includes OBJ triangular faces with normals, real UV indices, physical meter coordinates (Z up), named subparts, separate material slots. No stock packs or downloaded assets. Assembly sample is in `assembly_fence_example.json`.

Import into **Unreal Engine 5** as static meshes, convert from **meters to centimeters** by checking the importer. Add simple or convex collision per section. Do not use the detailed weave mesh for collision. Gate pivots must be realigned with hinge at x=0, and Blueprints authored to swing it; the gate is currently **not animated**.

Generate source files with Node.js 18+: `node Tools/Art/generate_wattle_fence_1348.js`. Test with `node Tools/Art/validate_wattle_fence_1348.js`.

Outstanding: realistic sculpted bark, high-resolution texture maps, UV atlas/lightmap UVs, testable gameplay interactions, UE5 render and 4K FPS validation. The MTL colors / PBR parameters are draft only.
