extends "res://Source/walk_scene.gd"

# Values follow the camp checkpoint (6) in the campaign save.
enum Stage { ELDER = 7, DECIDE, TRACK, SMITH_ESCAPED, SMITH_CLEARED, BLADE_ESCAPED, BLADE_CLEARED, GHOUL_ESCAPED, GHOUL_CLEARED, BELL_ESCAPED, BELL_CLEARED, WATCH_ESCAPED, WATCH_CLEARED }
const ELDER := Vector3(0,0,-5)
const PRISONER := Vector3(-4,0,-12)
const GRANARY := Vector3(4,0,-14)
const FORGE := Vector3(1,0,-23)
const WATCH := Vector3(-2.6,0,-26)
var chest_lid := Node3D.new()
var homespun := StandardMaterial3D.new()
var harness := StandardMaterial3D.new()
var lost_key := Node3D.new()
var dusk: Environment
var captive := Node3D.new()
var shutter := Node3D.new()
var alert := false
var smith: MeshInstance3D
var hearth_light: OmniLight3D
var clock := 0.0
var witness := Node3D.new()

func _ready() -> void:
	homespun.albedo_color = Color("#778077")
	homespun.albedo_texture = preload("res://Art/Forge/Scans/poly_wool_herringbone_diffuse.jpg")
	homespun.normal_enabled = true
	homespun.normal_texture = preload("res://Art/Forge/Scans/poly_wool_herringbone_nor_gl.jpg")
	homespun.uv1_triplanar = true
	homespun.uv1_scale = Vector3.ONE*3
	harness.albedo_color = Color("#a4937e")
	harness.albedo_texture = preload("res://Art/Forge/Scans/brown_leather_diffuse.jpg")
	harness.normal_enabled = true
	harness.normal_texture = preload("res://Art/Forge/Scans/brown_leather_nor_gl.jpg")
	harness.uv1_triplanar = true
	harness.uv1_scale = Vector3.ONE*2
	var ground := box(self,Vector3(0,-.25,-15),Vector3(70,.5,80),"ffffff")
	var soil := ShaderMaterial.new()
	soil.shader = preload("res://Art/Forge/wet_earth.gdshader")
	soil.set_shader_parameter("earth",preload("res://Art/Forge/Scans/forest_floor_diffuse.jpg"))
	soil.set_shader_parameter("relief",preload("res://Art/Forge/Scans/forest_floor_nor_gl.jpg"))
	ground.material_override = soil
	add_child(preload("res://Art/Forge/YardProps.glb").instantiate())
	var lane := MeshInstance3D.new()
	lane.mesh = preload("res://Art/Forge/smiths_lane.obj")
	add_child(lane)
	dress_lane(lane)
	add_child(lost_key)
	lost_key.position = GRANARY + Vector3(.38,.44,.15)
	var bow := TorusMesh.new()
	bow.inner_radius = .044
	bow.outer_radius = .077
	var ring := MeshInstance3D.new()
	ring.mesh = bow
	lost_key.add_child(ring)
	box(lost_key,Vector3(.115,0,0),Vector3(.18,.029,.028),"a58b58")
	box(lost_key,Vector3(.175,0,.028),Vector3(.034,.029,.065),"a58b58")
	build_forge_yard()
	build_watch_chest()
	resident(self,ELDER)
	add_child(captive)
	resident(captive,PRISONER)
	add_child(shutter)
	for i in 5:
		box(shutter,PRISONER+Vector3(-.8+i*.4,1.1,.65),Vector3(.13,2.2,.16),"4f3f32")
	add_child(witness)
	resident(witness,FORGE+Vector3(-3,0,-1))
	smith = resident(self,FORGE+Vector3(1.7,0,-1))

	box(self,GRANARY+Vector3.UP*.2,Vector3(1.8,.4,1.1),"705438")
	for i in 3:
		box(self,GRANARY+Vector3(i*.2-.3,.415,0),Vector3(.05,.02,.65),"312822").rotation.y = -.3

