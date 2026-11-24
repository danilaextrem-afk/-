extends CharacterBody3D
## Prototype FPS controller for keyboard/mouse and touchscreen.
## No external add-ons or downloaded assets required.

var health: float = 100.0
var stamina: float = 100.0
var battery: float = 100.0
var virtual_move: Vector2 = Vector2.ZERO
var virtual_sprint: bool = false
var alive: bool = true

const WALK_SPEED: float = 4.5
const RUN_SPEED: float = 7.5
const LOOK_SENSITIVITY: float = 0.0026

var boot_foley := preload("res://scripts/boot_foley.gd").new()
var hand: Node3D
var stride := 0.0
var camera: Camera3D
var torch: SpotLight3D
var flashlight_on: bool = true
var touch_drag_active: bool = false

func _ready() -> void:
    name = "Player"
    add_child(boot_foley)
    var shape := CollisionShape3D.new()
    var capsule := CapsuleShape3D.new()
    capsule.radius = 0.35
    capsule.height = 1.75
    shape.shape = capsule
    shape.position.y = 0.875
    add_child(shape)

    camera = Camera3D.new()
    camera.position.y = 1.57
    camera.current = true
    camera.fov = 74.0
    add_child(camera)

    hand = preload("res://assets/zalesye/field_lamp.glb").instantiate() as Node3D
    hand.position = Vector3(.255,-.237,-.47)
    hand.rotation_degrees = Vector3(5,12,-6)
    camera.add_child(hand)
    torch = SpotLight3D.new()
    torch.name = "Flashlight"
    torch.light_color = Color("e4d8ba")
    torch.light_energy = 1.15
    torch.spot_range = 16.0
    torch.spot_angle = 29.0
    torch.spot_angle_attenuation = .77
    torch.shadow_enabled = false
    camera.add_child(torch)

    if not OS.has_feature("mobile"):
        Input.mouse_mode = Input.MOUSE_MODE_CAPTURED

func _physics_process(delta: float) -> void:
    if not alive:
        velocity.x = 0.0
        velocity.z = 0.0
        velocity.y -= 18.0 * delta
        move_and_slide()
        return

    var input_vector := Input.get_vector("move_left", "move_right", "move_forward", "move_backward")
    input_vector += virtual_move
    input_vector = input_vector.limit_length()

    var running := (Input.is_action_pressed("sprint") or virtual_sprint) and input_vector.length() > 0.15 and stamina > 1.0
    if running:
        stamina = maxf(0.0, stamina - 19.0 * delta)
    else:
        stamina = minf(100.0, stamina + 14.0 * delta)

    var speed := RUN_SPEED if running else WALK_SPEED
    var direction := global_transform.basis * Vector3(input_vector.x, 0.0, input_vector.y)
    velocity.x = direction.x * speed
    velocity.z = direction.z * speed
    if is_on_floor():
        velocity.y = -0.2
    else:
        velocity.y -= 18.0 * delta
    move_and_slide()

    var displacement := get_position_delta()
    var travel := Vector2(displacement.x, displacement.z).length()
    boot_foley.advance(travel, is_on_floor(), running, global_position)
    var pace := travel / maxf(delta, 0.0001) if is_on_floor() else 0.0
    stride = fmod(stride + pace * delta * 1.65, TAU)
    var sway := Vector3(sin(stride)*.010, cos(stride*2.0)*.006, 0) * minf(1.0,pace/4)
    hand.position = hand.position.lerp(Vector3(.255,-.237,-.47)+sway, minf(1.0,delta*14))
    if flashlight_on:
        battery = maxf(0.0, battery - 0.12 * delta)
        if battery <= 0.0:
            flashlight_on = false
    torch.visible = flashlight_on and battery > 0.0

func _unhandled_input(event: InputEvent) -> void:
    if event is InputEventKey and event.pressed and not event.echo:
        if event.keycode == KEY_ESCAPE:
            Input.mouse_mode = Input.MOUSE_MODE_VISIBLE
    if event is InputEventMouseButton and event.pressed and not OS.has_feature("mobile"):
        if event.button_index == MOUSE_BUTTON_LEFT:
            Input.mouse_mode = Input.MOUSE_MODE_CAPTURED
    if event is InputEventMouseMotion and Input.mouse_mode == Input.MOUSE_MODE_CAPTURED:
        _look(event.relative)
    # Touch gestures are owned by MobileControls. Never rotate twice
    # because an action button and a camera drag share the same finger.

func look_from_touch(relative: Vector2) -> void:
    _look(relative)

func _look(delta_pixels: Vector2) -> void:
    rotate_y(-delta_pixels.x * LOOK_SENSITIVITY)
    camera.rotation.x = clampf(camera.rotation.x - delta_pixels.y * LOOK_SENSITIVITY, -1.35, 1.35)

func toggle_flashlight() -> void:
    if battery > 0.0:
        flashlight_on = not flashlight_on
        torch.visible = flashlight_on

func hit(damage: float) -> void:
    health = maxf(0.0, health - damage)
    if health <= 0.0:
        alive = false

func restore() -> void:
    health = 100.0
    stamina = 100.0
    battery = 100.0
    alive = true
