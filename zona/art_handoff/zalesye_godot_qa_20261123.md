# Zalesye Godot 4 QA handoff (NOT ART APPROVED)

Issue #128 / blocker #141. This branch records a reproducible integration specification for the existing 48m Zalesye slice, **not** a successful engine test.

The local deliverable archive contains: project.godot, zalesye_qa.tscn, assets/zalesye_hero_slice.glb, assets/generate.py, assets/atlas.png, assets/manifest.json, qa.json.

GLB inspection (trimesh reload): 174 meshes, 2088 triangles; bounds x -11.325..10.825, y -0.299..6.350, z -48.5..0.5 metres. The Godot Compatibility renderer scene uses a 75-degree camera, ambient/directional light and provisional floor/edge collision boxes. These collision boxes are **not** a final collision mesh. Geometry/texture quality remains rejected by #141.

The archive is an attached automation artifact, **not committed to this branch** because the GitHub connector only supports UTF-8 text uploads. Do not merge or claim a working Godot scene from this README alone.

Required acceptance: import GLB into Godot 4; verify scene parser and material texture in-engine; capture two untouched engine screenshots from distinct angles; compare with approved reference; inspect collision and benchmark on a physical Android device. None performed here.

No changes to paused UE5 project, gameplay or other developers' files.