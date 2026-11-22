# Watcher Ninefold Flail Head v1 — geometry WIP

Original 2026 occult enemy weapon attachment for the Watcher of Ninth Entrance. Nine radial teeth, forged shell, suspension ring. Intended to hang from a chain near the Watcher's gauntlet; **chain and gameplay socket not included**.

- OBJ units: metres; local Y up. Outer radial extent 0.193 m; body height 0.28 m; suspension top approx 0.196 m.
- 395 vertices, 395 UV entries, 702 triangles; procedural index-range check passed during initial construction.
- Generate: `node generate.js > SM_Watcher_NinefoldFlailHead_v1.obj` (independent Node rerun not yet verified).
- MTL is viewport-only, not a complete PBR texture set. UVs are parametric and overlapping; **not bake-ready**.
- Current readiness: geometry_wip. Integration blockers: actual DCC preview, chain connection, collision, UV atlas, normals/tangents, PBR textures, LODs, UE5 import and artistic approval against PR #7.
- No external assets, no Unreal engine claims. Keep separate from PR #13's key/chest scope and from Astra gameplay branches.
