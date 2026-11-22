extends Node3D

const STREET := preload("res://assets/village/street.glb")

func _ready() -> void:
    var street_scene: Node3D = STREET.instantiate()
    add_child(street_scene)
    # The inherited GLB contains a 10.75 m water tank precisely behind
    # the hero checkpoint. Remove only tower triangles by local geometry
    # coordinates, retaining authored houses, trees, road and atlas.
    # This one-time mesh filter is a WIP replacement for offline re-export.
    for model in street_scene.find_children("*", "MeshInstance3D", true, false):
        _strip_tower_triangles(model)


func _strip_tower_triangles(node: MeshInstance3D) -> void:
    var source := node.mesh as ArrayMesh
    if source == null:
        return
    var rebuilt := ArrayMesh.new()
    var removed: int = 0
    for index in range(source.get_surface_count()):
        var surface_arrays := source.surface_get_arrays(index)
        var positions: PackedVector3Array = surface_arrays[Mesh.ARRAY_VERTEX]
        var input_ids: PackedInt32Array = surface_arrays[Mesh.ARRAY_INDEX]
        if input_ids.is_empty():
            # Imported GLB should be indexed; preserve any unusual surface.
            rebuilt.add_surface_from_arrays(Mesh.PRIMITIVE_TRIANGLES, surface_arrays)
            rebuilt.surface_set_material(rebuilt.get_surface_count()-1, source.surface_get_material(index))
            continue
        var kept := PackedInt32Array()
        for k in range(0, input_ids.size(), 3):
            var a: Vector3 = positions[input_ids[k]]
            var b: Vector3 = positions[input_ids[k+1]]
            var d: Vector3 = positions[input_ids[k+2]]
            var center: Vector3 = (a+b+d)/3.0
            var tower := center.x > -2.2 and center.x < 3.4 and center.z > -51.6 and center.z < -46.4
            if tower:
                removed += 1
            else:
                kept.append_array(PackedInt32Array([input_ids[k], input_ids[k+1], input_ids[k+2]]))
        if kept.is_empty():
            continue
        surface_arrays[Mesh.ARRAY_INDEX] = kept
        rebuilt.add_surface_from_arrays(Mesh.PRIMITIVE_TRIANGLES, surface_arrays)
        rebuilt.surface_set_material(rebuilt.get_surface_count()-1, source.surface_get_material(index))
    if removed > 0:
        node.mesh = rebuilt
        set_meta("water_tower_triangles_filtered", removed)
        print("ZONA street: filtered legacy water tower triangles: ", removed)

    var surface := MeshInstance3D.new()
    var plane := PlaneMesh.new()
    plane.size = Vector2(9.4, 49.0)
    surface.mesh = plane
    surface.position = Vector3(0, 0.071, -20.5)
    var mud := ShaderMaterial.new()
    mud.shader = preload("res://assets/village/road.gdshader")
    mud.set_shader_parameter("ground_map", preload("res://assets/village/road.png"))
    mud.set_shader_parameter("sky_map", preload("res://assets/village/storm.jpg"))
    surface.material_override = mud
    surface.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
    add_child(surface)
    # House centres and yaw match the baked mesh. Collision excludes grass and cables.
    for house in [
        [-7.6, -6.0, 0.97, 5.6, 5.5], [8.2, -4.0, -1.07, 5.8, 5.2],
        [-9.3, -20.0, 1.35, 6.9, 5.7], [8.5, -17.0, -1.35, 4.9, 5.1],
        [-6.8, -33.0, 0.55, 5.4, 4.7], [7.1, -28.0, -1.57, 6.2, 5.0]
    ]:
        _solid(Vector3(house[0], 1.5, house[1]), Vector3(house[3], 3.0, house[4]), house[2])
    _solid(Vector3(-3.4, .67, -5.7), Vector3(1.6, 1.34, 3.55), .33)
    for x in [-4.25, 4.25]:
        for segment in [[-3, 2], [-15, -10], [-26, -21]]:
            _solid(Vector3(x, .57, (segment[0]+segment[1])*.5), Vector3(.18, 1.14, segment[1]-segment[0]), 0.0)
    for p in [Vector3(5.53, 2.13, -2.3), Vector3(-5.0, 2.13, -30.5)]:
        var light := OmniLight3D.new()
        light.position = p
        light.light_color = Color("ffad58")
        light.light_energy = 2.5
        light.omni_range = 5.0
        add_child(light)

func _solid(center: Vector3, size: Vector3, yaw: float) -> void:
    var body := StaticBody3D.new()
    body.position = center
    body.rotation.y = yaw
    var shape := BoxShape3D.new()
    shape.size = size
    var collision := CollisionShape3D.new()
    collision.shape = shape
    body.add_child(collision)
    add_child(body)
