extends Node3D
## Separate raised forest embankments in world coordinates, leaving access
## road, guardhouse, bunker ramp, and the central path unmodified.
const DETAIL := preload("res://assets/zalesye/forest_access_landform.gltf")
const LOD := preload("res://assets/zalesye/forest_access_landform_lod.gltf")

static func _height(x: float, z: float) -> float:
    var left := x <= 14.0
    var edge := 1.0-smoothstep(10.0,14.0,x) if left else smoothstep(31.0,39.0,x)
    var along := smoothstep(-36.0,-30.0,z)*(1.0-smoothstep(-9.0,-3.0,z))
    var undulate := 1.15+.56*sin(z*.19+x*.28)+.38*cos(z*.39-x*.16)
    var limit := 1.0-smoothstep(1.4,3.7,Vector2(x-12.0,z+13.5).length()) if left else 0.0
    return .018+maxf(.18,undulate)*edge*along*(1.0-limit*.93)

static func height_at(x: float, z: float) -> float:
    if z < -36.0 or z > -3.0 or not ((x >= 9.0 and x <= 14.0) or (x >= 31.0 and x <= 44.0)):
        return 0.0
    var gx := minf(floorf(x),13.0 if x <= 14.0 else 43.0)
    var gz := minf(floorf(z),-4.0)
    var u := x-gx
    var v := z-gz
    var a := _height(gx,gz)
    var b := _height(gx+1.0,gz)
    var c := _height(gx,gz+1.0)
    var d := _height(gx+1.0,gz+1.0)
    return a+u*(b-a)+v*(c-a) if u+v<=1.0 else d+(1.0-u)*(c-d)+(1.0-v)*(b-d)

func _ready() -> void:
    name = "ForestAccessLandform"
    var material := ShaderMaterial.new()
    material.shader = preload("res://assets/zalesye/landform.gdshader")
    material.set_shader_parameter("leaf_litter",preload("res://assets/village/leaf_litter.jpg"))
    for low in [false,true]:
        var model := (LOD if low else DETAIL).instantiate() as Node3D
        model.name = "Distant" if low else "Detailed"
        add_child(model)
        for part in model.find_children("*","MeshInstance3D",true,false):
            var mesh := part as MeshInstance3D
            mesh.material_override = material
            if low:
                mesh.visibility_range_begin = 62.0
                mesh.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
            else:
                mesh.visibility_range_end = 62.0
                mesh.create_trimesh_collision()
