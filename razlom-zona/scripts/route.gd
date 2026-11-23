extends Node3D
## RZ_ZONA_ROUTE_001: village -> forest road -> industrial checkpoint -> G-4.
## Original deterministic Godot geometry, no third-party asset packs.
## Route is deliberately kept clear for the existing player capsule.
const ROUTE_POINTS := [
    Vector3(-5.0, 0.0, 10.8),
    Vector3(8.0, 0.0, 2.0),
    Vector3(18.0, 0.0, -6.0),
    Vector3(18.0, 0.0, -12.0),
    Vector3(24.0, 0.0, -18.0),
    Vector3(24.0, 0.0, -20.0)
]
const ROAD_WIDTH := 4.2

func _ready() -> void:
    name = "VillageForestCheckpointRoute"
    var soil := _mat("3d493e")
    var asphalt := _mat("353e40")
    var faded := _mat("918b69")
    var steel := _mat("505a59")
    var rust := _mat("8a543e")
    var glass := _mat("263a40")
    var lamp := _mat("8bcac0", true)
    # The original MainRoad and SideRoad remain the walkable colliders.
    # Overlays are visual-only and cannot block the G-4 ramp.
    for i in range(ROUTE_POINTS.size() - 1):
        var a: Vector3 = ROUTE_POINTS[i]
        var b: Vector3 = ROUTE_POINTS[i + 1]
        var direction := b - a
        var center := (a + b) * 0.5
        var segment := _box("RoadMark_%02d" % i, center + Vector3(0, 0.074, 0),
            Vector3(0.09, 0.015, direction.length() * 0.55), faded, false)
        segment.rotation.y = atan2(direction.x, direction.z)
    # Forest edge: trunks outside the road; can be passed on foot.
    for i in range(10):
        var z := -9.0 - float(i) * 2.7
        var x := 13.0 if i % 2 == 0 else 31.0
        _box("ForestTrunk_%02d" % i, Vector3(x, 1.65, z),
            Vector3(0.58, 3.3, 0.58), soil, true)
        _box("ForestCrown_%02d" % i, Vector3(x, 4.1, z),
            Vector3(2.3, 2.4, 2.3), _mat("33483d"), false)
    # Checkpoint sits EAST of the route at (29,-16), so it cannot seal G-4.
    _box("CheckpointFloor", Vector3(29.3, 0.10, -15.2),
        Vector3(4.0, 0.20, 3.7), asphalt, true)
    _box("CheckpointBack", Vector3(29.3, 1.45, -17.0),
        Vector3(4.0, 2.7, 0.20), steel, true)
    _box("CheckpointSide", Vector3(31.2, 1.45, -15.2),
        Vector3(0.20, 2.7, 3.7), steel, true)
    _box("CheckpointRoof", Vector3(29.3, 2.94, -15.2),
        Vector3(4.4, 0.22, 4.1), rust, true)
    _box("CheckpointWindow", Vector3(27.4, 1.78, -15.5),
        Vector3(0.05, 0.85, 1.1), glass, false)
    _box("CheckpointCounter", Vector3(27.3, 0.88, -14.1),
        Vector3(0.32, 0.24, 1.5), steel, true)
    _box("CheckpointLamp", Vector3(27.38, 2.53, -15.1),
        Vector3(0.09, 0.15, 0.32), lamp, false)
    # A raised gate arm only, NOT a closed barrier or invisible wall.
    var boom := _box("RaisedBarrier", Vector3(26.8, 3.1, -11.2),
        Vector3(0.13, 3.8, 0.13), faded, false)
    boom.rotation.z = deg_to_rad(-32.0)
    _box("BarrierPost", Vector3(26.8, 0.95, -11.2),
        Vector3(0.35, 1.9, 0.35), rust, true)
    _box("G4Wayfinder", Vector3(26.8, 2.25, -17.5),
        Vector3(0.12, 0.65, 1.1), faded, false)
    # Ground markers show a navigable path without forcing story progression.
    for i in range(5):
        _box("G4EdgeLamp_%02d" % i,
            Vector3(26.5, 0.17, -19.0 - float(i) * 3.9),
            Vector3(0.18, 0.13, 0.30), lamp, false)

func _mat(color_hex: String, emissive: bool = false) -> StandardMaterial3D:
    var material := StandardMaterial3D.new()
    material.albedo_color = Color(color_hex)
    material.roughness = 0.95
    if emissive:
        material.emission_enabled = true
        material.emission = Color(color_hex)
        material.emission_energy_multiplier = 0.8
    return material

func _box(id: String, center: Vector3, size: Vector3, mat: Material,
        solid: bool) -> Node3D:
    var node := Node3D.new()
    node.name = id
    node.position = center
    add_child(node)
    var mesh := MeshInstance3D.new()
    var shape := BoxMesh.new()
    shape.size = size
    mesh.mesh = shape
    mesh.material_override = mat
    node.add_child(mesh)
    if solid:
        var body := StaticBody3D.new()
        var collision := CollisionShape3D.new()
        var box := BoxShape3D.new()
        box.size = size
        collision.shape = box
        body.add_child(collision)
        node.add_child(body)
    return node
