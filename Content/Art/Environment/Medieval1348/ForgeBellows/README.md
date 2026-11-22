# Original 1348 Forge Bellows — geometry WIP

Real triangulated meshes for the Veligrad smithy in RAZLOM: BLACK BELL. This is not a final AAA art asset.

Contents: LOD0/LOD1 OBJ, MTL draft palette, Node.js procedural generator. Geometry has UVs, explicit normals and eight material IDs. No purchased assets.

Construction: contoured timber paddles; full-perimeter pleated leather, stitching, metal straps, open conical tuyere with inner wall, upper lever, handgrip, wooden foot-cleats, rivets, and seven-bell decorative seal.

Import: OBJ units are meters, Z up. Verify conversion to centimeters on UE5 import (often ×100), preserve eight material slots, use simple collision rather than per-triangle for dynamic props. Use LOD1 at distance; inspect Nanite suitability rather than assuming.

Run generator: node Tools/Art/generate_forge_bellows_1348.js Content/Art/Environment/Medieval1348/ForgeBellows

Status: geometry + UV only. MTL colors are placeholders; no finished PBR textures, collision meshes, animation or validated UE5 in-engine screenshots. Need 4K PC visual pass.
