# Watcher cuirass v1 — integration handoff

Original visible 3D geometry for Ninth Entrance Watcher, Act I silhouette. Four overlapping chest lamellae, forged rivets, side straps, raised relief and two rear mounts for dorsal reliquary. Geometry is static OBJ in metres, Z up, front +Y. Approximate chest centre Z=1.5m, intended for 2.19m Watcher blockout. This is an independent armor accessory, not rigged.

Mesh: 288 vertices, 440 triangles, 34 named parts. OBJ includes simple planar UV coordinates, **overlapping/unstable islands** (not a production UV unwrap); draft MTL is not PBR. Reproduce with `node Tools/Art/generate_watcher_cuirass_v1.js > Content/Art/Characters/Watcher/SM_Watcher_Cuirass_v1.obj`. No third-party assets.

Checks: generated triangles use in-range indices and finite vertex coordinates; inspect winding and intersecting shells in Blender/UE. No Blender/UE5 import, material lighting, collisions, attachment, animation or quality approval performed. Integration blockers: verify torso clearance with existing body and gorget, adjust shoulder placement, UV unwrap, PBR maps, rig/socket binding and UE5 import scale (100 cm/m). No gameplay files modified.
