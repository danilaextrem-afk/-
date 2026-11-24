extends Node3D
## Original exterior scene component: bunker forecourt / abandoned control
## checkpoint. All meshes are true 3D, deterministic and mobile-conscious.
## Local origin = bunker center at world x=24, z=-20. Entrance is toward +Z.
## Leaves a 4.4m open route down into the pre-existing interactive gallery.

var seed := RandomNumberGenerator.new()
var palette: Dictionary = {}
var stone_tex: Texture2D

func _ready() -> void:
    seed.seed = 26071982
    stone_tex = _make_pixel_tex()
    _materials()
    _ground_bank(-1)
    _ground_bank(1)
    _checkpoint_dress()
    _foreground_wear()

func _make_pixel_tex() -> Texture2D:
    var image := Image.create(64, 64, false, Image.FORMAT_RGBA8)
    for y in 64:
        for x in 64:
            var block_x := int(x/2)
            var block_y := int(y/2)
            var h := (block_x*1567 + block_y*2237 + block_x*block_y*773) % 101
            var v := 0.66 + float(h % 26) * 0.011
            if (block_y*3+block_x*7) % 29 < 2:
                v *= 0.82
            image.set_pixel(x,y,Color(v, v*0.99, v*0.94, 1))
    return ImageTexture.create_from_image(image)

func _m(key: String, hex: String, metallic: float = 0.0) -> void:
    var mat := StandardMaterial3D.new()
    mat.albedo_color = Color(hex)
    mat.albedo_texture = stone_tex
    mat.texture_filter = BaseMaterial3D.TEXTURE_FILTER_NEAREST
    mat.roughness = 0.95
    mat.metallic = metallic
    palette[key] = mat

func _materials() -> void:
    _m("basalt", "343f44")
    _m("rock", "596365")
    _m("rock_dark", "26383d")
    _m("soil", "333833")
    _m("peat", "383034")
    _m("moss", "364f43")
    _m("deadwood", "493b36")
    _m("reed", "65715a")
    _m("rust", "744b3f", .46)
    _m("iron", "364347", .70)
    _m("trim", "6e7772", .54)
    _m("water", "1b353b", .24)
    _m("warning", "977c4d", .34)

func _block(name: String, pos: Vector3, dims: Vector3, kind: String, angle: float = 0.0) -> void:
    var m := MeshInstance3D.new()
    m.name = name
    var shape := BoxMesh.new()
    shape.size = dims
    m.mesh = shape
    m.position = pos
    m.rotation.y = angle
    m.material_override = palette[kind]
    add_child(m)

func _rod(name: String, a: Vector3, b: Vector3, radius: float, kind: String) -> void:
    var length := a.distance_to(b)
    if length < .003:
        return
    var mesh := MeshInstance3D.new()
    mesh.name = name
    var cylinder := CylinderMesh.new()
    cylinder.radial_segments = 5
    cylinder.height = length
    cylinder.top_radius = radius
    cylinder.bottom_radius = radius
    mesh.mesh = cylinder
    mesh.position = (a+b)*.5
    mesh.quaternion = Quaternion(Vector3.UP, (b-a).normalized())
    mesh.material_override = palette[kind]
    add_child(mesh)

func _rubble(name: String, pos: Vector3, scale: Vector3, kind: String) -> void:
    # Purposeful irregular 3D chipped block, not a repeating CubeMesh.
    var pts: Array[Vector3] = [
        Vector3(-.52,-.49,-.46), Vector3(.51,-.49,-.51),
        Vector3(.52,-.47,.49), Vector3(-.45,-.50,.52),
        Vector3(-.42,.47,-.49), Vector3(.42,.53,-.37),
        Vector3(.47,.33,.48), Vector3(-.50,.56,.41)]
    var index := PackedInt32Array([
        0,2,1, 0,3,2, 0,1,5,0,5,4,
        1,2,6,1,6,5,2,3,7,2,7,6,
        3,0,4,3,4,7,4,5,6,4,6,7])
    var st := SurfaceTool.new()
    st.begin(Mesh.PRIMITIVE_TRIANGLES)
    for k in range(0,index.size(),3):
        var a := pts[index[k]]*scale
        var b := pts[index[k+1]]*scale
        var c := pts[index[k+2]]*scale
        var n := (b-a).cross(c-a).normalized()
        for q in [a,b,c]:
            st.set_normal(n)
            st.set_uv(Vector2(q.x, q.z) * 0.3)
            st.add_vertex(q)
    var render := MeshInstance3D.new()
    render.name = name
    render.mesh = st.commit()
    render.position = pos
    render.rotation.y = seed.randf_range(-.3,.3)
    render.material_override = palette[kind]
    add_child(render)

