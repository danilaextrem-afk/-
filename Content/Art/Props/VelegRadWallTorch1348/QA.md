# QA — Wall torch v0.1

- In-memory procedural geometry validated: LOD0 740 triangles, LOD1 292 triangles.
- OBJ vertex, UV and normal indices are in range; all generated coordinates finite: true.
- UV per face and normals present; OBJ exported from real geometry.
- Limitation: independent Node.js reproduction, actual mesh render, topology manifold check, physical PBR maps, collision, and UE5 import NOT tested.
- Artistic benchmark PR #5 / PR #7 not met or verified; not final-quality.
- Interactive torch would require attachment sockets, lighting, flame VFX and collision.
