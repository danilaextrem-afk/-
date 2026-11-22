# RAZLOM: BLACK BELL — original modular izba (Rus 1348)

**Status:** geometry WIP for Windows PC / Unreal Engine 5. This is not a finished AAA asset.

Nine reusable asset families, each with two actual OBJ LOD meshes: WallSolid, WallWindow, WallDoor, CornerPost, Floor4m, RoofHalf, DoorLeaf, Table, Bench. Wall courses are individually modeled in 3D, with open window and door gaps (not transparent decals), split-log end grain, jambs and lintels, plank flooring, sloped timber rafters and overlapping shingles, hinged door leaf and furniture. No third-party assets.

## Mesh counts (triangles: LOD0 / LOD1)

- WallSolid: 1248 / 428
- WallWindow: 1800 / 676
- WallDoor: 2208 / 836
- CornerPost: 116 / 36
- Floor4m: 984 / 144
- RoofHalf: 2388 / 720
- DoorLeaf: 288 / 204
- Table: 284 / 132
- Bench: 248 / 108

## Snap and import

Wall segments: 4m length / 2.7m height / ~0.25m thickness. Local wall origin at left ground corner, along +X; front is -Y. Floor: 4m × 4m; top Z=0. RoofHalf: 4m × 2.6m, local ridge relative Z=0.9, eaves Z=0; position at world Z=2.7 and Y=2 to form a gable roof. `assembly_4m_cabin.json` provides a 14-instance placement blueprint. All coordinates are in **meters, Z up**; verify ×100 cm conversion on UE5 import.

Use the native Node 18+ code to regenerate the OBJ geometry from scratch:

```sh
node Tools/Art/generate_izba_1348.js
node Tools/Art/validate_izba_1348.js
```

## Remaining work

OBJ and MTL carry geometry and UVs; color materials are placeholders. Need dedicated sculpt/high-poly bake for unique timber wear; real PBR color/roughness/normal/AO maps; texture packing; authored collision, proper pivot/LOD/Nanite decision, in-engine Unreal scene composition, gameplay camera inspection, light builds, and measured Windows 4K performance. Do not mark final or UE5-validated yet.
