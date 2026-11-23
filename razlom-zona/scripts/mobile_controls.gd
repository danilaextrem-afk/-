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

const BG = Color(0.035, 0.068, 0.082, 0.66)
const OUTLINE = Color(0.44, 0.73, 0.71, 0.61)
const ACTIVE = Color(0.25, 0.96, 0.83, 0.90)
const LABEL = Color(0.91, 0.97, 0.95, 0.97)
const MUTED = Color(0.62, 0.74, 0.76, 0.85)

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

## Keep the interactive target close to the visible ring: camera swipes
## beside a button must not silently consume medkits or trigger saves.
func _touch_button_at(p: Vector2) -> String:
    var layout := _layout()
    for id in ["interact", "sprint", "torch", "medkit", "save"]:
        if p.distance_to(layout[id]) <= _button_radius(id) * 0.94:
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
    # Android may deliver a repeated press for the same pointer index.
    # Never reassign an active finger or fire a one-shot action twice.
    if finger == stick_finger or finger == look_finger or button_fingers.has(finger):
        return
    var button_id := _touch_button_at(p)
    if button_id != "":
        # A second finger on a one-shot action must not consume two items
        # or trigger two interactions during the same physical press.
        var first_press := not held_buttons.has(button_id)
        button_fingers[finger] = button_id
        held_buttons[button_id] = true
        if not first_press and button_id != "sprint":
            queue_redraw()
            return
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
        # A floating stick must start exactly under the thumb. Clamping
        # its origin to an inset margin made edge swipes jump abruptly
        # (especially on narrow Android viewports).
        stick_center = p
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
        # A second finger may still hold the same action. Only release it
        # when the last owning finger lifts, otherwise sprint flickers off.
        var still_held := button_fingers.values().has(id)
        if not still_held:
            held_buttons.erase(id)
        if id == "sprint":
            player.virtual_sprint = still_held
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
    var ink := ACTIVE if pressed else Color(.89,.89,.81,.68)
    draw_circle(center, radius*.86, Color(.05,.06,.055,.39 if pressed else .22))
    for quarter in 4:
        var start := quarter*PI*.5+.16
        draw_arc(center, radius*.86, start, start+1.23, 12, ink, 1.3*s, true)
    var font_size := maxi(12, int(15 * s))
    _draw_text(caption, center + Vector2(0, font_size * 0.31), font_size, LABEL, true)

func _draw() -> void:
    if size.x < 350 or size.y < 240:
        return
    var layout := _layout()
    var s := _scale()
    var base: Vector2 = layout["stick"] if stick_finger == -1 else stick_center
    var tip: Vector2 = base if stick_finger == -1 else stick_tip

    # Floating movement stick: distinct left-thumb target with real analog travel.
    var engaged := stick_finger != -1
    var outline_radius := (70 if engaged else 35)*s
    for quarter in 4:
        var start := quarter*PI*.5+.30
        draw_arc(base,outline_radius,start,start+.96,12, OUTLINE, 1.6*s,true)
    draw_circle(tip, (16 if engaged else 5)*s,Color(.84,.88,.77,.45))

    _draw_button("interact", "ДЕЙСТВ.", layout["interact"])
    _draw_button("sprint", "БЕГ", layout["sprint"])
    _draw_button("torch", "СВЕТ", layout["torch"])
    _draw_button("medkit", "АПТ", layout["medkit"])
    _draw_button("save", "СОХР", layout["save"])

    if is_instance_valid(player):
        for item in [["ЗДР",player.health,Color("#d9967c")], ["ВЫН",player.stamina,Color("#abb99a")], ["ЗАР",player.battery,Color("#b9c9c3")]]:
            var index: int = ["ЗДР","ВЫН","ЗАР"].find(item[0])
            var corner := Vector2(23+index*101,22)*s
            _draw_text(item[0],corner+Vector2(0,7)*s,maxi(12,int(14*s)),LABEL)
            var line := Rect2(corner+Vector2(0,14)*s,Vector2(79,4)*s)
            draw_rect(line,Color(.06,.07,.06,.65))
            draw_rect(Rect2(line.position,Vector2(line.size.x*clampf(float(item[1])/100,0,1),line.size.y)),item[2])
    if is_instance_valid(game) and game.inventory is Dictionary:
        _draw_text("АПТ %d" % int(game.inventory.get("medkit", 0)), Vector2(27 * s, size.y - 30 * s), maxi(12, int(16 * s)), MUTED)

    # Lightweight center reticle. It doesn't compete with right-thumb look.
    var aim := size / 2.0
    for v in [Vector2(-13, 0), Vector2(13, 0), Vector2(0, -13), Vector2(0, 13)]:
        var d: Vector2 = v.normalized()
        draw_line(aim + v - d * 5 * s, aim + v, Color(0.78, 0.95, 0.93, 0.55), 1.5 * s)