func build_watch_chest() -> void:
	var chest := Node3D.new()
	add_child(chest)
	chest.position = WATCH
	box(chest,Vector3(0,.12,0),Vector3(.96,.13,.58),"34271d")
	for side in [-1,1]:
		box(chest,Vector3(side*.45,.32,0),Vector3(.08,.40,.60),"594132")
		box(chest,Vector3(0,.32,side*.26),Vector3(.92,.40,.08),"654a35")
		for band in [-1,1]:
			box(chest,Vector3(band*.32,.32,side*.307),Vector3(.07,.41,.025),"424345")
	chest.add_child(chest_lid)
	chest_lid.position = Vector3(0,.52,-.30)
	box(chest_lid,Vector3(0,.045,.30),Vector3(1,.09,.64),"74583e")
	for side in [-1,1]:
		box(chest_lid,Vector3(side*.32,.095,.30),Vector3(.07,.018,.64),"48494a")
	box(chest_lid,Vector3(0,-.015,.628),Vector3(.085,.13,.026),"ae8960")
	var note := box(chest,Vector3(0,.23,0),Vector3(.39,.012,.25),"d2be98")
	note.rotation.y = -.16

func place(scene: PackedScene, where: Vector3, yaw: float, size: float = 1.0) -> Node3D:
	var prop := scene.instantiate() as Node3D
	add_child(prop)
	prop.position = where
	prop.rotation.y = yaw
	prop.scale = Vector3.ONE*size
	return prop

func dress_lane(lane: MeshInstance3D) -> void:
	var bark := StandardMaterial3D.new()
	bark.albedo_texture = preload("res://Art/Forge/Scans/pine_bark_diffuse.jpg")
	bark.normal_enabled = true
	bark.normal_texture = preload("res://Art/Forge/Scans/pine_bark_nor_gl.jpg")
	bark.normal_scale = .7
	for index in lane.mesh.get_surface_count():
		if lane.mesh.surface_get_name(index) == "old_logs":
			lane.set_surface_override_material(index,bark)
	var barrel := preload("res://Art/Forge/Scans/wine_barrel_01.glb")
	place(barrel,Vector3(3.94,0,-23.48),.35)
	place(barrel,Vector3(4.68,0,-24.10),-.93,.89)
	place(barrel,Vector3(-3.8,0,-25.84),.64)
	place(preload("res://Art/Forge/Scans/tree_stump_01.glb"),Vector3(-2.94,.09,-24.52),.75,.92)
	var axe := place(preload("res://Art/Forge/Scans/wooden_axe.glb"),Vector3(-2.92,.65,-24.47),-.54)
	axe.rotation.z = 2.62
	var fern := preload("res://Art/Forge/Scans/fern_02.glb")
	for i in 14:
		var side := 1 if i%2 == 0 else -1
		place(fern,Vector3(side*(4.4+.59*sin(i*1.9)),.02,-7-i*1.57),i*2.27,.83+.35*cos(i))
	place(preload("res://Art/Forge/Scans/stone_fire_pit.glb"),Vector3(-3.34,.12,-28.05),.7,.72)
	var straw := StandardMaterial3D.new()
	straw.albedo_texture = preload("res://Art/Forge/Scans/thatch_roof_angled_diffuse.jpg")
	straw.normal_enabled = true
	straw.normal_texture = preload("res://Art/Forge/Scans/thatch_roof_angled_nor_gl.jpg")
	straw.cull_mode = BaseMaterial3D.CULL_DISABLED
	for roof: Vector3 in [Vector3(-8,2.8,-5),Vector3(8,2.8,-7),Vector3(-9,2.8,-14),Vector3(9,2.8,-17),Vector3(-9,2.8,-25),Vector3(10,2.8,-29),Vector3(-1.15,5.9,-34)]:
		var tower: bool = roof.y>4
		for side in [-1,1]:
			var half_width := 2.58 if tower else 3.02
			var rise := 2.17 if tower else 1.78
			var surface := SurfaceTool.new()
			surface.begin(Mesh.PRIMITIVE_TRIANGLES)
			var depth := 4.08 if tower else 6.36
			var columns := 32
			for row in 6:
				for col in columns:
					for corner: Vector2i in [Vector2i(0,0),Vector2i(0,1),Vector2i(1,1),Vector2i(0,0),Vector2i(1,1),Vector2i(1,0)]:
						var u := (col+corner.x)/float(columns)
						var v := (row+corner.y)/6.0
						var fringe := .075*sin(col+corner.x+roof.x)*v*v*v
						var eave := half_width*v+fringe
						var y := rise*(1.0-v)+.14-.12*sin(PI*u)+.04*cos(u*19+roof.z)+.08*sin(v*PI)
						surface.set_uv(Vector2(u*depth*.65,v*1.52))
						surface.set_normal(Vector3(side*rise,half_width,0).normalized())
						surface.add_vertex(Vector3(side*eave,y-fringe*.60,(u-.5)*depth))
			var patch := MeshInstance3D.new()
			patch.mesh = surface.commit()
			patch.material_override = straw
			add_child(patch)
			patch.position = roof

