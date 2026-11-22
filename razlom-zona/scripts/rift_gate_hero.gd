extends Node3D
## ZONA-07: an original hand-authored procedural 3D landmark for Crimson Street.
## This builds real meshes, UV-mapped pixel albedo, fracture shards, props and
## intentional walkable gap. No external models or generated concept imagery.

var _rng := RandomNumberGenerator.new()
var _tex: ImageTexture
var _materials: Dictionary = {}
var _rift_light: OmniLight3D
var _edge_light: OmniLight3D
var _time: float = 0.0

func _ready() -> void:
    _rng.seed = 2026112207
    _tex = _pixel_grunge()
    _make_materials()
    _structure()
    _gate_iron()
    _floor_and_wear()
    _anomaly()
    _foliage_and_props()
    _collision_only()

func _pixel_grunge() -> ImageTexture:
    var image := Image.create(64, 64, false, Image.FORMAT_RGBA8)
    for y in 64:
        for x in 64:
            var a := (x * 743 + y * 571 + x * y * 37) % 101
            var value := 0.91 if a > 27 else 0.77
            if a < 7:
                value = 0.60
            if (x % 19 == 0 and y % 7 < 3) or (y % 23 == 0 and x % 11 < 4):
                value *= 0.78
            image.set_pixel(x, y, Color(value, value, value, 1.0))
    return ImageTexture.create_from_image(image)

func _mat(name: String, tint: String, metal: float = 0.0) -> StandardMaterial3D:
    var m := StandardMaterial3D.new()
    m.albedo_color = Color(tint)
    m.albedo_texture = _tex
    m.texture_filter = BaseMaterial3D.TEXTURE_FILTER_NEAREST
    m.roughness = 0.93
    m.metallic = metal
    _materials[name] = m
    return m

func _glow(name: String, tint: String, power: float) -> StandardMaterial3D:
    var m := StandardMaterial3D.new()
    m.albedo_color = Color(tint)
    m.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
    m.emission_enabled = true
    m.emission = Color(tint)
    m.emission_energy_multiplier = power
    m.metallic = 0.0
    m.roughness = 0.18
    m.cull_mode = BaseMaterial3D.CULL_DISABLED
    _materials[name] = m
    return m

func _make_materials() -> void:
    _mat("stone", "5e6468")
    _mat("stone_dark", "333f47")
    _mat("stone_warm", "79746b")
    _mat("rubble", "515456")
    _mat("rust", "845345", 0.32)
    _mat("iron", "27383a", 0.70)
    _mat("iron_edge", "73837f", 0.65)
    _mat("soil", "333a35")
    _mat("mud", "55433e")
    _mat("moss", "3c5344")
    _mat("dry_grass", "8b7e58")
    _mat("wet", "182f35")
    _mat("sign", "514f4b", 0.45)
    _glow("cyan", "3ee5d1", 2.3)
    _glow("magenta", "9839f0", 2.1)
    _glow("pale_cyan", "7afdf2", 2.9)

func _box(label: String, location: Vector3, dimensions: Vector3, material: String, yaw: float = 0.0) -> MeshInstance3D:
    var node := MeshInstance3D.new()
    node.name = label
    var cube := BoxMesh.new()
    cube.size = dimensions
    node.mesh = cube
    node.position = location
    node.rotation.y = yaw
    node.material_override = _materials[material]
    add_child(node)
    return node

func _rod(label: String, a: Vector3, b: Vector3, width: float, material: String) -> void:
    var distance := a.distance_to(b)
    if distance < 0.001:
        return
    var node := MeshInstance3D.new()
    node.name = label
    var tube := CylinderMesh.new()
    tube.top_radius = width
    tube.bottom_radius = width
    tube.height = distance
    tube.radial_segments = 5
    node.mesh = tube
    node.material_override = _materials[material]
    node.position = (a + b) * 0.5
    node.quaternion = Quaternion(Vector3.UP, (b - a).normalized())
    add_child(node)

func _polyhedron(label: String, points: Array[Vector3], indices: PackedInt32Array, material: String) -> void:
    var tool := SurfaceTool.new()
    tool.begin(Mesh.PRIMITIVE_TRIANGLES)
    for i in range(0, indices.size(), 3):
        var a: Vector3 = points[indices[i]]
        var b: Vector3 = points[indices[i + 1]]
        var c: Vector3 = points[indices[i + 2]]
        var n := (b - a).cross(c - a).normalized()
        for v in [a, b, c]:
            tool.set_normal(n)
            tool.set_uv(Vector2(v.x * 0.46, v.y * 0.46))
            tool.add_vertex(v)
    var node := MeshInstance3D.new()
    node.name = label
    node.mesh = tool.commit()
    node.material_override = _materials[material]
    add_child(node)

