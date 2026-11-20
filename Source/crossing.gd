extends "res://Source/walk_scene.gd"

const Slot = preload("res://Source/campaign_slot.gd")
enum Step { YARD, FUSE, CELLAR, GLIMPSE, FOREST, MIRA, CAMP }
const GATE := Vector3(2,0,-10)
const HATCH := Vector3(-4,0,-13)
const FUSE := Vector3(4,0,-5)
const BELL := Vector3(0,0,-12)
const GUIDE := Vector3(-4,0,-17)
const FIRE := Vector3(-2,0,-25)
var step: int = Step.YARD
var village := preload("res://Source/village.gd").new()
var sword := Node3D.new()
var tenant := ""
var evidence := ""
var slot_path := Slot.PATH
var yard := Node3D.new()
var cellar := Node3D.new()
var forest := Node3D.new()
var hero := Node3D.new()
var hud := Label.new()
var action: Button
var keys := {}
var vision_left := 0.0
var message := ""
var chime := AudioStreamPlayer3D.new()
var guide := Node3D.new()


func build_places() -> void:
	for place in [yard,cellar,forest,hero,village]:
		add_child(place)
	box(yard,Vector3(0,-.2,-10),Vector3(23,.4,37),"424a52")
	box(yard,Vector3(2,2.8,-13),Vector3(7,5.6,4),"784e45")
	box(yard,GATE+Vector3(0,1.3,-.9),Vector3(1.7,2.6,.2),"475155")
	box(yard,HATCH,Vector3(1.8,.14,1.8),"302e33")
	box(yard,FUSE+Vector3.UP*.9,Vector3(.8,1.8,.55),"53676a")
	label(yard,GATE+Vector3.UP*3.1,"КОТЕЛЬНАЯ")
	box(cellar,Vector3(0,-.2,-9),Vector3(12,.4,24),"564a45")
	for side in [-1,1]:
		for i in 7:
			box(cellar,Vector3(side*5,1.5,-i*3),Vector3(1,3,2.9),"6d594d")
	box(cellar,BELL+Vector3.UP*3.6,Vector3(4,.4,.5),"513e32")
	for side in [-1,1]:
		box(cellar,BELL+Vector3(side*1.75,1.7,0),Vector3(.35,3.4,.4),"513e32")
	var metal := CylinderMesh.new()
	metal.top_radius = .38
	metal.bottom_radius = .85
	metal.height = 1.1
	var bell := MeshInstance3D.new()
	bell.mesh = metal
	var brass := StandardMaterial3D.new()
	brass.albedo_color = Color("#996d3c")
	bell.material_override = brass
	cellar.add_child(bell)
	bell.position = BELL+Vector3.UP*2.6
	box(cellar,BELL+Vector3.UP*1.75,Vector3(.13,.9,.13),"6d4a35")
	cellar.add_child(chime)
	chime.position = bell.position
	chime.stream = preload("res://Source/gate_investigation.gd").bell_sound()
	box(forest,Vector3(0,-.2,-14),Vector3(45,.4,48),"3f533f")
	for i in 32:
		var side := -1 if i%2 == 0 else 1
		var x := side*(7.4+sin(i*3.1)*1.6)
		var z := 4.0-i*.99
		box(forest,Vector3(x,1.9,z),Vector3(.7,3.8,.7),"604b37")
		box(forest,Vector3(x,4.6,z),Vector3(3.4,3.2,3.2),"344b3b")
	forest.add_child(guide)
	person(guide,Vector3.ZERO,"766649")
	guide.position = GUIDE
	for i in 4:
		box(forest,FIRE+Vector3(.25*cos(i*1.6),.25,.25*sin(i*1.6)),Vector3(.24,.5,.24),"ff9f52")
	person(hero,Vector3.ZERO,"65707a")
	hero.add_child(sword)
	sword.position = Vector3(.57,1,.04)
	box(sword,Vector3(0,.35,0),Vector3(.10,.8,.035),"9b9c97")
	box(sword,Vector3.ZERO,Vector3(.29,.06,.06),"53483b")
	sword.rotation.x = -.65
	var camera := Camera3D.new()
	hero.add_child(camera)
	camera.position = Vector3(0,5.2,6.2)
	camera.rotation.x = -.56
	var sun := DirectionalLight3D.new()
	add_child(sun)
	sun.rotation_degrees = Vector3(-52,-22,0)

func _ready() -> void:
	build_places()
	var ui := CanvasLayer.new()
	add_child(ui)
	ui.add_child(hud)
	hud.position = Vector2(32,24)
	hud.add_theme_font_size_override("font_size",30)
	hud.add_theme_constant_override("outline_size",6)
	var row := HBoxContainer.new()
	ui.add_child(row)
	row.set_anchors_and_offsets_preset(Control.PRESET_BOTTOM_LEFT)
	row.position += Vector2(28,-104)
	for pair in [["ui_left","◀"],["ui_up","▲"],["ui_down","▼"],["ui_right","▶"]]:
		keys[pair[0]] = control(row,pair[1])
	action = control(row,"Осмотреть")
	action.pressed.connect(interact)
	var state: Dictionary = get_tree().get_meta("crossing_entry",{})
	if get_tree().has_meta("crossing_entry"):
		get_tree().remove_meta("crossing_entry")
	slot_path = state.get("slot_path",slot_path)
	step = maxi(0,state.get("crossing",0))
	tenant = state.get("tenant","")
	evidence = state.get("evidence","")
	if step >= village.Stage.SMITH_ESCAPED: hero.position = village.FORGE+Vector3.BACK*3
	if step == Step.MIRA: hero.position = GUIDE+Vector3.BACK*2
	if step == Step.CAMP:
		hero.position = FIRE+Vector3.BACK*2
		guide.position = FIRE+Vector3.LEFT
	if step == Step.GLIMPSE: step = Step.CELLAR
	draw_state()

