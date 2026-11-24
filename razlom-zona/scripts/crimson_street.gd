extends Node3D

const STREET := preload("res://assets/village/street.glb")

# Only two lamps flicker; no additional dynamic lights or shadow casters.
var unstable_lamps: Array[OmniLight3D] = []
var quarter := preload("res://scripts/zalesye_quarter.gd").new()
var lamp_clock := 0.0
var lamp_filaments: Array[MeshInstance3D] = []

func _process(delta: float) -> void:
    lamp_clock += delta
    for i in unstable_lamps.size():
        var lamp := unstable_lamps[i]
        if not is_instance_valid(lamp):
            continue
        var phase := lamp_clock * (2.3 + i * 0.37) + i * 2.1
        var tremor := sin(phase) * 0.12 + sin(phase * 4.7) * 0.045
        # Occasional short brownout, but never a full black frame.
        var brownout := 0.48 if sin(phase * 0.31) > 0.985 else 1.0
        lamp.light_energy = 2.5 * clampf((1.0 + tremor) * brownout, 0.35, 1.2)
        # The actual bulb visibly dims with the light; no extra light sources.
        var filament_material := lamp_filaments[i].material_override as StandardMaterial3D
        filament_material.emission_energy_multiplier = 0.8 + lamp.light_energy * 0.9


func _ready() -> void:
    var bus := preload("res://assets/transport/last_bus.glb").instantiate() as Node3D
    bus.position = Vector3(-3.95, .03, -7.1)
    bus.rotation.y = -.40
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
    mud.set_shader_parameter("sky_map", preload("res://assets/zalesye/evening.jpg"))
    surface.material_override = mud
    surface.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
    add_child(surface)
    quarter.name = "PostalQuarter"
    add_child(quarter)
    var shed := Transform3D(Basis(Vector3.UP,1.06), Vector3(-8,0,-7.5))
    for piece in [[Vector3(0,1.62,-2.35),Vector3(6.4,3,.16)],
            [Vector3(-3.08,1.5,0),Vector3(.18,2.8,4.7)],
            [Vector3(-2.35,1.54,2.16),Vector3(1.5,2.6,.18)],
            [Vector3(.72,1.6,2.14),Vector3(.21,3,.21)],
            [Vector3(3.08,1.6,2.14),Vector3(.21,3,.21)],
            [Vector3(.25,.58,-1.54),Vector3(3.03,1.16,.71)]]:
        _solid(shed * (piece[0] as Vector3), piece[1], 1.06)
    _solid(Vector3(-3.95, 1.36, -7.1), Vector3(2.25, 2.72, 6.30), -.40)
    for x in [-4.25, 4.25]:
        for segment in [[-3, 2], [-15, -10], [-26, -21]]:
            _solid(Vector3(x, .57, (segment[0]+segment[1])*.5), Vector3(.18, 1.14, segment[1]-segment[0]), 0.0)
    for p in [Vector3(-4.72, 2.34, -4.95), quarter.post.transform * Vector3(1.06,2.33,3.0), quarter.post.transform * Vector3(-1,2.48,-.60), Vector3(6.9,2.24,-17.65)]:
        var light := OmniLight3D.new()
        light.position = p
        light.light_color = Color("ffad58")
        light.light_energy = 2.5
        light.omni_range = 5.0
        light.shadow_enabled = false
        add_child(light)
        var bulb := MeshInstance3D.new()
        var bulb_mesh := SphereMesh.new()
        bulb_mesh.radius = 0.065
        bulb_mesh.height = 0.13
        bulb_mesh.radial_segments = 8
        bulb_mesh.rings = 4
        bulb.mesh = bulb_mesh
        bulb.position = p
        bulb.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
        var glass := StandardMaterial3D.new()
        glass.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
        glass.albedo_color = Color("ffca78")
        glass.emission_enabled = true
        glass.emission = Color("ffb45d")
        glass.emission_energy_multiplier = 3.05
        bulb.material_override = glass
        add_child(bulb)
        if unstable_lamps.size() < 2:
            unstable_lamps.append(light)
            lamp_filaments.append(bulb)

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
