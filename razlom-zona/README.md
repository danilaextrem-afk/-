# РАЗЛОМ: ЗОНА

**Standalone Android-first 3D survival / exploration prototype** in a dark pixel-voxel art direction. This is a **separate game** from the larger `РАЗЛОМ: ЧЁРНЫЙ ЗВОН` project.

> Играбельный прототип. Проверенный участок маршрута и ограничения описаны в [журнале проверки](docs/STATUS.md).

## First implemented systems

- Walk/look in first person, sprint and stamina, Android-oriented touch movement buttons + drag-look.
- Flashlight with finite battery; health and medkits.
- One compact procedurally assembled settlement with forest patches, warehouse, watchpost, and enterable bunker descent.
- Ten pick-ups across medkits, batteries and scrap; interaction with `E` / touchscreen action.
- Two ordinary hostile creatures (wander/chase/contact damage); no bosses.
- Two damaging anomaly fields and a shelter with save/heal.
- Persistent local JSON save of coordinates, health, inventory, battery and looted items.
- No mandatory story: gameplay starts in free-roam.

## Open in Godot

1. Install **Godot 4.4 standard** (compatible 4.x may require changes).
2. Import this folder through the Godot Project Manager or run `godot --path . -e` to inspect.
3. Press F6 in `scenes/main.tscn`, or F5 to launch project (Godot editor). No asset download required for graybox.
4. For actual phone testing, configure Android SDK/JDK and Android export templates, then export debug APK. See [`docs/ANDROID_EXPORT.md`](docs/ANDROID_EXPORT.md).

### Controls

| Action | PC | Touchscreen |
|---|---|---|
| Walk | WASD | Arrow buttons, lower left |
| Look | Mouse | Drag on right half of screen |
| Sprint | Shift | Hold «БЕГ» |
| Interact / use medkit | E | «ДЕЙСТВИЕ» |
| Flashlight | F | «СВЕТ» |
| Save | F5 or interact at shelter | Shelter |
| Unlock mouse | Esc | N/A |

## Состояние маршрута

Посёлок и склад остаются блокингом. Восточный спуск ведёт к аптечке на глубине около пяти метров. Следующий участок — дорога через лес и промышленный КПП.