func target_near(pos: Vector3) -> bool:
	return hero.position.distance_to(pos) < 2.6

func available() -> String:
	if step >= village.Stage.ELDER: return village.prompt(step,hero.position)
	match step:
		Step.YARD:
			if target_near(HATCH) and tenant == "rescued": return "Открыть угольный люк"
			if target_near(GATE): return "Проверить дверь"
		Step.FUSE:
			if target_near(FUSE): return "Включить питание и спуститься"
		Step.CELLAR:
			if target_near(BELL): return "Коснуться колокола"
		Step.FOREST:
			if target_near(GUIDE): return "Окликнуть охотницу"
		Step.MIRA:
			if target_near(FIRE): return "Сесть у огня"
		Step.CAMP:
			if target_near(FIRE): return "Идти с Мирой в селение"
	return ""

func draw_state() -> void:
	yard.visible = step <= Step.FUSE
	cellar.visible = step == Step.CELLAR
	forest.visible = step >= Step.GLIMPSE and step <= Step.CAMP
	village.visible = step >= village.Stage.ELDER
	village.show_stage(step)
	sword.visible = step >= village.Stage.BLADE_ESCAPED
	var lines := ["Архивный адрес · Москва, 2026\nПодойди к двери котельной.", "Замок обесточен. Справа во дворе стоит зелёный щит.",
		"Подвал часовни · На балке висит колокол без верёвки.", "Сквозь камень проступает лес. Звон ещё держится.",
		"Русь · 1348 · Лес чужих голосов\nТелефона и снаряжения нет. Справа зовут знакомым голосом. Слева у берёз стоит охотница.",
		"Мира: «Если лес заговорит твоим голосом — не отвечай. Иди за мной к огню».",
		"Русь · 1348 · Стоянка Миры\nПозади лес. За холмом дымят крыши селения."]
	hud.text = village.hint(step) if village.visible else lines[step]
	if step == Step.YARD and tenant == "rescued": hud.text += "\nВ плане отмечен угольный люк слева от здания."
	if not message.is_empty(): hud.text += "\n" + message
	refresh_action()

func refresh_action() -> void:
	var prompt := available()
	action.visible = not prompt.is_empty()
	action.text = prompt + " [E]"

func enter(next: int, spawn: Vector3) -> void:
	step = next
	if step == Step.CAMP: guide.position = FIRE+Vector3.LEFT
	hero.position = spawn
	message = ""
	if step != Step.GLIMPSE and Slot.store_checkpoint(11,evidence,slot_path,true,tenant,step) != OK:
		message = "Не удалось сохранить контрольную точку."
	draw_state()

func interact() -> void:
	if available().is_empty(): return
	if village.visible:
		enter(village.next_step(step,hero.position),hero.position)
		return
	match step:
		Step.YARD:
			if target_near(HATCH): enter(Step.CELLAR,Vector3(0,0,-6))
			else: enter(Step.FUSE,hero.position)
		Step.FUSE: enter(Step.CELLAR,Vector3.ZERO)
		Step.CELLAR:
			chime.play()
			vision_left = 4
			enter(Step.GLIMPSE,hero.position)
		Step.FOREST: enter(Step.MIRA,hero.position)
		Step.MIRA: enter(Step.CAMP,hero.position)
		Step.CAMP: enter(village.Stage.ELDER,Vector3.ZERO)

func _unhandled_key_input(key: InputEvent) -> void:
	if key is InputEventKey and key.pressed and not key.echo and key.physical_keycode == KEY_E: interact()

func _physics_process(delta: float) -> void:
	var move := Input.get_vector("ui_left","ui_right","ui_up","ui_down")
	move += Vector2(float(keys.ui_right.button_pressed)-float(keys.ui_left.button_pressed),float(keys.ui_down.button_pressed)-float(keys.ui_up.button_pressed))
	move = move.limit_length()
	hero.position += Vector3(move.x,0,move.y)*4.6*delta
	hero.position.x = clampf(hero.position.x,-5,5)
	hero.position.z = clampf(hero.position.z,-27 if forest.visible or village.visible else -17,2)
	if step == Step.GLIMPSE:
		vision_left -= delta
		if vision_left <= 0: enter(Step.FOREST,Vector3.ZERO)
	elif step == Step.FOREST and hero.position.x > 1.8 and hero.position.z < -13:
		hero.position = Vector3(0,0,-5)
		message = "Та же сломанная ветка. Тропа вернула тебя назад."
		draw_state()
	elif step == Step.MIRA:
		guide.position = guide.position.move_toward(FIRE+Vector3.LEFT,delta*2.3)
	refresh_action()
