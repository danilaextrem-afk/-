extends SceneTree
## Real physics-frame traversal from settlement to G-4. No teleport between waypoints.
func _initialize() -> void:
    run.call_deferred()

func run() -> void:
    var game = load("res://scenes/main.tscn").instantiate()
    root.add_child(game)
    for enemy in game.enemies:
        enemy.queue_free()
    var player: CharacterBody3D = game.player
    player.position = Vector3(-5.0, 0.25, 10.8)
    var route = game.get_node_or_null("VillageForestCheckpointRoute")
    if route == null:
        push_error("Route not instantiated")
        quit(1)
        return
    var points: Array[Vector3] = [
        Vector3(8, 0, 2), Vector3(18, 0, -6),
        Vector3(18, 0, -12), Vector3(24, 0, -18)
    ]
    var frames := 0
    for target in points:
        while Vector2(player.position.x - target.x, player.position.z - target.z).length() > 0.80:
            if frames > 1800:
                push_error("Route timed out near %s; target %s" % [player.position, target])
                game._touch_up("forward")
                quit(1)
                return
            var delta: Vector3 = target - player.position
            player.rotation.y = atan2(-delta.x, -delta.z)
            game._touch_down("forward")
            await physics_frame
            frames += 1
        game._touch_up("forward")
        await physics_frame
        if player.position.y < -0.2:
            push_error("Player fell through surface before G-4")
            quit(1)
            return
    if player.position.distance_to(Vector3(24, 0, -18)) > 1.8:
        push_error("Checkpoint route ended too far from G-4")
        quit(1)
        return
    if route.get_node_or_null("CheckpointRoof") == null or route.get_node_or_null("RaisedBarrier") == null:
        push_error("Checkpoint geometry missing")
        quit(1)
        return
    print("PASS village -> forest road -> checkpoint -> G-4 approach, physics frames ", frames)
    quit()