func _stone_block(label: String, location: Vector3, width: float, height: float, depth: float, material: String) -> void:
    var x := location.x
    var y := location.y
    var z := location.z
    var p: Array[Vector3] = [
        Vector3(x - width * .53, y - height * .49, z - depth * .47),
        Vector3(x + width * .48, y - height * .51, z - depth * .50),
        Vector3(x + width * .54, y + height * .46, z - depth * .42),
        Vector3(x - width * .47, y + height * .52, z - depth * .54),
        Vector3(x - width * .49, y - height * .47, z + depth * .47),
        Vector3(x + width * .50, y - height * .47, z + depth * .50),
        Vector3(x + width * .44, y + height * .54, z + depth * .54),
        Vector3(x - width * .55, y + height * .41, z + depth * .42)
    ]
    _polyhedron(label, p, PackedInt32Array([
        0, 2, 1, 0, 3, 2, 4, 5, 6, 4, 6, 7,
        0, 1, 5, 0, 5, 4, 3, 7, 6, 3, 6, 2,
        1, 2, 6, 1, 6, 5, 0, 4, 7, 0, 7, 3
    ]), material)

func _structure() -> void:
    # Two asymmetric ruined reinforced-concrete pylons; open central path
    # stays at least 3.6m wide, with NO collider across the portal.
    for side in [-1, 1]:
        var x: float = float(side) * 2.75
        for tier in 5:
            var w := 1.18 + 0.11 * float(tier % 2)
            var y := 0.40 + float(tier) * 0.69
            var z := .08 * float((tier + side + 8) % 3 - 1)
            var m := "stone" if tier % 3 != 0 else "stone_warm"
            _stone_block("fractured_pylon_%s_%d" % [str(side), tier], Vector3(x, y, z), w, .70, 1.50, m)
        _stone_block("pylon_broken_cap_%d" % side, Vector3(x, 3.69, 0), 1.42, .55, 1.52, "stone_dark")
        for i in 3:
            var bx := x + (float(i) - 1.0) * .23
            _rod("exposed_rebar", Vector3(bx, 3.76, 0), Vector3(bx + .13 * side, 4.67 + i * .12, .09), .037, "rust")
        for i in 12:
            var py := .38 + float(i % 6) * .45
            var px := x + float(side) * .67
            var pz := -.56 + float(i / 6) * .97
            _box("detached_stone_flake", Vector3(px, py, pz), Vector3(.075, .12, .19), "stone_dark", .13 * float(i))

    # The arch is not one rectangle: offset stone segments, a missing corner,
    # visible inner reinforcing steel and a separate sign support.
    for i in 8:
        var x := -2.24 + float(i) * .64
        var z := -.05 if i % 2 == 0 else .12
        var mat_name := "stone_warm" if i % 3 == 0 else "stone"
        _stone_block("broken_lintel_%d" % i, Vector3(x, 3.49 + float(i % 2) * .11, z), .68, .82, 1.45, mat_name)
    for i in 4:
        _rod("exposed_lintel_rebar_%d" % i, Vector3(-2.43, 3.91 + i * .11, -.36), Vector3(2.17, 3.88 + i * .11, -.36), .027, "rust")
    _box("nameplate_metal", Vector3(-.01, 3.79, -.82), Vector3(2.12, .53, .10), "sign")
    for x in [-.99, .99]:
        for y in [3.58, 3.95]:
            _box("plate_bolt", Vector3(x, y, -.888), Vector3(.047, .047, .035), "iron_edge")
    var sign := Label3D.new()
    sign.name = "SignZONA07"
    sign.text = "ЗОНА-07"
    sign.font_size = 40
    sign.pixel_size = 0.0086
    sign.modulate = Color("#ddab7a")
    sign.outline_modulate = Color("#171c1d")
    sign.outline_size = 5
    sign.position = Vector3(0.0, 3.68, -.898)
    sign.rotation.y = PI
    add_child(sign)

