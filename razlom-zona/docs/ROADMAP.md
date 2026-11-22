# Short-cycle production plan (goal: first Android playable build in 2–4 weeks)

**Estimate, not guarantee.** Work on environment art, programming and Android packaging in parallel; do not wait to finish art before testing the build.

## Week 1 — engine proof + controls
- Import project and remove parse/runtime errors, PC and Android touch smoke tests.
- Scene bootstrap, walk/look, pickup, health/stamina/flashlight, collision baseline.
- Deliverable: recorded in-engine test, not concept art.

## Week 2 — survival loop
- Verify hostile NPC damage, anomaly field warning, medkit/battery, shelter, local save/load.
- Fix touch gesture conflict and make interaction reliable on 6-inch screen.
- Deliverable: 10 minutes of repeatable play without crash.

## Week 3 — visual fidelity and world density
- Replace primitive buildings/creatures with modular low-poly/voxel art matching approved mood.
- Add forest, weathering, lighting pass, audio, one accessible bunker interior.
- Deliverable: side-by-side engine frames vs visual reference; first on-device performance logs.

## Week 4 — Android polish and release candidate
- Export signed/unsigned debug APK in a supported toolchain, target 30 FPS midrange device.
- Fix save/load, UI, death/respawn, GPU memory and input bugs; run offline.
- Deliverable: APK, screen recording from actual device, checklist and known issues.

## Explicitly out of scope for v0.1
- Huge open world, driveable vehicles, 40-hour narrative, two-era travel, networking, bosses, AAA graphics claims, app-store publication.
- The large 1348/2026 project remains independent; assets/mechanics may later be transferred where license and engine compatibility permit.
