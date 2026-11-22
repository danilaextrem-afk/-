extends SceneTree
## Headless proof that the actual hero landmark is real 3D geometry,
## part of the playable world, with a walkable central passage.
func _initialize() -> void:
    verify.call_deferred()

func verify() -> void:
    var game = load("res://scenes/main.tscn").instantiate()
    root.add_child(game)
    var gate: Node3D = game.get_node_or_null("ZONA07_RiftCheckpoint_Hero")
    if gate == null:
        push_error("Missing original rift checkpoint in playable scene")
        quit(1)
        return
    var street: Node3D = game.get_node_or_null("CrimsonStreet")
    if street == null or int(street.get_meta("water_tower_triangles_filtered", 0)) < 20:
        push_error("Obstructing original water tower still visible in scene")
        quit(1)
        return
    var meshes := gate.find_children("*", "MeshInstance3D", true, false)
    if meshes.size() < 140:
        push_error("Rift checkpoint is not real detailed 3D geometry: " + str(meshes.size()))
        quit(1)
        return
    for name in ["SolidPylon_-1", "SolidPylon_1", "SolidLintel"]:
        if gate.get_node_or_null(name) == null:
            push_error("Missing collision proxy " + name)
            quit(1)
            return
    if gate.get_node_or_null("PixelFracturePlane_NonCollision") == null:
        push_error("Missing real shader portal plane")
        quit(1)
        return
    var text: Label3D = gate.get_node_or_null("SignZONA07") as Label3D
    if text == null or text.text != "ЗОНА-07":
        push_error("Missing authored scene signage")
        quit(1)
        return
    var p = game.player
    p.position = Vector3(6.8, 0.12, 2.0)
    p.rotation.y = -PI / 2.0
    for frame in 30:
        await physics_frame
    if p.global_position.y < -0.15:
        push_error("Checkpoint approach ground not walkable")
        quit(1)
        return
    print("PASS ZONA07 checkpoint: ", meshes.size(), " real mesh instances, shader, signage and colliders")
    quit(0)
