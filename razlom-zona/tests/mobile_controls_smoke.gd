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
    overlay._begin_touch(3, Vector2(995, 616))
    overlay._end_touch(2)
    if not p.virtual_sprint or not overlay.held_buttons.has("sprint"):
        _fail("releasing one of two sprint fingers stopped sprint")
        return
    overlay._end_touch(3)
    if p.virtual_sprint or overlay.held_buttons.has("sprint"):
        _fail("sprint stuck after final finger released")
        return
    overlay._end_touch(1)
    if overlay.look_finger != -1:
        _fail("look finger not released")
        return
    var medkit_presses := [0]
    overlay.medkit_requested.connect(func() -> void: medkit_presses[0] += 1)
    overlay._begin_touch(4, Vector2(1045, 464))
    overlay._begin_touch(5, Vector2(1045, 464))
    if medkit_presses[0] != 1:
        _fail("overlapping fingers consumed multiple medkits")
        return
    overlay._end_touch(4)
    if not overlay.held_buttons.has("medkit"):
        _fail("one-shot button released while second finger still held")
        return
    overlay._end_touch(5)
    overlay._begin_touch(6, Vector2(1045, 464))
    overlay._end_touch(6)
    if medkit_presses[0] != 2:
        _fail("new medkit press was not recognized after release")
        return
    # The old 1.24x invisible hit radius could consume a medkit when
    # the player intended to rotate the camera just beside its icon.
    var near_medkit := Vector2(1088, 464)
    if overlay._touch_button_at(near_medkit) != "":
        _fail("invisible button hit area steals camera swipe beside medkit")
        return
    var yaw_before_edge: float = p.rotation.y
    overlay._begin_touch(7, near_medkit)
    overlay._drag_touch(7, near_medkit + Vector2(20, 0), Vector2(20, 0))
    if medkit_presses[0] != 2 or absf(p.rotation.y - yaw_before_edge) < 0.01:
        _fail("near-button swipe did not rotate camera safely")
        return
    overlay._end_touch(7)
    if overlay.look_finger != -1:
        _fail("near-button camera finger was not released")
        return
    print("PASS mobile controls: analog joystick + camera + sprint across 4 fingers with shared sprint")
    quit(0)
