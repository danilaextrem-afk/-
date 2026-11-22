extends CharacterBody3D
## Lightweight wander/chase enemy. Deliberately NOT a boss.
var player
var origin: Vector3
var creature_type: int = 0
var attack_timer: float = 0.0
var wander_time: float = 0.0
var wander_direction: Vector3 = Vector3.ZERO

func _ready() -> void:
    origin = global_position
    var collision := CollisionShape3D.new()
    var capsule := CapsuleShape3D.new()
    capsule.radius = 0.47
    capsule.height = 1.6
    collision.shape = capsule
    collision.position.y = 0.8
    add_child(collision)
    _build_visual()

func _build_visual() -> void:
    var body := MeshInstance3D.new()
    var mesh := BoxMesh.new()
    mesh.size = Vector3(0.9, 1.5, 0.65) if creature_type == 0 else Vector3(1.2, 0.9, 1.4)
    body.mesh = mesh
    body.position.y = 0.8 if creature_type == 0 else 0.48
    var mat := StandardMaterial3D.new()
    mat.albedo_color = Color("455151") if creature_type == 0 else Color("654150")
    mat.roughness = 1.0
    body.material_override = mat
    add_child(body)
    for side in [-1, 1]:
        var eye := MeshInstance3D.new()
        var em := BoxMesh.new()
        em.size = Vector3(0.12, 0.10, 0.08)
        eye.mesh = em
        eye.position = Vector3(side * 0.24, 1.21 if creature_type == 0 else 0.65, -0.36 if creature_type == 0 else -0.72)
        var eye_mat := StandardMaterial3D.new()
        eye_mat.albedo_color = Color("7eecd6") if creature_type == 0 else Color("ff6679")
        eye_mat.emission_enabled = true
        eye_mat.emission = eye_mat.albedo_color
        eye.material_override = eye_mat
        add_child(eye)

func _physics_process(delta: float) -> void:
    if not is_instance_valid(player) or not player.alive:
        return
    attack_timer = maxf(0.0, attack_timer - delta)
    var to_player: Vector3 = player.global_position - global_position
    var flat := Vector3(to_player.x, 0.0, to_player.z)
    var distance := flat.length()
    var direction := Vector3.ZERO
    var chase_distance := 12.0 if creature_type == 0 else 9.0
    if distance < chase_distance and distance > 1.35:
        direction = flat.normalized()
        look_at(global_position + direction, Vector3.UP)
    elif distance >= chase_distance:
        wander_time -= delta
        if wander_time <= 0.0:
            wander_time = randf_range(2.0, 4.0)
            var angle := randf_range(-PI, PI)
            wander_direction = Vector3(cos(angle), 0.0, sin(angle))
        if global_position.distance_to(origin) < 7.0:
            direction = wander_direction
        else:
            direction = (origin - global_position).normalized()
    velocity.x = direction.x * (2.6 if creature_type == 0 else 3.5)
    velocity.z = direction.z * (2.6 if creature_type == 0 else 3.5)
    velocity.y -= 18.0 * delta
    move_and_slide()
    if distance < 1.65 and attack_timer <= 0.0:
        player.hit(12.0 if creature_type == 0 else 8.0)
        attack_timer = 1.8
