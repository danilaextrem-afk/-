extends Node3D
## Abandoned base perimeter: deliberately outside the existing access route.
const INTACT := preload("res://assets/base/base_fence.gltf")
const INTACT_LOD := preload("res://assets/base/base_fence_lod.gltf")
const DAMAGED := preload("res://assets/base/base_fence_damaged.gltf")
const DAMAGED_LOD := preload("res://assets/base/base_fence_damaged_lod.gltf")

func _ready() -> void:
    name = "BasePerimeter"
    var finish := ShaderMaterial.new()
    finish.shader = preload("res://assets/zalesye/architecture_weathered.gdshader")
    finish.set_shader_parameter("district",preload("res://assets/zalesye/district.png"))
    finish.set_shader_parameter("plaster_tint",Color("c4c9bc"))
    finish.set_shader_parameter("building_seed",48.0)
    _section("FrontIntact",Vector3(24.0,0,-5.2),0.0,false,finish)
    _section("FrontCollapsed",Vector3(28.5,0,-5.2),0.0,true,finish)
    _section("EastIntact",Vector3(30.1,0,-8.2),PI*.5,false,finish)
    _section("EastCollapsed",Vector3(30.1,0,-11.8),PI*.5,true,finish)

func _section(label: String, at: Vector3, yaw: float, damaged: bool, finish: ShaderMaterial) -> void:
    var section := Node3D.new()
    section.name = label
    section.position = at
    section.rotation.y = yaw
    add_child(section)
    for low in [false,true]:
        var packed: PackedScene
        if damaged:
            packed = DAMAGED_LOD if low else DAMAGED
        else:
            packed = INTACT_LOD if low else INTACT
        var model := packed.instantiate() as Node3D
        model.name = "LOD" if low else "Detail"
        section.add_child(model)
        for part in model.find_children("*","MeshInstance3D",true,false):
            var mesh := part as MeshInstance3D
            mesh.material_override = finish
            if low:
                mesh.visibility_range_begin = 28.0
                mesh.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
            else:
                mesh.visibility_range_end = 28.0
    for x in [-1.47,1.47]:
        _collider(section,Vector3(x,1.1,0),Vector3(.36,2.2,.36))
    if damaged:
        # The twisted mesh is low, but still a physical obstacle.
        _collider(section,Vector3(0,.43,.40),Vector3(2.55,.86,.83))
    else:
        _collider(section,Vector3(0,1.04,0),Vector3(2.66,2.0,.12))

func _collider(section: Node3D, center: Vector3, dimensions: Vector3) -> void:
    var body := StaticBody3D.new()
    var shape := BoxShape3D.new()
    shape.size = dimensions
    var collision := CollisionShape3D.new()
    collision.shape = shape
    collision.position = center
    body.add_child(collision)
    section.add_child(body)
