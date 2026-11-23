# Залесье — UV atlas binding fix (local validated artifact)

Issue #128 / #141. Reworked existing 48 m street hero slice, **not a new environment kit**. Local generator v0.2.0 binds the original 128×128 pixel atlas as an embedded glTF texture using UV coordinates on every geometry part (previous version only used vertex colors and did not bind the atlas).

Local QA: GLB exported and reloaded; 174 geometries / 2088 triangles; all meshes have UVs; vertices finite; nondegenerate triangles. Two orthographic geometry views generated from GLB. Generator uses Python, trimesh, numpy, Pillow; no third-party asset packs.

**BLOCKER:** GitHub text connector cannot access files generated in the local runtime. The .glb, generator, atlas, and two views have NOT been committed to this branch. They are preserved as run artifacts, and must not be treated as available in GitHub. No Godot 4 runtime or Android hardware validation performed. This is not an accepted art pass, and does not satisfy issue #141's required real Godot screenshots.

Status: ART REWORK / NOT APPROVED. Do not merge without integration and owner review.
