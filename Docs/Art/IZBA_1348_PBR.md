# RAZLOM 1348 — original Izba PBR texture pack

This is a **draft procedural material pack** for the nine-part modular house kit (PR #92). It matches the seven original OBJ \`usemtl\` IDs: Timber_Log, Timber_Endgrain, Timber_Plank, Timber_Trim, Chinking_Dark, Iron_Forged, Shingle_Wood. Does not include any third-party images or stock textures.

Run from repository root with Python 3.12+:

\`\`\`bash
python -m pip install numpy Pillow
python Tools/Art/generate_izba_pbr.py --size 1024
python Tools/Art/validate_izba_pbr.py --size 1024
\`\`\`

This writes **21 PNGs**, three per material: BaseColor (sRGB ON); Normal_DX (normal compression, sRGB OFF); ORM (sRGB OFF, R=procedural cavity AO, G=roughness, B=metallic). Normal Y convention is **DirectX/Unreal**. The normal maps are generated from synthetic height fields using finite differences. ORM AO is a local cavity estimate, **not geometry-baked ambient occlusion**.

Workflow \`.github/workflows/izba-1348-pbr.yml\` generates maps on push/PR and uploads a downloadable Actions artifact. A manual dispatch supports 512, 1024 or 2048 resolution. This avoids permanently committing 21 large generated binary maps, while keeping generation repeatable.

**Art status:** draft procedural channels, not AAA-final materials. Textures still need unique high-poly normal/AO baking, better art-directed wear, unique atlas seams, full UE5 import and material instances, visual verification under calibrated light, and real Windows/4K performance measurements. Wall/roof UV directions must be checked in-engine.

**UE5 import guidance:** match generated PBR textures by ID to the existing \`usemtl\` slots, set normal compression \`TC_Normalmap\`, disable sRGB on normal and ORM; select mask compression for ORM, plug R into AO, G into Roughness and B into Metallic. Do not assign ORM as a linear RGB color map.
