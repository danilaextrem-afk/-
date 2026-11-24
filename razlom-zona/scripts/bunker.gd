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
