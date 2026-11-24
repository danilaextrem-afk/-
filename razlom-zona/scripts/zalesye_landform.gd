extends Node3D
## The central road, doors and existing flat gameplay strip remain unchanged.
const DETAIL := preload("res://assets/zalesye/zalesye_landform.gltf")
const LOD := preload("res://assets/zalesye/zalesye_landform_lod.gltf")

static func height_at(x: float, z: float) -> float:
    if absf(x) < 5.0 or absf(x) > 35.0 or z < -48.0 or z > 6.0:
        return 0.0
    # Same height function and grid triangulation as build_zalesye_landform.cjs.
    var gx := floorf((x + 35.0) / 2.0) * 2.0 - 35.0
    var gz := floorf((z + 48.0) / 2.0) * 2.0 - 48.0
    gx = minf(gx, 33.0)
    gz = minf(gz, 4.0)
    var u := clampf((x - gx) / 2.0, 0.0, 1.0)
    var v := clampf((z - gz) / 2.0, 0.0, 1.0)
    var a := _height(gx,gz)
    var b := _height(gx+2.0,gz)
    var c := _height(gx,gz+2.0)
    var d := _height(gx+2.0,gz+2.0)
    return a + u * (b-a) + v * (c-a) if u+v <= 1.0 else d + (1.0-u)*(c-d) + (1.0-v)*(b-d)

static func _height(x: float, z: float) -> float:
    var envelope := smoothstep(13.0,24.0,absf(x))*smoothstep(-48.0,-43.0,z)*(1.0-smoothstep(0.0,5.0,z))
    var contours := 0.9 + 0.52*sin(z*.15+x*.105) + 0.33*sin(z*.31-x*.17) + 0.18*cos(x*.42+z*.27)
    var clear := smoothstep(4.5,9.0,Vector2(x+22.0,z+36.0).length())
    return .025 + envelope * maxf(.18,contours) * clear

func _ready() -> void:
    name = "ZalesyeLandform"
    var material := ShaderMaterial.new()
    material.shader = preload("res://assets/zalesye/landform.gdshader")
    material.set_shader_parameter("leaf_litter", preload("res://assets/village/leaf_litter.jpg"))
    for low in [false,true]:
        var model := (LOD if low else DETAIL).instantiate() as Node3D
        model.name = "Distant" if low else "Detailed"
        add_child(model)
        for part in model.find_children("*","MeshInstance3D",true,false):
            var mesh := part as MeshInstance3D
            mesh.material_override = material
            if low:
                mesh.visibility_range_begin = 65.0
                mesh.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
            else:
                mesh.visibility_range_end = 65.0
                mesh.create_trimesh_collision()
