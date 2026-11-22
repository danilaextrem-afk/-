# RAZLOM: ZONA — Anomaly Beacon v1
Original modular voxel-style 3D scene prop for Godot 4 Android. Scene: abandoned depot / anomalous field boundary. Height 2.23 m, footprint 0.84 m. Source geometry: 30 closed cuboids, 240 vertices, 360 triangles, 720 UV records, 5 material slots.
Rebuild: `python Tools/Art/build_zona_anomaly_beacon.py` (Python stdlib only).
Source: OBJ/MTL with UV0; procedural generator is editable. Palette is preview-only, not PBR. No rig or animations. Suggested gameplay: emissive cyan warning core and interaction prompt; no behavior is implemented.
Status: real geometry source committed; not imported or rendered in Godot; no on-device profiling; no final baked PBR maps. Integration blockers: verify Godot importer, normals/triangulation, collision mesh, mobile material budget and emissive effect. Do not call this engine screenshot or production-ready.
