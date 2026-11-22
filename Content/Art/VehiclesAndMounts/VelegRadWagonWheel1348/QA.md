# QA — wagon wheel v0.1
Procedural original 3D wheel (wood rim, ten spokes, hub, iron tyre, iron spoke brackets). Generator requires Node.js 20+.

Run: `node generate.cjs 32 .` and `node generate.cjs 16 .`.

UV coordinates and triangular faces are emitted by the generator. Each component is a separate intersecting solid; manifold and collision checks remain pending. Material values are provisional; no texture maps or UE5 import tested. Art quality floor PR #7 / bell v3 PR #5 NOT achieved.
