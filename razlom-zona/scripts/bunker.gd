extends Node3D
## Each bay ends one metre below its entry socket. Collision stays original.
const BAY := preload("res://assets/bunker/gallery.gltf")
const WEATHER := preload("res://assets/bunker/gallery_weathered.gdshader")
const ATLAS := preload("res://assets/bunker/gallery_atlas.png")
const BAY_COUNT := 5
const TINTS := [Color("c3cbc0"), Color("abb3a9"), Color("939c94"), Color("b7aaa0"), Color("a59a88")]
const LAMP_TINTS := [Color("d0dec9"), Color("b3c6b8"), Color("a3c2be"), Color("a9a08f"), Color("e8b06b")]
const POWERS := [0.85, 0.0, 0.55, 0.0, 0.72]
var emergency_lamp: OmniLight3D
var emergency_material: ShaderMaterial
var elapsed := 0.0

func _ready() -> void:
    for index in BAY_COUNT:
        var bay := BAY.instantiate() as Node3D
        bay.name = "GalleryBay_%02d" % index
        bay.position = Vector3(0, -index, -4 * index)
        add_child(bay)
        var weather := ShaderMaterial.new()
        weather.shader = WEATHER
        weather.set_shader_parameter("atlas", ATLAS)
        weather.set_shader_parameter("section_id", float(index))
        weather.set_shader_parameter("section_tint", TINTS[index])
        weather.set_shader_parameter("lamp_tint", LAMP_TINTS[index])
        weather.set_shader_parameter("lamp_power", POWERS[index])
        for child in bay.find_children("*", "MeshInstance3D", true, false):
            var mesh := child as MeshInstance3D
            mesh.material_override = weather
            mesh.create_trimesh_collision()
        # Different service functions distinguish the otherwise identical bays.
        match index:
            0:
                _equipment(bay, "Switchgear", preload("res://assets/bunker/g4_switchgear.gltf"), preload("res://assets/bunker/g4_switchgear_lod.gltf"), -1.3, 1.25, Vector3(1.22,1.22,.27))
            1:
                _equipment(bay, "ValveManifold", preload("res://assets/bunker/g4_valve.gltf"), preload("res://assets/bunker/g4_valve_lod.gltf"), -1.5, 1.30, Vector3(1.36,.91,.30))
            3:
                _equipment(bay, "Ventilation", preload("res://assets/bunker/g4_vent.gltf"), preload("res://assets/bunker/g4_vent_lod.gltf"), -1.2, 1.40, Vector3(1.24,.92,.30))
        if index % 2 == 0:
            var lamp := OmniLight3D.new()
            lamp.name = "ServiceLight"
            # Matches the luminous panel in the existing sloped gallery mesh.
            lamp.position = Vector3(0.49, 2.24, -1.2)
            lamp.light_color = LAMP_TINTS[index]
            lamp.light_energy = 1.55 if index == 0 else (1.05 if index == 2 else 1.4)
            lamp.omni_range = 5.2
            lamp.shadow_enabled = false
            bay.add_child(lamp)
            if index == 4:
                emergency_lamp = lamp
                emergency_material = weather

func _process(delta: float) -> void:
    if not is_instance_valid(emergency_lamp):
        return
    elapsed += delta
    # A slowly failing emergency circuit at the radio post, never full dark.
    var pulse := 0.85 + 0.15 * sin(elapsed * 2.6)
    emergency_lamp.light_energy = 1.4 * pulse
    emergency_material.set_shader_parameter("lamp_power", 0.72 * pulse)

func _equipment(bay: Node3D, label: String, near_scene: PackedScene, far_scene: PackedScene, z: float, height: float, size: Vector3) -> void:
    var mount := Node3D.new()
    mount.name = label
    # Right wall, away from the old return pipes and radio cabinet. The
    # centre line remains clear for the player's existing capsule.
    mount.position = Vector3(1.77,height+z*.25,z)
    mount.rotation.y = -PI*.5
    bay.add_child(mount)
    var finish := ShaderMaterial.new()
    finish.shader = preload("res://assets/zalesye/architecture_weathered.gdshader")
    finish.set_shader_parameter("district",preload("res://assets/zalesye/district.png"))
    finish.set_shader_parameter("building_seed",height*14.0+z)
    for low in [false,true]:
        var model := (far_scene if low else near_scene).instantiate() as Node3D
        model.name = "LOD" if low else "Detail"
        mount.add_child(model)
        for part in model.find_children("*","MeshInstance3D",true,false):
            var mesh := part as MeshInstance3D
            mesh.material_override = finish
            if low:
                mesh.visibility_range_begin = 9.0
                mesh.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
            else:
                mesh.visibility_range_end = 9.0
    var body := StaticBody3D.new()
    body.name = "EquipmentCollision"
    var collider := CollisionShape3D.new()
    var shape := BoxShape3D.new()
    shape.size = size
    collider.shape = shape
    collider.position.z = size.z*.5
    body.add_child(collider)
    mount.add_child(body)
