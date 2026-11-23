# ZONA industrial checkpoint frame — WIP

Original modular voxel-style concrete/metal overhead checkpoint frame for forest → industrial base route (#128). Designed as a **distinct checkpoint frame**, not a replacement for PR #151 industrial gates or PR #153 approach road.

- Asset ID: `ZONA_ENV_INDUSTRIAL_CHECKPOINT_FRAME_001`, v0.1.0
- Units: meters; Y-up; approximately 4m wide, 3.05m high; central open passage 2.4m
- Reproducible source: `generate.py`; run `python generate.py` with numpy, trimesh, pillow
- Generator writes a textured `industrial_checkpoint_frame.glb`, `atlas.png`, `manifest.json`.
- 27 mesh components / 324 triangles; original palette atlas 256×32, palette-centered UVs; no third-party art.
- Suggested collisions: two concrete posts/feet; not embedded in GLB.
- Status: **geometry WIP / NOT ART APPROVED**. Godot import and Android FPS untested.
- Actual geometry views and locally exported GLB were generated during authoring, but binaries were not uploaded to this PR.
- No gameplay code modified; no merge requested.
