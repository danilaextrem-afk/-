extends "res://Source/walk_scene.gd"

# Values follow the camp checkpoint (6) in the campaign save.
enum Stage { ELDER = 7, DECIDE, TRACK, SMITH_ESCAPED, SMITH_CLEARED, BLADE_ESCAPED, BLADE_CLEARED, GHOUL_ESCAPED, GHOUL_CLEARED }
const ELDER := Vector3(0,0,-5)
const PRISONER := Vector3(-4,0,-12)
const GRANARY := Vector3(4,0,-14)
const FORGE := Vector3(1,0,-23)
var dusk: Environment
var captive := Node3D.new()
var shutter := Node3D.new()
var witness := Node3D.new()

func _ready() -> void:
	box(self,Vector3(0,-.25,-13),Vector3(40,.5,58),"484034")
	build_forge_yard()
	for i in range(6):
		var x := -9 if i%2 == 0 else 9
		var z := -5.0-floori(i/2.0)*8
		box(self,Vector3(x,1.5,z),Vector3(5,3,5),"70503a")
		for side in [-1,1]:
			var roof := box(self,Vector3(x+side*1.45,3.55,z),Vector3(3.3,.28,5.6),"443d33")
			roof.rotation.z = -side*.34
		box(self,Vector3(x,1.3,z+2.52),Vector3(1.2,2.6,.1),"3f3029")
	person(self,ELDER,"746344")
	add_child(captive)
	person(captive,PRISONER,"56655e")
	add_child(shutter)
	for i in 5:
		box(shutter,PRISONER+Vector3(-.8+i*.4,1.1,.65),Vector3(.13,2.2,.16),"4f3f32")
	add_child(witness)
	person(witness,FORGE+Vector3.LEFT*2,"56655e")
	person(self,FORGE,"573e37")
	box(self,FORGE+Vector3(1,.7,0),Vector3(1.4,.5,.7),"42434a")
	box(self,GRANARY+Vector3.UP*.2,Vector3(1.8,.4,1.1),"705438")
	for i in 3:
		box(self,GRANARY+Vector3(i*.2-.3,.415,0),Vector3(.05,.02,.65),"312822").rotation.y = -.3

func build_forge_yard() -> void:
	dusk = Environment.new()
	dusk.background_mode = Environment.BG_COLOR
	dusk.background_color = Color("#243746")
	dusk.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
	dusk.ambient_light_color = Color("#9dbbd6")
	dusk.ambient_light_energy = .55
	dusk.fog_enabled = true
	dusk.fog_light_color = Color("#3d505e")
	dusk.fog_density = .029
	var rng := RandomNumberGenerator.new()
	rng.seed = 1348
	for i in 120:
		var pos := Vector3(rng.randf_range(-4.8,4.8),.015,rng.randf_range(-29,1))
		var width := rng.randf_range(.12,.5)
		box(self,pos,Vector3(width,.035,width*.7),"5b5144").rotation.y = rng.randf()*TAU
	for i in 13:
		var x := i*1.6-9.6
		var z := -31.0-rng.randf_range(0,7)
		var height := rng.randf_range(4.2,8.0)
		box(self,Vector3(x,height*.5,z),Vector3(.32,height,.34),"342e2a")
		for layer in 3:
			var crown := MeshInstance3D.new()
			var cone := CylinderMesh.new()
			cone.radial_segments = 7
			cone.top_radius = 0.0
			cone.bottom_radius = 1.15-layer*.24
			cone.height = height*.48
			crown.mesh = cone
			var material := StandardMaterial3D.new()
			material.albedo_color = Color("#263a3b")
			crown.material_override = material
			add_child(crown)
			crown.position = Vector3(x,height*(.53+layer*.17),z)
	for side in [-1,1]:
		for i in 7:
			box(self,Vector3(side*5.8,1,-18-i*1.32),Vector3(.22,2,.24),"43372f")
		for y in [.62,1.25]:
			box(self,Vector3(side*5.8,y,-22),Vector3(.13,.19,8.8),"524139")
		var torch_pos := Vector3(side*4.8,1.68,-24)
		box(self,torch_pos-Vector3.UP*.82,Vector3(.18,1.7,.18),"49392d")
		var flame := box(self,torch_pos,Vector3(.29,.38,.27),"f68e3b")
		var flame_material := flame.material_override as StandardMaterial3D
		flame_material.emission_enabled = true
		flame_material.emission = Color("#ff8c32")
		var lamp := OmniLight3D.new()
		add_child(lamp)
		lamp.position = torch_pos
		lamp.light_color = Color("#ff994c")
		lamp.light_energy = 2.8
		lamp.omni_range = 8
		lamp.shadow_enabled = true

func show_stage(stage: int) -> void:
	captive.visible = stage in [Stage.ELDER,Stage.DECIDE,Stage.TRACK]
	shutter.visible = captive.visible
	witness.visible = stage in [Stage.SMITH_CLEARED,Stage.BLADE_CLEARED,Stage.GHOUL_CLEARED]

func hint(stage: int) -> String:
	match stage:
		Stage.ELDER: return "Мира привела тебя в селение. Староста ждёт на дороге перед избами."
		Stage.DECIDE: return "Староста: «Засов разнесло, зерно пропало. А кроме Богдашки тут никого».\nСлева — клеть с пленником. Справа — выломанный порог амбара."
		Stage.TRACK: return "Три борозды идут изнутри. Щепа лежит снаружи. Отнеси обломок старосте."
		Stage.SMITH_ESCAPED: return "Богдашка скрылся в чаще. Староста захлопнул дверь. Мира ждёт тебя у кузнеца."
		Stage.SMITH_CLEARED: return "Пленника отпустили. Он замолвил за тебя слово у кузнеца, в конце улицы."
		Stage.BLADE_ESCAPED: return "Кузнец дал Мире под поручительство затупленный клинок. Староста помнит побег. Вернись к кузнецу за уроком."
		Stage.GHOUL_ESCAPED: return "Упырь повержен. Мира просит тебя вернуться к кузнецу. Староста ещё помнит побег."
		Stage.GHOUL_CLEARED: return "Упырь повержен. Богдашка остался у кузницы и видел весь бой."
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
	return stage

func prompt(stage: int, pos: Vector3) -> String:
	var next := next_step(stage,pos)
	if next == stage: return ""
	match next:
		Stage.DECIDE: return "Выслушать старосту"
		Stage.SMITH_ESCAPED: return "Отпереть клеть и выпустить пленника"
		Stage.TRACK: return "Осмотреть сломанный порог"
		Stage.SMITH_CLEARED: return "Предъявить обломок"
	return "Взять учебный клинок"
