extends Node3D
## Two original landmarks replace the featureless administration/warehouse boxes.
const ADMIN := preload("res://assets/base/industrial_admin.gltf")
const ADMIN_LOD := preload("res://assets/base/industrial_admin_lod.gltf")
const WAREHOUSE := preload("res://assets/base/industrial_warehouse.gltf")
const WAREHOUSE_LOD := preload("res://assets/base/industrial_warehouse_lod.gltf")

func _ready() -> void:
    name = "IndustrialSkyline"
    _building("Administration", ADMIN, ADMIN_LOD, Vector3(-3,0,-20), Vector3(11,8,9), Color("aeb4ab"), 51.0)
    _building("Warehouse", WAREHOUSE, WAREHOUSE_LOD, Vector3(1,0,29), Vector3(15,8,12), Color("a4aba4"), 69.0)

func _building(label: String, near_scene: PackedScene, far_scene: PackedScene, at: Vector3, dimensions: Vector3, tint: Color, variation: float) -> void:
    var building := Node3D.new()
    building.name = label
    building.position = at
    add_child(building)
    var finish := ShaderMaterial.new()
    finish.shader = preload("res://assets/zalesye/architecture_weathered.gdshader")
    finish.set_shader_parameter("district", preload("res://assets/zalesye/district.png"))
    finish.set_shader_parameter("plaster_tint", tint)
    finish.set_shader_parameter("building_seed", variation)
    for low in [false,true]:
        var model := (far_scene if low else near_scene).instantiate() as Node3D
        model.name = "LOD" if low else "Detail"
        building.add_child(model)
        for part in model.find_children("*","MeshInstance3D",true,false):
            var mesh := part as MeshInstance3D
            mesh.material_override = finish
            if low:
                mesh.visibility_range_begin = 42.0
                mesh.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
            else:
                mesh.visibility_range_end = 42.0
    _solid(building, "Shell", Vector3(0, dimensions.y*.5, 0), dimensions)
    if label == "Administration":
        for side in [-1.0,1.0]:
            _solid(building, "CanopyPost", Vector3(side*1.18,1.2,5.4), Vector3(.18,2.4,.18))
    else:
        for i in range(4):
            _solid(building, "LoadingPost", Vector3(-7.1+float(i)*4.73,2.34,-6.91), Vector3(.18,4.68,.18))

func _solid(building: Node3D, label: String, center: Vector3, size: Vector3) -> void:
    var body := StaticBody3D.new()
    body.name = label
    body.position = center
    var collision := CollisionShape3D.new()
    var shape := BoxShape3D.new()
    shape.size = size
    collision.shape = shape
    body.add_child(collision)
    building.add_child(body)
