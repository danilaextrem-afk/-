extends Node3D

const STREET := preload("res://assets/village/street.glb")

func _ready() -> void:
    add_child(STREET.instantiate())
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
