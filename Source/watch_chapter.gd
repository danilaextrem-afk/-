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
var village := preload("res://Source/watch_village.gd").new()
var fight := preload("res://Source/forge_combat.gd").new()
var fighting := false
var strike_button: Button
var block_button: Button
var block_held := false
var figure := Node3D.new()
var character_animator: AnimationPlayer
var meters := preload("res://Source/forge_hud.gd").new()
var sword := Node3D.new()
var tenant := ""
var evidence := ""
const CHAPTER_PATH := "user://forge_watch_v1.cfg"
var slot_path := CHAPTER_PATH
var yard := Node3D.new()
var cellar := Node3D.new()
var forest := Node3D.new()
var hero := Node3D.new()
var hud := Label.new()
var action: Button
var keys := {}
var vision_left := 0.0
var message := ""
var camera := Camera3D.new()
var sun := DirectionalLight3D.new()
var chime := AudioStreamPlayer3D.new()
var guide := Node3D.new()


func build_places() -> void:
	for place in [yard,cellar,forest,hero,village,fight]:
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
	hero.add_child(figure)
	var character := preload("res://Art/Forge/RoadWanderer.glb").instantiate() as Node3D
	figure.add_child(character)
	character.scale = Vector3.ONE*.111
	character.rotation.y = PI
	character_animator = character.find_child("AnimationPlayer",true,false)
	for clip in ["Walk","Idle"]:
		character_animator.get_animation(clip).loop_mode = Animation.LOOP_LINEAR
	character_animator.play("Idle")
	var skeleton := character.find_children("*","Skeleton3D",true,false)[0] as Skeleton3D
	var grip := BoneAttachment3D.new()
	skeleton.add_child(grip)
	grip.bone_name = "hand_R"
	grip.add_child(sword)
	sword.scale = Vector3.ONE/.111
	sword.position = Vector3(0,.48,0)
	sword.rotation = Vector3(.16,0,1.31)
	sword.add_child(preload("res://Art/Forge/SmithBlade.glb").instantiate())
	hero.add_child(camera)
	camera.position = Vector3(0,5.2,6.2)
	camera.rotation.x = -.56
	add_child(sun)
	sun.rotation_degrees = Vector3(-52,-22,0)

func _ready() -> void:
	build_places()
	var ui := CanvasLayer.new()
	add_child(ui)
	ui.add_child(meters)
	meters.mouse_filter = Control.MOUSE_FILTER_IGNORE
	meters.position = Vector2(54,40)
	ui.add_child(hud)
	hud.position = Vector2(54,40)
	hud.add_theme_font_size_override("font_size",25)
	hud.add_theme_constant_override("outline_size",4)
	var row := HBoxContainer.new()
	ui.add_child(row)
	row.set_anchors_and_offsets_preset(Control.PRESET_BOTTOM_LEFT)
	row.position += Vector2(28,-104)
	for pair in [["ui_left","◀"],["ui_up","▲"],["ui_down","▼"],["ui_right","▶"]]:
		keys[pair[0]] = control(row,pair[1])
	for button in keys.values():
		button.visible = DisplayServer.is_touchscreen_available()
	action = control(row,"Осмотреть")
	action.pressed.connect(interact)
	strike_button = control(row,"Удар [F]")
	strike_button.pressed.connect(strike)
	block_button = control(row,"Блок [Q]")
	block_button.button_down.connect(func(): block_held = true)
	block_button.button_up.connect(func(): block_held = false)
	var state: Dictionary = get_tree().get_meta("watch_entry",{})
	if get_tree().has_meta("watch_entry"):
		get_tree().remove_meta("watch_entry")
	slot_path = state.get("slot_path",slot_path)
	step = maxi(0,state.get("crossing",13))
	tenant = state.get("tenant","rescued")
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
	if fighting: return ""
	if step in [village.Stage.WATCH_ESCAPED,village.Stage.WATCH_CLEARED]: return "Вернуться в гараж"
	if step in [village.Stage.BLADE_ESCAPED,village.Stage.BLADE_CLEARED] and target_near(village.FORGE):
		return "Начать боевой урок"
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
	village.alert = fighting
	fight.visible = fighting or (fight.enemy_health == 0 and step >= village.Stage.GHOUL_ESCAPED)
	meters.visible = fighting
	hud.position = Vector2(54,146) if fighting else Vector2(54,40)
	camera.position = Vector3(.83,1.68,1.82) if fighting else (Vector3(.48,2.15,3.7) if step >= village.Stage.ELDER else Vector3(0,5.2,6.2))
	camera.fov = 63.0 if fighting else 75.0
	camera.rotation.x = -.026 if fighting else (-.21 if step >= village.Stage.ELDER else -.56)
	strike_button.visible = fighting
	block_button.visible = fighting
	yard.visible = step <= Step.FUSE
	cellar.visible = step == Step.CELLAR
	forest.visible = step >= Step.GLIMPSE and step <= Step.CAMP
	village.visible = step >= village.Stage.ELDER
	village.show_stage(step)
	camera.environment = village.dusk if village.visible else null
	sun.light_energy = .91 if village.visible else 1.0
	sun.light_color = Color("#b5c7da") if village.visible else Color.WHITE
	sun.rotation_degrees = Vector3(-28,-46,0) if village.visible else Vector3(-52,-22,0)
	sun.shadow_enabled = village.visible
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
	if step in [village.Stage.WATCH_ESCAPED,village.Stage.WATCH_CLEARED]:
		get_tree().change_scene_to_file("res://Content/NightRoad.tscn")
		return
	if step in [village.Stage.BLADE_ESCAPED,village.Stage.BLADE_CLEARED]:
		fight.reset()
		fighting = true
		message = ""
		hero.position = fight.START
		draw_state()
		advance_fight(0.0)
		return
	if village.visible:
		var next := village.next_step(step,hero.position)
		if next in [village.Stage.BELL_ESCAPED,village.Stage.BELL_CLEARED]:
			chime.reparent(self)
			chime.position = hero.position + Vector3(0,3,-5)
			chime.play()
		enter(next,hero.position)
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

