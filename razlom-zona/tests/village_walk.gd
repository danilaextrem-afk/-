extends SceneTree

func _initialize() -> void:
    walk.call_deferred()

func walk() -> void:
    if FileAccess.file_exists("user://razlom_zona_save_v1.json"):
        push_error("Use a fresh test data directory")
        quit(1)
        return
    var game = load("res://scenes/main.tscn").instantiate()
    root.add_child(game)
    var player = game.player
    game._touch_down("forward")
    for frame in 370:
        await physics_frame
    game._touch_up("forward")
    if player.position.x < -13 or absf(player.position.z - 2) > .6 or not player.is_on_floor():
        push_error("Street obstructed: %s" % player.position)
        quit(1)
        return
    print("PASS new-game walk through Zalesye: ", player.position)
    # Isolated physical encounter over the map, not a change to the real level.
    var enemy = game.enemies[0]
    if enemy.near_joints.size() != 9 or enemy.far_joints.size() != 9:
        push_error("Missing articulated watchman joint map")
        quit(1)
        return
    for joint in enemy.JOINT_NAMES:
        if not is_instance_valid(enemy.near_joints[joint]) or not is_instance_valid(enemy.far_joints[joint]):
            push_error("Missing watchman joint " + joint)
            quit(1)
            return
    enemy.set_physics_process(false)
    player.set_physics_process(false)
    game._box("EncounterTestFloor", Vector3(0,49.5,0), Vector3(12,1,12), game._material("555555"))
    player.position = Vector3(1.45,50.05,0)
    enemy.position = Vector3(0,50.05,0)
    enemy.velocity = Vector3.ZERO
    enemy.attack_timer = 0.0
    enemy.windup = 0.0
    player.restore()
    for frame in 3:
        await physics_frame
    enemy.set_physics_process(true)
    for frame in 12:
        await physics_frame
    if player.health != 100.0 or enemy.windup <= 0.0 or enemy.strike_pose <= 0.0:
        push_error("Watchman failed to telegraph a strike before damage")
        quit(1)
        return
    if not is_equal_approx(enemy.near_joints["ForearmR"].rotation.x, enemy.far_joints["ForearmR"].rotation.x) or enemy.near_joints["ForearmR"].rotation.x >= -.30:
        push_error("Watchman elbow wind-up or LOD synchronization missing")
        quit(1)
        return
    player.position = Vector3(4,50.05,0)
    for frame in 32:
        await physics_frame
    if player.health != 100.0:
        push_error("Watchman strike cannot be dodged")
        quit(1)
        return
    enemy.set_physics_process(false)
    enemy.position = Vector3(0,50.05,0)
    enemy.velocity = Vector3.ZERO
    enemy.attack_timer = 0.0
    enemy.windup = 0.0
    player.position = Vector3(1.45,50.05,0)
    var barrier: Node3D = game._box("EncounterTestWall", Vector3(.73,51,0), Vector3(.16,2,2), game._material("555555"))
    for frame in 3:
        await physics_frame
    if enemy._can_reach():
        push_error("Watchman can strike through a solid wall")
        quit(1)
        return
    barrier.queue_free()
    for frame in 3:
        await physics_frame
    enemy.set_physics_process(true)
    for frame in 35:
        await physics_frame
    if player.health != 88.0:
        push_error("Watchman did not land exactly one readable strike")
        quit(1)
        return
    print("PASS watchman wind-up, dodge, wall occlusion and strike")
    quit()
