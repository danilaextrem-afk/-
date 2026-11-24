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
    if street == null:
        push_error("Missing approach street")
        quit(1)
        return
    var model := load("res://assets/village/street.glb").instantiate() as Node3D
    for instance in model.find_children("*", "MeshInstance3D", true, false):
        var mesh: Mesh = instance.mesh
        for surface in mesh.get_surface_count():
            var positions: PackedVector3Array = mesh.surface_get_arrays(surface)[Mesh.ARRAY_VERTEX]
            for p in positions:
                if p.y > 4.5 and p.x > -2.2 and p.x < 3.4 and p.z > -51.6 and p.z < -46.4:
                    push_error("High geometry obstructs the sightline to checkpoint 07")
                    model.free()
                    quit(1)
                    return
    model.free()
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
    var backdrop := gate.get_node_or_null("AnomalyDarkJaggedDepth_NoCollision") as MeshInstance3D
    var fracture := gate.get_node_or_null("PixelFracturePlane_NonCollision") as MeshInstance3D
    if backdrop == null or fracture == null:
        push_error("Missing fracture layers")
        quit(1)
        return
    var haze := backdrop.material_override as StandardMaterial3D
    var shader_mat := fracture.material_override as ShaderMaterial
    if haze == null or haze.transparency != BaseMaterial3D.TRANSPARENCY_ALPHA or haze.albedo_color.a > 0.15:
        push_error("Opaque rift backdrop hides route beyond checkpoint")
        quit(1)
        return
    if shader_mat == null or shader_mat.shader == null or fracture.cast_shadow != GeometryInstance3D.SHADOW_CASTING_SETTING_OFF:
        push_error("Fracture must remain a non-shadowing visual effect")
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
