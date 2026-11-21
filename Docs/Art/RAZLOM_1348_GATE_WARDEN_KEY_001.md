# Gate Warden's Key / 1348 — art handoff

Asset ID: `RAZLOM-1348-GATE-WARDEN-KEY-HERO-001`.

This is an **actual original 3D mesh** of an ornate key in a separate art branch, not a screenshot or AI concept image. The model depicts an eight-riveted quatrefoil bow, central sigil, forged shaft, engraved collars, chased serpentine motifs, cut-bit teeth and antique metal inlay. The proportions are deliberately a little monumental for a clue/inspect close-up.

**Files**: `Content/Art/HeroProps/SM_GateWardenKey_1348_v1.obj`, corresponding `.mtl`, and JSON manifest. OBJ: 9,032 vertices / 17,416 faces (triangles); 0.12345 m wide × 0.0228 m thick × 0.25745 m tall. Source uses +Z up and SI metres. In Unreal, confirm conversion to 100 cm per metre; do not blindly import at scale 1.

**Important limitations**: The MTL is only preview material slots; no UV unwrap, texture-backed PBR or import proof yet. `geometry_wip`, **not accepted**, no real Blender/UE5 screenshot. Sculpt proportions, appearance, topology, mesh normals, performance and close-up photorealism still require actual visual review. Count this as an incremental asset prototype, not as meeting the owner's Black Bell v3 artistic minimum.

**For Astra 1**: Use asset ID as optional interaction/inventory item after the watchman-key mission, no changes required to gameplay code by the artist.

**For Astra 2**: Check triangulation, shading, face orientation, unit scale, duplicate/intersecting teeth, collision simplification and import warnings. Do not report a successful UE5 import without actual editor logs. Request final PBR and revised geometry from art before production sign-off.

**Next art pass**: audited face winding / degenerate checks, first real UV/PBR set, improved bevel and scratches, close-up rendered approval and low-cost UE5 collision proxy.
