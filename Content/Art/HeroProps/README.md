# Black Bell 1348 — high-detail art development (Windows PC / UE5)

**Asset:** \`SM_BlackBell_Hero_v2.obj\` (author-generated 3D mesh, OBJ + UV + normals)  
**Materials:** \`BlackBell_Hero_v2.mtl\` and 12 authored PNG PBR maps (bronze, iron, oak, each with albedo, tangent-space normal, roughness, metallic).  
**Rebuild:** \`python Tools/generate_blackbell_hero.py .\` from repository root. The GitHub Action \`build-blackbell-hero.yml\` rebuilds and checks these files on generator updates.

## Visual and gameplay design

The artefact is the medieval Black Bell from «РАЗЛОМ: ЧЁРНЫЙ ЗВОН». The geometry is not a stack of cubes: separate hollow bell casting, lip, ornamental gilded rings, real raised rune strokes, seven crown arch motifs, enamel bosses, internal clapper, dark forged hanging hooks, textured chamfered oak crossbeam, hammered straps and mechanical rivets.

Designed for a dark supernatural XIII–XIV-century setting: antique blackened bronze, local turquoise patina, rubbed gilded relief and splintered wood. Mesh is modeled in **centimeters, +Z up**, for **Unreal Engine 5 Windows PC**. Original source, not purchased asset packs.

## Known issues / actual quality state

- The ZIP preview and mesh have **not** been imported/rendered or performance-tested on a Windows Unreal Engine installation. A technical software preview is not proof of AAA quality.
- PBR maps are **procedural 1024×1024**, not high-resolution hand-baked microgeometry or scanned materials. A proper UE5 material graph still needs to connect each map at appropriate roughness, tint, normal strength and metallic scale.
- The mesh has multiple open/disconnected hard-surface elements. It is **not watertight**; that may be appropriate for detachable metal and wooden components but requires actual collision/Nanite verification.
- Needs artistic iteration for authentic aging, damage, more distinctive nonrepeated relief, material calibration, cinematic lighting and close-up acceptance. UV islands and texel density must be checked in-engine.
- No production LODs, collision meshes, finalized texture atlases, sound/animation setup or measured 4K FPS yet.

## Quality gate before calling it an AAA asset

1. Run \`python Tools/validate_blackbell_hero.py .\` and require **zero degenerate triangles**, valid indices/UVs/normals and all maps readable.
2. Import OBJ, MTL and textures into a *test* Unreal Engine 5 PC project. Build physically based material instances and check face orientation, axis, tangents, no black seams, no missing maps.
3. Compare a real engine screenshot against the approved visual reference under controlled light. No PR or README claim of AAA approval before this step.
4. Evaluate triangle and material slot cost and Nanite support on target PC hardware; add simplified collision as appropriate.
5. Only after artistic approval replace low-detail scene props; preserve the original v1 source and version history.

**Important:** This is a major technical/detail pass, not the game's finished visual standard.