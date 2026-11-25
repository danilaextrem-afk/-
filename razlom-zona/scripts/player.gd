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
var motion_blend := 0.0
var look_lag := Vector2.ZERO
var hand_base := Vector3(.255,-.237,-.47)
var hand_rotation := Vector3(5,12,-6)
var lamp_level := 1.0
var lamp_elapsed := 0.0
var lens: MeshInstance3D
var lens_finish: StandardMaterial3D
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
    hand.position = hand_base
    hand.rotation_degrees = hand_rotation
    camera.add_child(hand)
    torch = SpotLight3D.new()
    torch.name = "Flashlight"
    torch.light_color = Color("e4d8ba")
    torch.light_energy = 1.65
    torch.spot_range = 17.0
    torch.spot_angle = 32.0
    torch.spot_angle_attenuation = .77
    torch.shadow_enabled = false
    camera.add_child(torch)
    lens = MeshInstance3D.new()
    lens.name = "LampLens"
    var lens_mesh := CylinderMesh.new()
    lens_mesh.top_radius = .048
    lens_mesh.bottom_radius = .048
    lens_mesh.height = .003
    lens_mesh.radial_segments = 20
    lens_mesh.rings = 1
    lens.mesh = lens_mesh
    lens.position = Vector3(0,0,-.180)
    lens.rotation.x = PI*.5
    lens.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
    lens_finish = StandardMaterial3D.new()
    lens_finish.albedo_color = Color("a5b7a9")
    lens_finish.roughness = .3
    lens_finish.emission_enabled = true
    lens_finish.emission = Color("f4dfb4")
    lens.material_override = lens_finish
    hand.add_child(lens)
    _update_lamp(0.0, 0.0, false)

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
    _update_lamp(delta, pace, running)
    if flashlight_on:
        battery = maxf(0.0, battery - 0.12 * delta)
        if battery <= 0.0:
            flashlight_on = false
    torch.visible = flashlight_on and battery > 0.0

func _update_lamp(delta: float, pace: float, running: bool) -> void:
    lamp_elapsed += delta
    motion_blend = move_toward(motion_blend, clampf(pace / WALK_SPEED,0.0,1.0), delta*6.5)
    look_lag = look_lag.lerp(Vector2.ZERO,minf(1.0,delta*9.0))
    var stride_strength := motion_blend * (1.35 if running else 1.0)
    var breath := sin(lamp_elapsed*1.8)*.0025
    var sway := Vector3(sin(stride)*.015,cos(stride*2.0)*.009,absf(sin(stride))*.010)*stride_strength
    var target := hand_base+sway+Vector3(look_lag.x*.00065,look_lag.y*.00055+breath,0.0)
    hand.position = hand.position.lerp(target,minf(1.0,delta*12.0))
    var angle := hand_rotation+Vector3(look_lag.y*.16,sin(stride)*1.8*stride_strength+look_lag.x*.17,-sin(stride)*1.5*stride_strength)
    hand.rotation_degrees = hand.rotation_degrees.lerp(angle,minf(1.0,delta*12.0))
    # Light originates at the visible lens; aim converges near the centre of view.
    torch.position = camera.to_local(hand.to_global(Vector3(0,0,-.185)))
    torch.look_at(camera.to_global(Vector3(look_lag.x*.008,-.06+look_lag.y*.008,-8.0)),camera.global_basis.y)
    var power := 0.0 if not flashlight_on or battery <= 0.0 else lerpf(.38,1.0,smoothstep(0.0,18.0,battery))
    lamp_level = move_toward(lamp_level,power,delta*8.0)
    torch.light_energy = 1.65*lamp_level
    lens_finish.emission_energy_multiplier = 1.9*lamp_level if flashlight_on else 0.0
    lens_finish.albedo_color = Color("b9c3ad") if flashlight_on else Color("354440")

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
    look_lag = (look_lag+delta_pixels*.28).limit_length(14.0)
    rotate_y(-delta_pixels.x * LOOK_SENSITIVITY)
    camera.rotation.x = clampf(camera.rotation.x - delta_pixels.y * LOOK_SENSITIVITY, -1.35, 1.35)

func toggle_flashlight() -> void:
    if battery > 0.0:
        flashlight_on = not flashlight_on
        torch.visible = flashlight_on
        if not flashlight_on:
            lens_finish.emission_energy_multiplier = 0.0
            lens_finish.albedo_color = Color("354440")

func hit(damage: float) -> void:
    health = maxf(0.0, health - damage)
    if health <= 0.0:
        alive = false

func restore() -> void:
    health = 100.0
    stamina = 100.0
    battery = 100.0
    alive = true