func _gate_iron() -> void:
    # Both leaves are forced open against their posts, no invisible blocking.
    for side in [-1, 1]:
        var px := float(side) * 3.59
        for i in 9:
            var x := px + float(side) * (float(i) * .23)
            var y_top := 1.74 + float((i * 7 + 3) % 3) * .18
            _rod("flaking_fence_bar_%d_%d" % [side, i], Vector3(x, .13, -.47), Vector3(x + .02 * float(i % 2), y_top, -.48), .026, "iron")
        for h in [.55, 1.32]:
            _rod("fence_rust_crossbar", Vector3(px - .08 * side, h, -.49), Vector3(px + 1.92 * side, h + .04, -.49), .042, "rust")
        for i in 5:
            var z := -.77 - float(i) * .05
            _box("iron_hinge", Vector3(float(side)*3.03, .42+i*.48, z), Vector3(.13, .075, .08), "iron_edge")
        # Cables hang over the entrance to sell a used abandoned place.
        for i in 2:
            var x := float(side) * (2.86 + i * .18)
            _rod("torn_power_cable", Vector3(x, 3.93, -.64), Vector3(x+.28*float(side), 2.88-i*.31, -.81), .018, "iron")

func _spike(label: String, base: Vector3, height: float, radius: float, material: String) -> void:
    var tip := base + Vector3(.16 * height * _rng.randf_range(-1, 1), height, .13 * height * _rng.randf_range(-1, 1))
    var p: Array[Vector3] = [
        base + Vector3(-radius, 0, -radius), base + Vector3(radius, 0, -radius),
        base + Vector3(radius, 0, radius), base + Vector3(-radius, 0, radius), tip
    ]
    _polyhedron(label, p, PackedInt32Array([0,1,4,1,2,4,2,3,4,3,0,4,0,3,2,0,2,1]), material)

func _anomaly() -> void:
    var pane := MeshInstance3D.new()
    pane.name = "PixelFracturePlane_NonCollision"
    var p := PlaneMesh.new()
    p.size = Vector2(3.33, 3.15)
    pane.mesh = p
    pane.position = Vector3(0, 1.88, 0.35)
    pane.rotation_degrees.x = 90.0
    var sh := ShaderMaterial.new()
    sh.shader = preload("res://assets/anomaly/rift_surface.gdshader")
    pane.material_override = sh
    add_child(pane)

    # Three-dimensional crystalline edges, irregular local highlights
    # and two coloured real lights make the portal dimensional in-engine.
    for i in 24:
        var a := float(i) / 23.0
        var side := -1.0 if i % 2 == 0 else 1.0
        var x := side * _rng.randf_range(1.35, 2.36)
        var z := _rng.randf_range(-.45, .88)
        var y := .12 if i < 12 else _rng.randf_range(.60, 1.8)
        var height := _rng.randf_range(.18, .85)
        var mat_name := "cyan" if i % 3 != 0 else "magenta"
        _spike("rift_crystal_%02d" % i, Vector3(x, y, z), height, _rng.randf_range(.07, .16), mat_name)
        if i % 4 == 0:
            _rod("rift_electric_filament", Vector3(x, y + height*.55, z), Vector3(x - side*.20, y + height*1.23, z-.25), .014, "pale_cyan")
    for i in 12:
        var x := _rng.randf_range(-1.0, 1.0)
        _spike("floor_ether_%02d" % i, Vector3(x, .11, _rng.randf_range(-1.2, .8)), _rng.randf_range(.08, .28), .035, "cyan")
    _rift_light = OmniLight3D.new()
    _rift_light.name = "RiftBounceCyan"
    _rift_light.position = Vector3(-1.1, 2.0, -.64)
    _rift_light.light_color = Color("#41dacf")
    _rift_light.light_energy = 2.6
    _rift_light.omni_range = 7.0
    _rift_light.shadow_enabled = false
    add_child(_rift_light)
    _edge_light = OmniLight3D.new()
    _edge_light.name = "RiftBounceViolet"
    _edge_light.position = Vector3(1.7, 2.9, 1.0)
    _edge_light.light_color = Color("#8d3cce")
    _edge_light.light_energy = 1.55
    _edge_light.omni_range = 4.5
    _edge_light.shadow_enabled = false
    add_child(_edge_light)

