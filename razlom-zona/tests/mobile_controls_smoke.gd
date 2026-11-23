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

    # Tremor stays still, and crossing the radial threshold must not
    # instantly jump to ~8.5% movement speed.
    overlay._drag_touch(0, Vector2(145 + 69.0 * 0.08, 590), Vector2.ZERO)
    if p.virtual_move.length() > 0.001:
        _fail("joystick tremor inside deadzone moved player")
        return
    overlay._drag_touch(0, Vector2(145 + 69.0 * 0.10, 590), Vector2.ZERO)
    if p.virtual_move.length() <= 0.0 or p.virtual_move.length() >= 0.04:
        _fail("joystick crossed deadzone with abrupt movement jump")
        return
    overlay._drag_touch(0, Vector2(205, 590), Vector2.ZERO)
    if p.virtual_move.x < 0.7:
        _fail("joystick lost full analog range after deadzone")
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
    # Duplicate down for pointer 4 must not steal the camera or re-fire medkit.
    overlay._begin_touch(4, Vector2(820, 300))
    if overlay.look_finger == 4 or overlay.button_fingers.get(4, "") != "medkit":
        _fail("duplicate pointer-down reassigned active medkit finger")
        return
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
    # Regression: an edge-started floating stick must not teleport inward.
    # It should begin under the actual thumb and preserve analog direction.
    var edge_start := Vector2(18, 700)
    overlay._begin_touch(8, edge_start)
    if overlay.stick_finger != 8 or overlay.stick_center.distance_to(edge_start) > 0.01:
        _fail("edge joystick origin jumped away from the touch point")
        return
    overlay._drag_touch(8, edge_start + Vector2(35, -35), Vector2(35, -35))
    if p.virtual_move.x < 0.4 or p.virtual_move.y > -0.4:
        _fail("edge joystick drag lost its expected analog direction")
        return
    overlay._end_touch(8)
    if p.virtual_move != Vector2.ZERO or overlay.stick_finger != -1:
        _fail("edge joystick remained engaged after release")
        return
    # Upper-left area is outside the lower-left joystick zone and must
    # support camera look rather than silently dropping the gesture.
    var upper_left := Vector2(150, 90)
    var yaw_before_upper_left: float = p.rotation.y
    overlay._begin_touch(9, upper_left)
    if overlay.look_finger != 9 or overlay.stick_finger != -1:
        _fail("upper-left free-screen touch was not assigned to camera")
        return
    overlay._drag_touch(9, upper_left + Vector2(40, 0), Vector2(40, 0))
    if absf(p.rotation.y - yaw_before_upper_left) < 0.01:
        _fail("upper-left camera swipe did not rotate player")
        return
    overlay._end_touch(9)
    if overlay.look_finger != -1:
        _fail("upper-left camera gesture remained engaged after release")
        return
    # A second free finger takes over camera look when the first lifts.
    # Releasing the waiting finger early must never steal the camera.
    overlay._begin_touch(10, Vector2(820, 300))
    overlay._begin_touch(11, Vector2(770, 310))
    if overlay.look_finger != 10 or not overlay.waiting_look_fingers.has(11):
        _fail("backup camera finger not tracked")
        return
    overlay._end_touch(10)
    if overlay.look_finger != 11:
        _fail("camera look stopped instead of transferring to held finger")
        return
    var yaw_before_handoff: float = p.rotation.y
    overlay._drag_touch(11, Vector2(790, 310), Vector2(20, 0))
    if absf(p.rotation.y - yaw_before_handoff) < 0.01:
        _fail("promoted camera finger cannot rotate view")
        return
    overlay._end_touch(11)
    if overlay.look_finger != -1 or not overlay.waiting_look_fingers.is_empty():
        _fail("camera finger handoff left stale pointer ownership")
        return
    overlay._begin_touch(12, Vector2(820, 300))
    overlay._begin_touch(13, Vector2(770, 310))
    overlay._end_touch(13)
    if overlay.look_finger != 12 or not overlay.waiting_look_fingers.is_empty():
        _fail("releasing backup camera finger stole primary look")
        return
    overlay._end_touch(12)
    # Exercise HUD drawing on a tall landscape screen and at low resource
    # values; a draw-time error must fail the headless gameplay smoke run.
    overlay.size = Vector2(1600, 720)
    p.health = 0.0
    p.stamina = 47.0
    p.battery = 100.0
    overlay.queue_redraw()
    await process_frame
    if overlay._scale() <= 0.0 or overlay._layout()["save"].x > overlay.size.x:
        _fail("HUD layout invalid at 20:9 landscape")
        return
    overlay.size = Vector2(1280, 720)
    overlay.queue_redraw()
    await process_frame
    print("PASS mobile controls: analog joystick + camera + sprint across 4 fingers with shared sprint")
    quit(0)
