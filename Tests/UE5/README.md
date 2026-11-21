# Unreal Engine 5 verification gate (NOT YET RUN)

Requires Windows + Epic Launcher UE 5.5 installation or a compatible source build.

1. Open Razlom.uproject; allow IDE project generation / compile the RazlomEditor target.
2. Attach RazlomTimeGlimpseComponent to a pawn or a scene actor.
3. In Blueprint bind OnGlimpseStarted/OnGlimpseEnded to camera, overlay and audio transitions.
4. Trigger StartGlimpse("horse_to_car", 4.0); record exact 4-second duration and verify pawn transform / velocity / save-state never change.
5. Check overlap rejection, cancellation, invalid durations 2.99 / 5.01, and event IDs on completion.
6. Test 3840×2160 mode and **measure** FPS/VRAM on the target GPU; screenshots should be taken in PIE/packaged game, not mistaken for mesh previews.

No Windows Unreal compilation, shader build, packaged EXE, or frame-rate test has occurred in this session.
Historical Godot .gd and .tscn content remains unmodified as an archived prototype.
