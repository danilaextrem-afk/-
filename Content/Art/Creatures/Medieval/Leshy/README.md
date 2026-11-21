# Леший — 1348 (v0.1)
Original authored 3D mesh for RAZLOM, not concept art. `SM_Leshy_1348_v01.obj` contains 100 independently named organic mesh parts, 7,158 vertices, 12,722 triangles, UV coordinates and three material groups. Geometry is in **centimeters, Z-up**. The silhouette is an irregular 4.3 m tree-spirit with root feet, hanging moss, long arms, asymmetric antlers and glowing eyes.

**Quality status: geometry_wip.** It is a real, early procedural sculpt, **not yet at the owner-approved Black Bell v3 quality floor**. The 32x32 ASCII PPM maps are test textures only. It needs detailed sculpting, high-res PBR, organic transitions, retopology, rig/skin weights, animation, collision/LOD and actual UE5 import/render before acceptance.

Validation: OBJ face indices reference existing vertex and UV arrays; all material names exist in MTL. `build_source.py` documents geometry generation helpers and construction recipe; it is not a full bit-identical re-exporter. No UE5/FPS/AAA verification claimed.

Next pass: sculpt trunk-to-limb transitions and facial cavities, design an articulated skeleton, bake 2–4K PBR from real high-poly geometry, create LODs, render imported mesh in UE5 and compare visually against Black Bell v3.