func strike() -> void:
	if fighting:
		fight.strike()

func _unhandled_key_input(key: InputEvent) -> void:
	if key is InputEventKey and key.pressed and not key.echo:
		if key.physical_keycode == KEY_E: interact()
		if key.physical_keycode == KEY_F: strike()

func advance_fight(delta: float) -> void:
	var blocking := Input.is_physical_key_pressed(KEY_Q) or block_held
	fight.advance(delta,hero.position,blocking)
	fight.pose()
	if fight.enemy_health == 0 or fight.health == 0:
		fighting = false
		block_held = false
		if fight.enemy_health == 0:
			fight.animator.play("Dying",.12)
			enter(step+2,hero.position)
		else:
			hero.position = village.FORGE+Vector3.BACK*1.5
			message = "Мира оттащила тебя к кузнице. Можно повторить урок."
			draw_state()
		return
	meters.update_fight(fight,blocking)
	hud.text = "Упырь замахнулся" if fight.windup > 0 else "Первый клинок  ·  Кузнечный двор"

func _physics_process(delta: float) -> void:
	var move := Input.get_vector("ui_left","ui_right","ui_up","ui_down")
	move += Vector2(float(keys.ui_right.button_pressed)-float(keys.ui_left.button_pressed),float(keys.ui_down.button_pressed)-float(keys.ui_up.button_pressed))
	move = move.limit_length()
	var direction := Vector3(move.x,0,move.y)
	if fighting:
		var to_enemy: Vector3 = fight.enemy.position - hero.position
		if to_enemy.length_squared() > .001:
			figure.rotation.y = lerp_angle(figure.rotation.y,atan2(-to_enemy.x,-to_enemy.z),minf(1,delta*12))
	elif direction.length_squared() > .01:
		figure.rotation.y = lerp_angle(figure.rotation.y,atan2(-direction.x,-direction.z),minf(1,delta*12))
	var clip := "Walk" if move.length_squared() > .01 else "Idle"
	if fighting:
		if fight.swing > 0:
			clip = "Strike"
		elif block_held or Input.is_physical_key_pressed(KEY_Q) or move.length_squared() < .01:
			clip = "Guard"
	if clip == "Strike":
		character_animator.play(clip)
		character_animator.seek(character_animator.get_animation(clip).length*(1.0-fight.swing/.55),true)
		character_animator.pause()
	elif clip == "Guard":
		character_animator.play(clip)
		character_animator.seek(character_animator.get_animation(clip).length*(1.0 if block_held or Input.is_physical_key_pressed(KEY_Q) else .78),true)
		character_animator.pause()
	elif character_animator.current_animation != clip or not character_animator.is_playing():
		character_animator.play(clip,.16)
	hero.position += direction*4.6*delta
	hero.position.x = clampf(hero.position.x,-5,5)
	hero.position.z = clampf(hero.position.z,-27 if forest.visible or village.visible else -17,2)
	if fighting:
		advance_fight(delta)
	elif step == Step.GLIMPSE:
		vision_left -= delta
		if vision_left <= 0: enter(Step.FOREST,Vector3.ZERO)
	elif step == Step.FOREST and hero.position.x > 1.8 and hero.position.z < -13:
		hero.position = Vector3(0,0,-5)
		message = "Та же сломанная ветка. Тропа вернула тебя назад."
		draw_state()
	elif step == Step.MIRA:
		guide.position = guide.position.move_toward(FIRE+Vector3.LEFT,delta*2.3)
	refresh_action()