func build_forge_yard() -> void:
	dusk = Environment.new()
	dusk.background_mode = Environment.BG_SKY
	dusk.sky = Sky.new()
	var panorama := PanoramaSkyMaterial.new()
	panorama.panorama = preload("res://Art/dusk_panorama.jpg")
	dusk.sky.sky_material = panorama
	dusk.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
	dusk.ambient_light_color = Color("#bccdda")
	dusk.ambient_light_energy = .55
	dusk.background_energy_multiplier = .56
	dusk.tonemap_mode = Environment.TONE_MAPPER_FILMIC
	dusk.fog_enabled = true
	dusk.fog_light_color = Color("#8193a0")
	dusk.fog_density = .014
	dusk.fog_sky_affect = .08
	for pos in [Vector3(3.65,1,-25.6),Vector3(-5.5,1.4,-25)]:
		var lamp := OmniLight3D.new()
		add_child(lamp)
		lamp.position = pos
		lamp.light_color = Color("#ff9646")
		lamp.light_energy = 1.45
		lamp.omni_range = 8.0
		if pos.x > 0:
			hearth_light = lamp
			lamp.shadow_enabled = true
	for k in 3:
		plume(Vector3(3.65,1.02,-25.68-k*.045),Vector2(.94,1.04),false,k)
	plume(Vector3(3.63,5.39,-26.35),Vector2(1.64,3.3),true,0)

func plume(at: Vector3, size: Vector2, smoke: bool, phase: int) -> void:
	var sprite := MeshInstance3D.new()
	var quad := QuadMesh.new()
	quad.size = size
	sprite.mesh = quad
	var shader := ShaderMaterial.new()
	shader.shader = preload("res://Art/Forge/hearth.gdshader")
	shader.set_shader_parameter("smoke",smoke)
	shader.set_shader_parameter("offset",phase*.77)
	sprite.material_override = shader
	sprite.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
	add_child(sprite)
	sprite.position = at
	sprite.rotation.y = -.28+phase*.23

func _process(delta: float) -> void:
	if not is_visible_in_tree(): return
	clock += delta
	hearth_light.light_energy = 2.75 + .36*sin(clock*8.1)+.39*sin(clock*13.6)
	var shelter := FORGE+Vector3(4.40,0,-.7) if alert else FORGE+Vector3(1.7,0,-1)
	smith.position = smith.position.move_toward(shelter,delta*2.45)
	smith.rotation.y = lerp_angle(smith.rotation.y,1.15 if alert else 2.36,minf(delta*4,1))
	smith.rotation.x = lerpf(smith.rotation.x,-.13 if alert else .02*sin(clock*2),minf(delta*4,1))


func resident(parent: Node3D, pos: Vector3) -> MeshInstance3D:
	var body := MeshInstance3D.new()
	body.mesh = preload("res://Art/Forge/traveller.obj")
	for surface in body.mesh.get_surface_count():
		match body.mesh.surface_get_name(surface):
			"ash_blue_wool": body.set_surface_override_material(surface,homespun)
			"oxblood_leather": body.set_surface_override_material(surface,harness)
	parent.add_child(body)
	body.add_child(preload("res://Art/Forge/Scans/FoundryFace.glb").instantiate())
	body.position = pos
	body.rotation.y = 2.36 if pos.x > 0 else -2.36
	return body

