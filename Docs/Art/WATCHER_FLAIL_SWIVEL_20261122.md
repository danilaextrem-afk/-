# Watcher flail swivel v1
Original procedural 3D connector for Watcher ninefold flail head (#70) and chain/grip (#75). Units: metres; Z up. Two perpendicular forged eyelets, barrel, two collars and six rivets. Separate static OBJ mesh, not a physics constraint. Intended as visual connector at chain-to-head interface; placement and pivot must be validated against parent meshes.

Geometry: 1230 vertices, 1230 UV, 2208 triangles, 11 groups. In-memory face index errors 0, degenerate triangles 0. OBJ is authored output of Tools/Art/generate_watcher_flail_swivel.js. Run from output directory with Node.js to regenerate.

Status: geometry WIP. UVs are per-parametric-part and overlap; MTL is viewport-only, not PBR. Blockers: actual chain/head alignment, rigid-body constraint and collision design, PBR maps, DCC render, independent generator run, UE5 import and owner art review. No merge authorized.
