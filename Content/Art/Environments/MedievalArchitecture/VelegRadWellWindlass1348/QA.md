# QA — VelegRad well windlass v0.1.0

Original procedural OBJ mesh with UV per vertex, 4 preview MTL materials, LOD0 816 vertices / 1384 triangles; LOD1 368 vertices / 584 triangles.

Local Python reference geometry was generated and loaded with trimesh 4.11.1: 1384 and 584 triangles respectively. The committed Node.js generator has not been independently executed after upload. No actual mesh render, PBR maps, collision, UE5 import, AAA fidelity or FPS check. The bucket is a solid placeholder cylinder, not hollow; stone curb is a coarse assembly, not final sculpt. Status geometry_wip. No ready-made asset packs.

Run: `node generate.cjs` in this directory, compare generated OBJ/manifest, inspect in Blender, validate collision and PBR before promotion.
