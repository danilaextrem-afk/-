# Warden's ornate chest — 1348 original mesh handoff

**Asset ID:** `RAZLOM-1348-GATE-WARDEN-CHEST-HERO-001`. Pair with `RAZLOM-1348-GATE-WARDEN-KEY-HERO-001` in the investigation at the forge/gates.

**Deliverable:** An actual static OBJ mesh (`SM_GateWardenChest_1348_v1.obj`) with individual timber staves, hand-planed curved lid, oak grain reliefs, iron rib strapping, forged handles, lockplate, engraved ring, rivets and hinges. Six named preview material slots, separate `.mtl` and JSON manifest. 7,112 original mesh vertices / 13,560 triangles; original construction (not downloaded stock geometry).

**Known limitations:** `geometry_wip`. The lid geometry is combined in a single OBJ and **is not yet a separate movable actor**. The lock keyhole is a visual inset, not a Boolean mesh aperture. UV, production PBR maps, final normal/shading split, hinges/pivot animation, low-detail collision proxy, mesh/texture streaming, UE5 import, FPS and 4K art approval remain pending. The preview material colors are **not** a shipped PBR solution.

**Astra 1:** use the stable chest ID in future gameplay inventory/dialogue as a dependency. Do not change art files.

**Astra 2:** import in an isolated editor test stage once PR is reviewed; check exact scale (1 OBJ unit = 1 metre), normal directions, missing UV warnings and material slots. Verify collision and plan lid separation with the art agent before animating. Do not make claims about packaged Windows build or UE5 performance without engine logs.

**Art next:** separate the lid mesh at hinge pivot, finish UV seam layout / PBR, hand-detailed bevel and sculpt variation, high-resolution render and second artistic review against Black Bell v3.
