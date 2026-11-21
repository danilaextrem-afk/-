# Wagon wheel 1348 — original geometry WIP

Reproducible source: `node generate.mjs > SM_WagonWheel1348_LOD0.obj`.

150 cm outside diameter; twelve spokes, wooden rim and hub, iron tyre and hub collar. Units centimetres, Z up, axle Y. OBJ has UV0 and named material groups. MTL uses preliminary metallic/roughness scalar parameters; full PBR texture maps are pending.

Validation: generated 1248 vertices, 2448 triangles; no invalid face indices or repeated indices within triangles. No mesh-render or UE5 import verified. This is **not** approved game art. Next: improve joinery, bevels and wood grain, texture bake, LOD1, render from actual mesh, and test UE5.
