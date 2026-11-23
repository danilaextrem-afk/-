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
    model = preload("res://assets/transport/watchman.glb").instantiate() as Node3D
    add_child(model)
    for name_part in ["ArmL","ArmR","LegL","LegR"]:
        limbs.append(model.find_child(name_part,true,false) as Node3D)

var model: Node3D
var limbs: Array[Node3D] = []
var phase := 0.0

func animate(delta: float, pace: float) -> void:
    phase = fmod(phase + delta*(2.0+pace*3.5), TAU)
    var motion := sin(phase)*minf(.47,pace*.19)
    limbs[0].rotation.x = -.16+motion
    limbs[1].rotation.x = -.28-motion
    limbs[2].rotation.x = -motion
    limbs[3].rotation.x = motion
    model.position.y = absf(cos(phase))*.019*minf(1,pace)

func _physics_process(delta: float) -> void:
    if not is_instance_valid(player) or not player.alive:
        return
    attack_timer = maxf(0.0, attack_timer - delta)
    var to_player: Vector3 = player.global_position - global_position
    var flat := Vector3(to_player.x, 0.0, to_player.z)
    var distance := flat.length()
    var direction := Vector3.ZERO
    var chase_distance := 12.0 if creature_type == 0 else 9.0
    if distance < chase_distance and distance > 1.35 and attack_timer < 1.20:
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
    animate(delta,Vector2(velocity.x,velocity.z).length())
    if distance < 1.65 and attack_timer <= 0.0:
        player.hit(12.0 if creature_type == 0 else 8.0)
        attack_timer = 1.8
