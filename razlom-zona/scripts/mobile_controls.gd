extends Control
## Multi-touch, floating joystick + independent camera look for Android.
## This overlay owns touch fingers, never makes simulated keyboard events.
## All dimensions derive from the current viewport so it works on 16:9 and 20:9.

signal interact_requested
signal flashlight_requested
signal medkit_requested
signal save_requested

var player  # dynamic Player script contains virtual_move/stamina/battery
var game  # dynamic Game script exposes inventory
var stick_finger: int = -1
var look_finger: int = -1
var stick_center: Vector2 = Vector2.ZERO
var stick_tip: Vector2 = Vector2.ZERO
var button_fingers: Dictionary = {}  # index -> button name
var held_buttons: Dictionary = {}
var show_labels: bool = true
var look_multiplier: float = 1.08

const BG := Color(0.035, 0.068, 0.082, 0.66)
const OUTLINE := Color(0.44, 0.73, 0.71, 0.61)
const ACTIVE := Color(0.25, 0.96, 0.83, 0.90)
const LABEL := Color(0.91, 0.97, 0.95, 0.97)
const MUTED := Color(0.62, 0.74, 0.76, 0.85)

func configure(player_node: CharacterBody3D, game_node: Node3D) -> void:
    player = player_node
    game = game_node
    mouse_filter = Control.MOUSE_FILTER_IGNORE
    focus_mode = Control.FOCUS_NONE
    set_process_input(true)
    set_process(true)
    queue_redraw()

func _scale() -> float:
    return clampf(minf(size.x / 1280.0, size.y / 720.0), 0.78, 1.24)

func _layout() -> Dictionary:
    var s := _scale()
    var w := size.x
    var h := size.y
    return {
        "stick": Vector2(139.0 * s, h - 135.0 * s),
        "interact": Vector2(w - 110.0 * s, h - 120.0 * s),
        "sprint": Vector2(w - 285.0 * s, h - 104.0 * s),
        "torch": Vector2(w - 99.0 * s, h - 282.0 * s),
        "medkit": Vector2(w - 235.0 * s, h - 256.0 * s),
        "save": Vector2(w - 85.0 * s, 78.0 * s)
    }

func _button_radius(id: String) -> float:
    var factor := 0.0
    match id:
        "interact": factor = 56.0
        "sprint": factor = 46.0
        "torch": factor = 43.0
        "medkit": factor = 39.0
        "save": factor = 34.0
    return factor * _scale()

func _touch_button_at(p: Vector2) -> String:
    var layout := _layout()
    for id in ["interact", "sprint", "torch", "medkit", "save"]:
        if p.distance_to(layout[id]) < _button_radius(id) * 1.24:
            return id
    return ""

func _input(event: InputEvent) -> void:
    if player == null or not is_instance_valid(player):
        return
    if event is InputEventScreenTouch:
        if event.pressed:
            _begin_touch(event.index, event.position)
        else:
            _end_touch(event.index)
        get_viewport().set_input_as_handled()
    elif event is InputEventScreenDrag:
        _drag_touch(event.index, event.position, event.relative)
        get_viewport().set_input_as_handled()

func _begin_touch(finger: int, p: Vector2) -> void:
    var button_id := _touch_button_at(p)
    if button_id != "":
        button_fingers[finger] = button_id
        held_buttons[button_id] = true
        match button_id:
            "interact": interact_requested.emit()
            "torch": flashlight_requested.emit()
            "medkit": medkit_requested.emit()
            "save": save_requested.emit()
            "sprint": player.virtual_sprint = true
        queue_redraw()
        return

    # Left side is a floating, analog thumbstick. Starting anywhere in
    # the lower-left 45% avoids requiring a precise thumb position.
    if stick_finger == -1 and p.x < size.x * 0.45 and p.y > size.y * 0.38:
        stick_finger = finger
        var margin := 88.0 * _scale()
        stick_center = Vector2(
            clampf(p.x, margin, size.x * 0.45 - margin),
            clampf(p.y, size.y * 0.42, size.y - margin)
        )
        stick_tip = stick_center
        player.virtual_move = Vector2.ZERO
    elif look_finger == -1 and p.x >= size.x * 0.42:
        look_finger = finger
    queue_redraw()

func _drag_touch(finger: int, p: Vector2, relative: Vector2) -> void:
    if finger == stick_finger:
        var radius := 69.0 * _scale()
        var v: Vector2 = (p - stick_center) / radius
        # Ignore small tremors; full analog movement / diagonal control.
        if v.length() < 0.085:
            v = Vector2.ZERO
        stick_tip = stick_center + v.limit_length() * radius
        player.virtual_move = v.limit_length()
        queue_redraw()
    elif finger == look_finger:
        player.look_from_touch(relative * look_multiplier)

func _end_touch(finger: int) -> void:
    if finger == stick_finger:
        stick_finger = -1
        stick_tip = Vector2.ZERO
        player.virtual_move = Vector2.ZERO
    if finger == look_finger:
        look_finger = -1
    if button_fingers.has(finger):
        var id: String = button_fingers[finger]
        button_fingers.erase(finger)
        held_buttons.erase(id)
        if id == "sprint":
            player.virtual_sprint = false
    queue_redraw()

