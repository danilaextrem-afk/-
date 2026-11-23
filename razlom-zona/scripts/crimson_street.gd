extends Node3D

const STREET := preload("res://assets/village/street.glb")

func _ready() -> void:
    var bus := preload("res://assets/transport/last_bus.glb").instantiate() as Node3D
    bus.position = Vector3(-3.95, .03, -7.1)
    bus.rotation.y = .70
    add_child(bus)
    var street_scene: Node3D = STREET.instantiate()
    add_child(street_scene)
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
        [7.5, -6.4, -.68, 6.5, 4.16],
        [-9.3, -20.0, 1.35, 6.9, 5.7], [8.5, -17.0, -1.35, 4.9, 5.1],
        [-6.8, -33.0, 0.55, 5.4, 4.7], [7.1, -28.0, -1.57, 6.2, 5.0]
    ]:
        _solid(Vector3(house[0], 1.5, house[1]), Vector3(house[3], 3.0, house[4]), house[2])
    var shed := Transform3D(Basis(Vector3.UP,1.06), Vector3(-8,0,-7.5))
    for piece in [[Vector3(0,1.62,-2.35),Vector3(6.4,3,.16)],
            [Vector3(-3.08,1.5,0),Vector3(.18,2.8,4.7)],
            [Vector3(-2.35,1.54,2.16),Vector3(1.5,2.6,.18)],
            [Vector3(.72,1.6,2.14),Vector3(.21,3,.21)],
            [Vector3(3.08,1.6,2.14),Vector3(.21,3,.21)],
            [Vector3(.25,.58,-1.54),Vector3(3.03,1.16,.71)]]:
        _solid(shed * (piece[0] as Vector3), piece[1], 1.06)
    _solid(Vector3(-3.95, 1.36, -7.1), Vector3(2.25, 2.72, 6.30), .70)
    for x in [-4.25, 4.25]:
        for segment in [[-3, 2], [-15, -10], [-26, -21]]:
            _solid(Vector3(x, .57, (segment[0]+segment[1])*.5), Vector3(.18, 1.14, segment[1]-segment[0]), 0.0)
    for p in [Vector3(-4.72, 2.34, -4.95), Vector3(4.08, 2.1, -5.9), Vector3(7.25, 2.1, -3.03), Vector3(-5.0, 2.13, -30.5)]:
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
