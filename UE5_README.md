# RAZLOM — Windows PC Unreal Engine 5 (initial native foundation)

- This is **not** a compiled or released game.
- `Razlom.uproject` and `Source/Razlom` contain a real native UE5 C++ module and a first time-vision component.
- `URazlomTimeGlimpseComponent` emits enter/exit Blueprint events lasting 3–5 seconds without teleporting the player.
- The native A01M05 entrance blockout uses this component for a four-second palisade vision. Its own checkpoint slot tracks the tenant investigation and choice. The Windows build and in-engine playthrough remain unverified.
- `Tools/ue5_prepare_import.py` is a script to run **inside the Unreal Editor's Python environment** to import current draft OBJ models as engine assets. It does not set up full physically based materials.
- `Tests/ue5_scaffold_check.py` validates source contracts on GitHub Actions without the engine; this is NOT evidence of a successful Windows UE5 build.
- `Docs/09_ART_REWORK_ACCEPTANCE.md` is the art quality checklist and current list of items needing replacement.

## Windows developer setup
1. Install Unreal Engine 5.5 (or retarget compatible UE5 installation) and Windows C++ build tools.
2. Open `Razlom.uproject` and compile the native `Razlom` module. If the editor asks to upgrade/retarget project files, verify with the branch changes recorded in git.
3. In-editor, run Tools/ue5_prepare_import.py with editor scripting enabled. Verify OBJ triangulation, scaling, UV, normals, material slots.
4. To build and exercise the entrance blockout, run `Tools/build_entrance_win64.ps1 -EngineRoot <installed-UE-5.5-directory>`. WASD moves, mouse aims, E interacts, left mouse repels the shade, Q leaves the tenant.
5. Only label an asset approved/AAA after visible and performance checks.

The historical Godot project remains in place for legacy reference. **No 4K AAA visual claim is currently supported**.
