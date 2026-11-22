# ZONA_ENV_INDUSTRIAL_GATE_001 v0.1.0

Environment kit #128: industrial checkpoint gateway between forest and depot. Separate from legacy UE5 game.

Locally generated original GLB with 468 triangles, 39 cuboid components, UV coordinates in [0,1], 128x128 original pixel atlas, one glTF PBR material (metallic 0.05, roughness 0.88). Geometry is Y-up, meter units, approximately 6.7m wide x 3.3m high x 0.8m deep. Pivot ground center. Gate center remains open. Collision specification: box colliders on two posts and overhead header; collision is NOT embedded in GLB.

QA performed locally: glTF export, trimesh GLB roundtrip, triangle count, winding consistency, finite UV and coordinates, no zero-area faces. Two orthographic projected mesh views were generated locally. Godot import and Android FPS NOT tested. This PR contains only documentation; actual GLB, generator, atlas, and preview PNGs are local conversation artifacts and NOT committed. Do not merge as delivered environment kit.

Asset status: GEOMETRY_WIP / NOT APPROVED. License: original CC0. Related #127, #128, #141. Prior forest transition PR #149, facade #147, bunker vent #142 checked to avoid duplication.