func show_stage(stage: int) -> void:
	chest_lid.rotation.x = deg_to_rad(-108) if stage in [Stage.WATCH_ESCAPED,Stage.WATCH_CLEARED] else 0.0
	lost_key.visible = stage in [Stage.GHOUL_ESCAPED,Stage.GHOUL_CLEARED]
	captive.visible = stage in [Stage.ELDER,Stage.DECIDE,Stage.TRACK]
	shutter.visible = captive.visible
	witness.visible = stage in [Stage.SMITH_CLEARED,Stage.BLADE_CLEARED,Stage.GHOUL_CLEARED,Stage.BELL_CLEARED,Stage.WATCH_CLEARED]

func hint(stage: int) -> String:
	match stage:
		Stage.ELDER: return "Мира привела тебя в селение. Староста ждёт на дороге перед избами."
		Stage.DECIDE: return "Староста: «Засов разнесло, зерно пропало. А кроме Богдашки тут никого».\nСлева — клеть с пленником. Справа — выломанный порог амбара."
		Stage.TRACK: return "Три борозды идут изнутри. Щепа лежит снаружи. Отнеси обломок старосте."
		Stage.SMITH_ESCAPED: return "Богдашка скрылся в чаще. Староста захлопнул дверь. Мира ждёт тебя у кузнеца."
		Stage.SMITH_CLEARED: return "Пленника отпустили. Он замолвил за тебя слово у кузнеца, в конце улицы."
		Stage.BLADE_ESCAPED: return "Кузнец дал Мире под поручительство затупленный клинок. Староста помнит побег. Вернись к кузнецу за уроком."
		Stage.GHOUL_ESCAPED: return "За воротами ударили в колокол. На пороге амбара что-то блеснуло."
		Stage.GHOUL_CLEARED: return "Богдашка: «Слышишь? Так звонили, когда пропал сторож. Его ключ я видел в амбаре»."
		Stage.BELL_ESCAPED: return "На ключе вырезана зарубка. Такая же метка на ларце слева от ворот."
		Stage.BELL_CLEARED: return "Богдашка узнал ключ пропавшего сторожа. Он указал на ларец слева от ворот."
		Stage.WATCH_ESCAPED: return "В ларце записка сторожа: «Третий удар слышен со старой переправы». Подпись выскоблена."
		Stage.WATCH_CLEARED: return "Богдашка прочитал записку: «Третий удар слышен со старой переправы». На обороте он узнал метку перевозчика."
	return "Кузнец одолжил учебный клинок. Богдашка остался в селении и готов поручиться за тебя. Подойди к кузнецу за уроком."

func next_step(stage: int, pos: Vector3) -> int:
	match stage:
		Stage.ELDER:
			if pos.distance_to(ELDER)<2.6: return Stage.DECIDE
		Stage.DECIDE:
			if pos.distance_to(PRISONER)<2.6: return Stage.SMITH_ESCAPED
			if pos.distance_to(GRANARY)<2.6: return Stage.TRACK
		Stage.TRACK:
			if pos.distance_to(ELDER)<2.6: return Stage.SMITH_CLEARED
		Stage.SMITH_ESCAPED,Stage.SMITH_CLEARED:
			if pos.distance_to(FORGE)<2.6: return stage+2
		Stage.GHOUL_ESCAPED,Stage.GHOUL_CLEARED:
			if pos.distance_to(GRANARY)<2.6: return stage+2
		Stage.BELL_ESCAPED,Stage.BELL_CLEARED:
			if pos.distance_to(WATCH)<2.0: return stage+2
	return stage

func prompt(stage: int, pos: Vector3) -> String:
	var next := next_step(stage,pos)
	if next == stage: return ""
	match next:
		Stage.WATCH_ESCAPED,Stage.WATCH_CLEARED: return "Отпереть ларец сторожа"
		Stage.BELL_ESCAPED,Stage.BELL_CLEARED: return "Поднять ключ сторожа"
		Stage.DECIDE: return "Выслушать старосту"
		Stage.SMITH_ESCAPED: return "Отпереть клеть и выпустить пленника"
		Stage.TRACK: return "Осмотреть сломанный порог"
		Stage.SMITH_CLEARED: return "Предъявить обломок"
	return "Взять учебный клинок"
