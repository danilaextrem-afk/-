# Art direction — user's approved reference appearance

The game must aim for the **cinematic 3D pixel/voxel** look of the five previously approved concept artworks for `РАЗЛОМ`: fractured sky, fog, detailed surfaces and careful cyan/violet/red lighting. Those concepts are visual targets, **not in-engine captures** and are not imported here.

## Standalone mini-game focus

- Setting: an anomalous Russian settlement at dusk, damp concrete, abandoned industrial buildings, broken fences, bunker, pine forest. Ordinary life and hostile zones may coexist in the larger universe.
- Silhouette: blocky geometry with deliberate high-quality shapes and restrained polygon counts; do NOT mistake plain cubes for shipping art.
- Palettes: slate charcoal, cold blue fog, desaturated pine/moss, one carefully restrained luminous turquoise rift accent; rare muted magenta anomaly for tension.
- Lighting: dark readable foreground + warm/cold separation; flashlight must reveal usable detail. No eye-straining blackness.
- Detail budget: weathered modular facade atlas, visible window frames and railings, believable ground breakup, trash, foliage, cables, signage and strong props.
- Character style: anatomically recognizable silhouettes translated into deliberate voxel proportions; no random cuboid mannequins as final assets.
- Mobile design: baked/static lights, low-poly clustered meshes, atlas textures, simple colliders, instancing, culling, LOD; measure GPU overdraw particularly on transparent anomaly effects.
- Frame output: Android landscape 16:9, 1280x720 base UI layout; pixelated render may upscale, but text/UI stays crisp.

## Scene-acceptance checklist

1. A **genuine engine screenshot** of the settlement at 720p, not AI concept art.
2. Readable atmospheric landmarks: shelter, village street, warehouse, anomaly.
3. One fully art-directed building facade plus one art-directed humanoid creature (no primitive cube placeholder).
4. Performance capture after Android build; minimum 30 FPS is a **target**, not an unmeasured claim.
5. Visual comparison against the five user-approved concepts, without pretending their assets have been reproduced.