func _floor_and_wear() -> void:
    _box("old_slab_under_entrance", Vector3(0, .035, 0), Vector3(9.8, .075, 7.2), "mud")
    # Visible road edges, pools and post-collapse rubble.
    for side in [-1, 1]:
        _box("road_broken_curb", Vector3(float(side)*3.98, .12, -.4), Vector3(.31,.20, 6.75), "stone_dark", .04*float(side))
        for i in 9:
            var x := float(side) * _rng.randf_range(2.07, 4.8)
            var z := _rng.randf_range(-3.3, 2.7)
            _stone_block("fallen_scree_%02d_%d" % [i, side], Vector3(x,.19,z), _rng.randf_range(.13,.45), _rng.randf_range(.08,.28),_rng.randf_range(.11,.38), "rubble")
        for i in 5:
            var x := float(side) * _rng.randf_range(2.3, 4.5)
            _box("moss_island", Vector3(x, .097, -2.7 + i*.96), Vector3(.38, .01, .16), "moss", .6*i)
    for i in 7:
        var xx := _rng.randf_range(-3.4,3.4)
        var zz := _rng.randf_range(-3.2,3.1)
        _box("oil_rain_puddle_%02d" % i, Vector3(xx,.090,zz),Vector3(_rng.randf_range(.3,.92),.005,_rng.randf_range(.18,.54)),"wet",_rng.randf_range(-1.5,1.5))
    for i in 23:
        var x := _rng.randf_range(-4.2,4.2)
        var z := _rng.randf_range(-3.2,3.0)
        _box("road_crack_%02d" % i, Vector3(x,.096,z),Vector3(.014,.004,_rng.randf_range(.1,.65)),"stone_dark",_rng.randf_range(-1.2,1.2))

func _foliage_and_props() -> void:
    for i in 65:
        var side := -1.0 if i%2==0 else 1.0
        var origin := Vector3(side * _rng.randf_range(2.25, 5.4), .07, _rng.randf_range(-3.4, 3.3))
        _rod("dry_weeds_%02d" % i, origin, origin + Vector3(_rng.randf_range(-.13,.13), _rng.randf_range(.15,.58), _rng.randf_range(-.17,.17)), .009, "dry_grass" if i%3==0 else "moss")
    for i in 19:
        var side := -1.0 if i%2==0 else 1.0
        var base := Vector3(side * _rng.randf_range(3.0,5.1),.1,_rng.randf_range(-3.4,3.1))
        _rod("wild_branch_trunk_%02d" % i, base, base+Vector3(_rng.randf_range(-.25,.25),.32,_rng.randf_range(-.21,.21)), .018, "moss")
    # Physical original roadside posts and abandoned electrical box.
    _box("rusted_power_box", Vector3(4.05,1.15,-1.45), Vector3(.50,.69,.28), "rust", -.08)
    _box("switchboard_face", Vector3(4.05,1.16,-1.62),Vector3(.40,.58,.02),"iron")
    for i in 3:
        _box("fuse_socket", Vector3(3.90+.15*i,1.22,-1.65),Vector3(.09,.11,.02),"iron_edge")
    for i in 4:
        _rod("loose_electrical_cable", Vector3(4.10+i*.08,1.00,-1.6), Vector3(4.20+i*.15,.35,-1.85), .013,"rust")
    for side in [-1,1]:
        for i in 4:
            var x := float(side)*(4.72+i*.31)
            _rod("deformed_barbed_wire", Vector3(x,.42,-2.8), Vector3(x+.20,.56,-2.54),.012,"iron")

func _collider(name: String, origin: Vector3, dimensions: Vector3) -> void:
    var body := StaticBody3D.new()
    body.name = name
    body.position = origin
    var collision := CollisionShape3D.new()
    var shape := BoxShape3D.new()
    shape.size = dimensions
    collision.shape = shape
    body.add_child(collision)
    add_child(body)

func _collision_only() -> void:
    # Important: central passage is clear, animation and player interaction
    # are not blocked by the energy plane.
    for side in [-1, 1]:
        _collider("SolidPylon_%s" % str(side), Vector3(float(side)*2.75,1.68,0), Vector3(1.20,3.36,1.56))
    _collider("SolidLintel",Vector3(0,3.53,0),Vector3(4.50,.72,1.45))

func _process(delta: float) -> void:
    _time += delta
    if is_instance_valid(_rift_light):
        _rift_light.light_energy = 2.25 + sin(_time * 1.5) * .32
    if is_instance_valid(_edge_light):
        _edge_light.light_energy = 1.3 + sin(_time * 2.3 + 1.4) * .22