func _ground_bank(side: int) -> void:
    var s := float(side)
    # Graduated retaining bank on both sides of the ramp, clear central lane.
    for row in 8:
        var z := 3.25 - row * 1.21
        var h := 1.4 + 0.6 * sin(float(row)*0.4)
        var x := s * (3.9 + float(row % 3) * .16)
        _rubble("BrokenBank_%d_%02d" % [side,row],
            Vector3(x, .35 + h*.45, z), Vector3(2.0, h, 1.54),
            "basalt" if row % 3 else "rock_dark")
        _rubble("BankStratum_%d_%02d" % [side,row],
            Vector3(x+s*.65,.30,z+.10), Vector3(1.5,.66,1.25),
            "soil" if row%2==0 else "rock")
        # Actual silhouette against the sky rather than flat, open horizon.
        for i in 2:
            var dx := seed.randf_range(.30,1.45)
            var bx := x + s*(.75+dx)
            var bz := z+seed.randf_range(-.6,.6)
            _rubble("BreakSlab_%d_%02d_%d" % [side,row,i],
                Vector3(bx,.25,bz), Vector3(seed.randf_range(.4,1),seed.randf_range(.3,.7),seed.randf_range(.4,1.15)), "rock")
    for i in 3:
        var z := 2.8 - i*4.8
        _dead_tree(Vector3(s*(6.4+i*.48),0,z),1.4+float(i)*.22)
    for i in 16:
        var x := s*seed.randf_range(3.15,7.0)
        var z := seed.randf_range(-7,4)
        _rod("Reeds_%d_%02d" % [side,i],
            Vector3(x,.12,z), Vector3(x+seed.randf_range(-.2,.2),seed.randf_range(.25,.78),z+seed.randf_range(-.23,.23)),
            .012,"reed" if i%3==0 else "moss")

func _dead_tree(base: Vector3, scale_factor: float) -> void:
    var height := seed.randf_range(3.6,6.0)*scale_factor
    var top := base + Vector3(.23*scale_factor,height,.11*scale_factor)
    _rod("DeadPineTrunk",base+Vector3(0,.13,0),top,.13*scale_factor,"deadwood")
    for i in 7:
        var t := (float(i)+.8)/8.
        var start := base.lerp(top,t)
        var theta := float(i)*2.399
        var reach := (1.0-t)*scale_factor*seed.randf_range(1.2,1.9)
        var end := start + Vector3(cos(theta)*reach, -.32*reach, sin(theta)*reach)
        _rod("DeadPineBranch",start,end,.05*scale_factor*(1.-t*.55),"deadwood")
        if i%3==0:
            _rod("DeadPineTwig",end*.65+start*.35,end+Vector3(.22*scale_factor,.45*scale_factor,.13*scale_factor),.018,"deadwood")

func _checkpoint_dress() -> void:
    # Concrete side cheeks, chipped hazard strip, contrasting cable lines.
    for side in [-1,1]:
        var s := float(side)
        _block("RetainingWallFace",Vector3(s*2.88,.48,-3.5),Vector3(.22,.92,8.4),"basalt")
        _block("BrokenCurb",Vector3(s*2.69,.16,-2.3),Vector3(.29,.22,9.1),"rock_dark")
        for i in 10:
            var z := -6.6 + i*.9
            _block("PaintChipHazard",Vector3(s*2.77,.29,z),
                Vector3(.04,.16,.31),"warning" if i%2==0 else "rust")
        for i in 5:
            var z := 2.5 - i*1.86
            _rubble("ShoulderRubble",Vector3(s*2.50,.18,z),
                Vector3(.14+.12*float(i%2),.16,.53),"rock")
        # Bent poles guide the eye to the entrance with real local fixtures.
        var px := s*3.15
        _rod("UtilityPole",Vector3(px,0,1.7),Vector3(px+s*.11,3.5,1.62),.065,"iron")
        _rod("PoleCrossarm",Vector3(px-s*.45,3.32,1.63),Vector3(px+s*.44,3.33,1.63),.043,"rust")
        _rod("CableToBunker",Vector3(px+s*.15,3.30,1.63),Vector3(px+s*.10,2.3,-4.9),.018,"iron")
        for j in range(2):
            _block("RoadsideBolt",Vector3(px+s*.12,.20+j*.16,1.71),
                Vector3(.10,.065,.13),"trim")
        # Two lights, no expensive shadow casting.
        var lamp := OmniLight3D.new()
        lamp.name = "BunkerApproachLamp"
        lamp.position = Vector3(s*2.75,2.7,-3.95)
        lamp.light_color = Color("#66b9b5") if side<0 else Color("#cea277")
        lamp.light_energy = 0.72
        lamp.omni_range = 4.8
        lamp.shadow_enabled = false
        add_child(lamp)
        _block("DampStrip",Vector3(s*2.23,.082,-2.2),Vector3(.6,.01,9.2),"water")
    # More expressive entry: dangling utility cabinet, rust and discarded drums.
    for i in 3:
        _block("DiscardedDrum",Vector3(-4.35+i*.53,.45,2.85+i*.25),Vector3(.42,.77,.43),
            "rust" if i != 1 else "iron", float(i)*.35)
        _block("DrumLid",Vector3(-4.35+i*.53,.84,2.85+i*.25),Vector3(.45,.05,.46),"trim")
    _block("WornSign",Vector3(-3.31,2.48,-.65),Vector3(.09,.60,1.33),"rust")
    var label := Label3D.new()
    label.name = "SectorLabel"
    label.text = "ОБЪЕКТ 07"
    label.font_size = 31
    label.pixel_size = .009
    label.modulate = Color("#c3cfb9")
    label.outline_modulate = Color("#111d20")
    label.outline_size = 4
    label.position = Vector3(-3.39,2.42,-.65)
    label.rotation.y = -PI/2.0
    add_child(label)

func _foreground_wear() -> void:
    # Dark foreground framing, layered moss/puddles; kept off the walkable path.
    for i in 19:
        var side := -1.0 if i%2==0 else 1.0
        var x := side*seed.randf_range(2.15,3.05)
        var z := seed.randf_range(-6.5,4.0)
        _block("WetEdgeOrMoss",Vector3(x,.085,z),
            Vector3(seed.randf_range(.18,.6),.012,seed.randf_range(.2,.9)),
            "water" if i%5==0 else "moss",seed.randf_range(-.9,.9))
