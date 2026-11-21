# B2 Старый Хозяин Чернолесья — actual mesh source / QA

Original procedural OBJ created in this branch. 1496 v + 1496 vt, 2012 triangle faces, 38 geometry groups. Z-up, centimeters. The mesh is a connected collection of 38 separate closed/open tubular forms, NOT a rigged skinned character. Source-of-truth editable geometry is OBJ itself; UVs are per-vertex cylindrical strips. Use the Blender helper below for preview and mesh checks; no third-party asset pack.

The 32×32 PPM maps are **low-resolution draft source data**, not baked production maps. PPM is not guaranteed UE5 importable; convert to 16-bit PNG/TGA and properly bake from a higher-detail sculpt before acceptance. The normal map is illustrative, not mesh-baked. MTL only references albedo and normal; roughness/metallic/AO need explicit UE5 material hookup.

The visual quality of this blockout has NOT been demonstrated equivalent to the owner's accepted Black Bell v3. No real render, UE5 import, collision, rigging, animation, FPS or 4K quality evidence exists yet.

Next: sculpt non-repetitive wood grain and broken root anatomy, face/heart core, retopologize for deformation, rig phase states, bake real 2K/4K PBR, inspect in Blender and UE5, add collision and LOD/Nanite after profiling.
