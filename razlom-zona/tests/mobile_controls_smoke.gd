extends SceneTree
## Tests gesture ownership and independent fingers without Android emulator.

func _initialize() -> void:
    _run.call_deferred()

func _fail(reason: String) -> void:
    push_error("MOBILE CONTROL FAIL: " + reason)
    quit(1)

func _run() -> void:
    var p = load("res://scripts/player.gd").new()
    root.add_child(p)
    var overlay = load("res://scripts/mobile_controls.gd").new()
    root.add_child(overlay)
    overlay.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
    overlay.size = Vector2(1280, 720)
    overlay.configure(p, null)
    await process_frame

    overlay._begin_touch(0, Vector2(145, 590))
    overlay._drag_touch(0, Vector2(205, 590), Vector2(60, 0))
    if p.virtual_move.x < 0.7:
        _fail("analog left-thumb move did not register")
        return

    var initial_yaw: float = p.rotation.y
    overlay._begin_touch(1, Vector2(820, 300))
    overlay._drag_touch(1, Vector2(873, 306), Vector2(53, 6))
    if absf(p.rotation.y - initial_yaw) < 0.01:
        _fail("independent right-thumb camera look did not rotate")
        return
    if p.virtual_move.x < 0.7:
        _fail("camera look stopped joystick")
        return

    overlay._begin_touch(2, Vector2(995, 616))
    if not p.virtual_sprint:
        _fail("third finger cannot hold sprint")
        return
    overlay._end_touch(0)
    if p.virtual_move != Vector2.ZERO or not p.virtual_sprint:
        _fail("releasing joystick disturbed sprint")
        return
    overlay._end_touch(2)
    if p.virtual_sprint:
        _fail("sprint stuck on release")
        return
    overlay._end_touch(1)
    if overlay.look_finger != -1:
        _fail("look finger not released")
        return
    print("PASS mobile controls: analog joystick + camera + sprint across 3 fingers")
    quit(0)