func _notification(what: int) -> void:
    if what == NOTIFICATION_RESIZED:
        queue_redraw()
    elif what == NOTIFICATION_WM_WINDOW_FOCUS_OUT:
        stick_finger = -1
        look_finger = -1
        button_fingers.clear()
        held_buttons.clear()
        if is_instance_valid(player):
            player.virtual_move = Vector2.ZERO
            player.virtual_sprint = false
        queue_redraw()

func _process(_delta: float) -> void:
    # Bars, object prompts, battery and visual pressed states update live.
    queue_redraw()

func _draw_text(value: String, pos: Vector2, font_size: int, color: Color = LABEL, center: bool = false) -> void:
    var font := ThemeDB.fallback_font
    var p := pos
    if center:
        var width := font.get_string_size(value, HORIZONTAL_ALIGNMENT_LEFT, -1, font_size).x
        p.x -= width / 2.0
    draw_string(font, p, value, HORIZONTAL_ALIGNMENT_LEFT, -1, font_size, color)

func _draw_button(id: String, caption: String, center: Vector2) -> void:
    var radius := _button_radius(id)
    var pressed := held_buttons.has(id)
    var s := _scale()
    draw_circle(center + Vector2(0, 4.0 * s), radius + 6.0 * s, Color(0, 0, 0, 0.34))
    draw_circle(center, radius, Color(0.055, 0.12, 0.14, 0.83 if pressed else 0.65))
    draw_arc(center, radius - 2.2 * s, 0.0, TAU, 64, ACTIVE if pressed else OUTLINE, 2.5 * s, true)
    if pressed:
        draw_circle(center, radius * 0.79, Color(0.22, 0.64, 0.57, 0.23))
    var font_size := maxi(12, int(15 * s))
    _draw_text(caption, center + Vector2(0, font_size * 0.31), font_size, LABEL, true)

func _stat(title: String, value: float, color: Color, origin: Vector2, scale_factor: float) -> void:
    var s := scale_factor
    _draw_text(title, origin + Vector2(0, 10 * s), maxi(11, int(14 * s)), MUTED)
    var bg := Rect2(origin + Vector2(54 * s, 2 * s), Vector2(158 * s, 11 * s))
    draw_rect(bg, Color(0.05, 0.10, 0.11, 0.75), true)
    draw_rect(Rect2(bg.position, Vector2(bg.size.x * clampf(value / 100.0, 0.0, 1.0), bg.size.y)), color, true)
    draw_rect(bg, OUTLINE, false, maxf(1.0, s))
    _draw_text("%d" % int(value), origin + Vector2(220 * s, 11 * s), maxi(11, int(13 * s)), LABEL)

func _draw() -> void:
    if size.x < 350 or size.y < 240:
        return
    var layout := _layout()
    var s := _scale()
    var base: Vector2 = layout["stick"] if stick_finger == -1 else stick_center
    var tip: Vector2 = base if stick_finger == -1 else stick_tip

    # Floating movement stick: distinct left-thumb target with real analog travel.
    draw_circle(base, 87 * s, Color(0.0, 0.0, 0.0, 0.21))
    draw_circle(base, 73 * s, BG)
    draw_arc(base, 73 * s, 0.0, TAU, 72, OUTLINE, 2.8 * s, true)
    draw_line(base + Vector2(-24, 0) * s, base + Vector2(24, 0) * s, Color(0.7, 0.9, 0.88, 0.19), 1.5 * s)
    draw_line(base + Vector2(0, -24) * s, base + Vector2(0, 24) * s, Color(0.7, 0.9, 0.88, 0.19), 1.5 * s)
    draw_circle(tip, 33 * s, Color(0.12, 0.28, 0.29, 0.87))
    draw_arc(tip, 32 * s, 0.0, TAU, 50, ACTIVE if stick_finger != -1 else OUTLINE, 2.3 * s, true)

    _draw_button("interact", "ВЗЯТЬ", layout["interact"])
    _draw_button("sprint", "БЕГ", layout["sprint"])
    _draw_button("torch", "СВЕТ", layout["torch"])
    _draw_button("medkit", "АПТ", layout["medkit"])
    _draw_button("save", "СОХР", layout["save"])

    # Crisp readable mobile HUD, independent of 3D render resolution.
    draw_rect(Rect2(Vector2(17, 13) * s, Vector2(267, 115) * s), Color(0.03, 0.065, 0.079, 0.62), true)
    draw_rect(Rect2(Vector2(17, 13) * s, Vector2(267, 115) * s), Color(0.26, 0.54, 0.56, 0.31), false, maxf(1, s))
    if is_instance_valid(player):
        _stat("ЗДР", player.health, Color("#ee7568"), Vector2(32, 31) * s, s)
        _stat("ВЫН", player.stamina, Color("#c5c67c"), Vector2(32, 63) * s, s)
        _stat("ЗАР", player.battery, Color("#6bdbcc"), Vector2(32, 95) * s, s)
    if is_instance_valid(game) and game.inventory is Dictionary:
        _draw_text("АПТ %d" % int(game.inventory.get("medkit", 0)), Vector2(27 * s, size.y - 30 * s), maxi(12, int(16 * s)), MUTED)

    # Lightweight center reticle. It doesn't compete with right-thumb look.
    var aim := size / 2.0
    for v in [Vector2(-13, 0), Vector2(13, 0), Vector2(0, -13), Vector2(0, 13)]:
        var d := v.normalized()
        draw_line(aim + v - d * 5 * s, aim + v, Color(0.78, 0.95, 0.93, 0.55), 1.5 * s)
