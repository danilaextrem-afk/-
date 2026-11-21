extends "res://Source/walk_scene.gd"

# Values follow the camp checkpoint (6) in the campaign save.
enum Stage { ELDER = 7, DECIDE, TRACK, SMITH_ESCAPED, SMITH_CLEARED, BLADE_ESCAPED, BLADE_CLEARED, GHOUL_ESCAPED, GHOUL_CLEARED, BELL_ESCAPED, BELL_CLEARED }
const ELDER := Vector3(0,0,-5)
const PRISONER := Vector3(-4,0,-12)
const GRANARY := Vector3(4,0,-14)
const FORGE := Vector3(1,0,-23)
var lost_key := Node3D.new()
var dusk: Environment
var captive := Node3D.new()
var shutter := Node3D.new()
var witness := Node3D.new()

func _ready() -> void:
	var ground := box(self,Vector3(0,-.25,-15),Vector3(70,.5,80),"ffffff")
	var soil := ground.material_override as StandardMaterial3D
	soil.albedo_color = Color("#9d9c93")
	soil.albedo_texture = preload("res://Art/earth.jpg")
	soil.uv1_triplanar = true
	soil.uv1_scale = Vector3.ONE * .33
	var lane := MeshInstance3D.new()
	lane.mesh = preload("res://Art/Forge/smiths_lane.obj")
	add_child(lane)
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
	resident(self,ELDER)
	add_child(captive)
	resident(captive,PRISONER)
	add_child(shutter)
	for i in 5:
		box(shutter,PRISONER+Vector3(-.8+i*.4,1.1,.65),Vector3(.13,2.2,.16),"4f3f32")
	add_child(witness)
	resident(witness,FORGE+Vector3(-3,0,-1))
	resident(self,FORGE+Vector3(1.7,0,-1))

	box(self,GRANARY+Vector3.UP*.2,Vector3(1.8,.4,1.1),"705438")
	for i in 3:
		box(self,GRANARY+Vector3(i*.2-.3,.415,0),Vector3(.05,.02,.65),"312822").rotation.y = -.3

func build_forge_yard() -> void:
	dusk = Environment.new()
	dusk.background_mode = Environment.BG_SKY
	dusk.sky = Sky.new()
	var panorama := PanoramaSkyMaterial.new()
	panorama.panorama = preload("res://Art/dusk_panorama.jpg")
	dusk.sky.sky_material = panorama
	dusk.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
	dusk.ambient_light_color = Color("#cfdae0")
	dusk.ambient_light_energy = .86
	dusk.fog_enabled = true
	dusk.fog_light_color = Color("#9ca8b1")
	dusk.fog_density = .008
	dusk.fog_sky_affect = .08
	for pos in [Vector3(3.65,1,-25.6),Vector3(-5.5,1.4,-25)]:
		var lamp := OmniLight3D.new()
		add_child(lamp)
		lamp.position = pos
		lamp.light_color = Color("#ff9646")
		lamp.light_energy = 3.8
		lamp.omni_range = 8.0

func resident(parent: Node3D, pos: Vector3) -> void:
	var body := MeshInstance3D.new()
	body.mesh = preload("res://Art/Forge/traveller.obj")
	parent.add_child(body)
	body.position = pos
	body.rotation.y = 2.36 if pos.x > 0 else -2.36

func show_stage(stage: int) -> void:
	lost_key.visible = stage in [Stage.GHOUL_ESCAPED,Stage.GHOUL_CLEARED]
	captive.visible = stage in [Stage.ELDER,Stage.DECIDE,Stage.TRACK]
	shutter.visible = captive.visible
	witness.visible = stage in [Stage.SMITH_CLEARED,Stage.BLADE_CLEARED,Stage.GHOUL_CLEARED,Stage.BELL_CLEARED]

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
		Stage.BELL_ESCAPED: return "На ключе вырезана зарубка. Придётся самому узнать, к какой двери он подходит."
		Stage.BELL_CLEARED: return "Богдашка узнал ключ пропавшего сторожа. За частоколом стих последний удар."
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
	return stage

func prompt(stage: int, pos: Vector3) -> String:
	var next := next_step(stage,pos)
	if next == stage: return ""
	match next:
		Stage.BELL_ESCAPED,Stage.BELL_CLEARED: return "Поднять ключ сторожа"
		Stage.DECIDE: return "Выслушать старосту"
		Stage.SMITH_ESCAPED: return "Отпереть клеть и выпустить пленника"
		Stage.TRACK: return "Осмотреть сломанный порог"
		Stage.SMITH_CLEARED: return "Предъявить обломок"
	return "Взять учебный клинок"
