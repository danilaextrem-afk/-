extends Node3D
## Cluster trees by 14 m cells so the renderer can cull stands independently.
const SPRUCE = preload("res://assets/zalesye/spruce.glb")
const ALDER = preload("res://assets/zalesye/alder.glb")
const BIRCH = preload("res://assets/zalesye/birch.glb")
const VERGE = preload("res://assets/zalesye/verge.glb")

var cells: Dictionary = {}
var canopy_meshes: Array[Mesh] = []
var rng := RandomNumberGenerator.new()

func _ready() -> void:
    rng.seed = 47081
    var foliage := ShaderMaterial.new()
    foliage.shader = preload("res://assets/zalesye/canopy.gdshader")
    foliage.set_shader_parameter("pigment",preload("res://assets/zalesye/district.png"))
    for packed in [SPRUCE, ALDER, BIRCH, VERGE]:
        var source: Node3D = packed.instantiate()
        var component := source.find_children("*", "MeshInstance3D", true, false)[0] as MeshInstance3D
        var mesh: Mesh = component.mesh.duplicate()
        for i in mesh.get_surface_count():
            var material := mesh.surface_get_material(i) as BaseMaterial3D
            if material != null and material.transparency == BaseMaterial3D.TRANSPARENCY_ALPHA_SCISSOR:
                mesh.surface_set_material(i,foliage)
        canopy_meshes.append(mesh)
        source.free()
    # Hand-placed near silhouettes flank the entrance and the houses.
    for tree in [[-5.8,2.4,2,.91],[5.7,1.8,1,1.07],[-7.8,-13.7,2,.91],
            [6.0,-13.4,1,.79],[-5.8,-25.1,2,.87],[5.4,-23.8,2,.81],
            [-8.3,-36.8,1,.93],[7.4,-37.5,2,1.12]]:
        _place(tree[2], Vector3(tree[0],0,tree[1]), tree[3], true)
    for side in [-1,1]:
        for i in 67:
            var z := rng.randf_range(-55,10)
            var x: float = side * (17.0 + 2.5*sin(z*.19) + rng.randf_range(0,13))
            # Keep the access lane, guardhouse and occupied structures clear.
            var world := to_global(Vector3(x,0,z))
            if world.x > 10 and world.z < 0 and world.z > -26:
                continue
            _place(rng.randi_range(0,2), Vector3(x,0,z), rng.randf_range(.65,1.26), false)
    for i in 90:
        var x := rng.randf_range(-38,40)
        var z := rng.randf_range(-80,-60)
        _place(0 if i%3 else 1, Vector3(x,0,z), rng.randf_range(.68,1.26), false)
    # Follow bends in the verge and leave the pedestrian openings clear.
    for i in 169:
        var side := -1 if i%2 else 1
        var z := rng.randf_range(-45,4)
        var x: float = side*(3.15+.44*sin(z*.32)+rng.randf_range(0,1.35))
        if x > 0 and z > -8 and z < -3:
            continue
        _place(3,Vector3(x,.055,z),rng.randf_range(.65,1.37),false)
    # Access lane beyond the checkpoint stays in world space.
    for coord in [Vector3(13,0,-4),Vector3(11,0,-7),Vector3(13,0,-10),
            Vector3(11.8,0,-15),Vector3(10,0,-18),Vector3(13,0,-22),
            Vector3(23,0,-3),Vector3(31,0,-5),Vector3(32,0,-15),Vector3(32,0,-21)]:
        _place(rng.randi_range(0,2),to_local(coord),rng.randf_range(.91,1.20),true)
    for key in cells:
        var batch: Array = cells[key]
        var multimesh := MultiMesh.new()
        multimesh.transform_format = MultiMesh.TRANSFORM_3D
        multimesh.mesh = canopy_meshes[key.z]
        multimesh.instance_count = batch.size()
        for index in batch.size():
            multimesh.set_instance_transform(index,batch[index])
        var patch := MultiMeshInstance3D.new()
        patch.multimesh = multimesh
        patch.visibility_range_end = 36.0 if key.z == 3 else 111.0
        patch.visibility_range_end_margin = 3.0
        if key.z == 3:
            patch.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
        add_child(patch)
    cells.clear()

func _place(species: int, origin: Vector3, factor: float, solid: bool) -> void:
    var key := Vector3i(floori(origin.x/14),floori(origin.z/14),species)
    if not cells.has(key):
        cells[key] = []
    var basis := Basis(Vector3.UP,rng.randf_range(-PI,PI)).scaled(Vector3.ONE*factor)
    cells[key].append(Transform3D(basis,origin))
    if solid:
        var trunk := StaticBody3D.new()
        var cylinder := CylinderShape3D.new()
        cylinder.radius = .19*factor
        cylinder.height = 3.1*factor
        var collider := CollisionShape3D.new()
        collider.shape = cylinder
        trunk.add_child(collider)
        trunk.position = origin + Vector3.UP*cylinder.height*.5
        add_child(trunk)
