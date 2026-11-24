extends CharacterBody3D
## A grounded watchman encounter with a readable strike wind-up.
var player
var origin: Vector3
var creature_type: int = 0
var attack_timer: float = 0.0
var windup := 0.0
var wander_time: float = 0.0
var wander_direction: Vector3 = Vector3.ZERO
var model: Node3D
var limbs: Array[Node3D] = []
var distant_model: Node3D
var near_joints: Dictionary = {}
var far_joints: Dictionary = {}
const JOINT_NAMES := ["ArmL","ArmR","LegL","LegR","ForearmL","ForearmR","ShinL","ShinR","Head"]
var phase := 0.0
var stride_blend := 0.0
var strike_pose := 0.0
const REACH := 1.65
const WINDUP_TIME := 0.46
const RECOVERY_TIME := 1.34

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
    model = preload("res://assets/transport/watchman_rework.gltf").instantiate() as Node3D
    model.name = "WatchmanDetail"
    add_child(model)
    distant_model = preload("res://assets/transport/watchman_rework_lod.gltf").instantiate() as Node3D
    distant_model.name = "WatchmanLOD"
    add_child(distant_model)
    for name_part in JOINT_NAMES:
        near_joints[name_part] = model.find_child(name_part,true,false) as Node3D
        far_joints[name_part] = distant_model.find_child(name_part,true,false) as Node3D
    for name_part in ["ArmL","ArmR","LegL","LegR"]:
        limbs.append(near_joints[name_part])
    # Switch the whole model together, so individual articulated pieces cannot
    # disappear at slightly different distances from the camera.
    distant_model.visible = false

func animate(delta: float, pace: float) -> void:
    stride_blend = move_toward(stride_blend, minf(pace / 2.6, 1.0), delta * 5.5)
    # Distance-driven cycle stops when blocked; avoid sliding feet and idle marching.
    phase = fmod(phase + delta * pace * TAU / 1.65, TAU)
    var motion := sin(phase) * .46 * stride_blend
    var breathe := sin(Time.get_ticks_msec() * .002) * .012
    var anticipation := 1.0 - windup / WINDUP_TIME if windup > 0.0 else 0.0
    var target_pose := sin(anticipation * PI * .5) if windup > 0.0 else 0.0
    if attack_timer > RECOVERY_TIME - .19 and windup <= 0.0:
        target_pose = -.48
    strike_pose = move_toward(strike_pose, target_pose, delta * 8.0)
    if limbs.size() == 4 and is_instance_valid(limbs[0]) and is_instance_valid(limbs[1]) and is_instance_valid(limbs[2]) and is_instance_valid(limbs[3]):
        limbs[0].rotation.x = -.16 + motion - maxf(strike_pose,0.0)*.9
        limbs[1].rotation.x = -.28 - motion - strike_pose*1.42
        limbs[0].rotation.z = -.07 - maxf(strike_pose,0.0)*.17
        limbs[1].rotation.z = .08 + maxf(strike_pose,0.0)*.25
        limbs[2].rotation.x = -motion
        limbs[3].rotation.x = motion
    model.position.y = absf(sin(phase))*.025*stride_blend + breathe
    model.rotation.x = lerpf(model.rotation.x, .08*stride_blend - strike_pose*.13, minf(1.0,delta*8.0))
    model.rotation.z = sin(phase)*.025*stride_blend
    var knee_l := maxf(0.0, sin(phase)) * .65 * stride_blend
    var knee_r := maxf(0.0, -sin(phase)) * .65 * stride_blend
    if is_instance_valid(near_joints.get("ShinL")):
        near_joints["ShinL"].rotation.x = knee_l
        near_joints["ShinR"].rotation.x = knee_r
        near_joints["ForearmL"].rotation.x = -.18 - maxf(strike_pose,0.0)*.36 - .13*stride_blend
        near_joints["ForearmR"].rotation.x = -.24 - maxf(strike_pose,0.0)*.73 - .16*stride_blend
        near_joints["Head"].rotation.x = -.07*stride_blend + strike_pose*.09
    for joint_name in JOINT_NAMES:
        if is_instance_valid(near_joints.get(joint_name)) and is_instance_valid(far_joints.get(joint_name)):
            far_joints[joint_name].rotation = near_joints[joint_name].rotation
    distant_model.transform = model.transform
    var observer := get_viewport().get_camera_3d()
    if is_instance_valid(observer):
        var d := observer.global_position.distance_to(global_position)
        var show_far := d > (21.0 if model.visible else 19.0)
        model.visible = not show_far
        distant_model.visible = show_far

func _can_reach() -> bool:
    if not is_instance_valid(player) or not player.alive:
        return false
    var offset: Vector3 = player.global_position - global_position
    if absf(offset.y) > .85 or Vector2(offset.x,offset.z).length() > REACH:
        return false
    return _has_line_of_sight()

func _has_line_of_sight() -> bool:
    var query := PhysicsRayQueryParameters3D.create(global_position + Vector3.UP*1.05, player.global_position + Vector3.UP*.85)
    query.exclude = [get_rid()]
    var hit := get_world_3d().direct_space_state.intersect_ray(query)
    return hit.is_empty() or hit.get("collider") == player

func _physics_process(delta: float) -> void:
    if not is_instance_valid(player) or not player.alive:
        return
    attack_timer = maxf(0.0, attack_timer - delta)
    var to_player: Vector3 = player.global_position - global_position
    var flat := Vector3(to_player.x, 0.0, to_player.z)
    var distance := flat.length()
    var direction := Vector3.ZERO
    var chase_distance := 12.0 if creature_type == 0 else 9.0
    var can_see := distance < chase_distance and absf(to_player.y) < 2.0 and _has_line_of_sight()
    if windup > 0.0:
        windup = maxf(0.0, windup - delta)
        if windup <= 0.0:
            if _can_reach():
                player.hit(12.0 if creature_type == 0 else 8.0)
            attack_timer = RECOVERY_TIME
    elif attack_timer <= 0.0 and can_see and _can_reach():
        windup = WINDUP_TIME
    elif can_see and distance > 1.35 and attack_timer < .6:
        direction = flat.normalized()
    elif not can_see and attack_timer <= 0.0:
        wander_time -= delta
        if wander_time <= 0.0:
            wander_time = randf_range(2.0, 4.0)
            var angle := randf_range(-PI, PI)
            wander_direction = Vector3(cos(angle), 0.0, sin(angle))
        if global_position.distance_to(origin) < 7.0:
            direction = wander_direction
        else:
            var home := origin - global_position
            home.y = 0.0
            direction = home.normalized()
    var facing := flat if windup > 0.0 and distance > .05 else direction
    if facing.length_squared() > .001:
        rotation.y = lerp_angle(rotation.y, atan2(-facing.x,-facing.z), minf(1.0,delta*7.0))
    var speed := (2.6 if creature_type == 0 else 3.5) if can_see else 1.25
    velocity.x = move_toward(velocity.x, direction.x * speed, delta*9.0)
    velocity.z = move_toward(velocity.z, direction.z * speed, delta*9.0)
    if windup > 0.0 or attack_timer > .6:
        velocity.x = 0.0
        velocity.z = 0.0
    velocity.y = -.2 if is_on_floor() else velocity.y - 18.0 * delta
    move_and_slide()
    var movement := get_position_delta()
    var pace := Vector2(movement.x,movement.z).length()/maxf(delta,.0001) if is_on_floor() else 0.0
    animate(delta,pace)
