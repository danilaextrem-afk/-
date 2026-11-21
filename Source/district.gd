extends "res://Source/walk_scene.gd"

const Slot = preload("res://Source/campaign_slot.gd")
enum Stage { ILYA, RECORDER, CAMERA, NEIGHBOR, ENTRANCE, LANDING, FALSE_DOOR, SIGN, LISTEN, TENANT, MARINA, ARCHIVE }
const TARGETS := [Vector3(0,0,-6), Vector3(4,0,-8), Vector3(-4,0,-13), Vector3(3,0,-23), Vector3(0,0,-29), Vector3(0,0,-6), Vector3(3,0,-13), Vector3(-3,0,-16), Vector3(-4,0,-8), Vector3(-4,0,-8), Vector3(2,0,-20), Vector3(-2,0,-13)]
const ACTIONS := ["Поговорить с Ильёй","Проверить запись","Прочитать шильдик","Выслушать соседку","Войти в подъезд № 9","Осмотреть площадку","Посветить на замок","Зарисовать знак","Ответить на стук","Помочь выйти","Поговорить с Мариной","Сверить архивный план"]
const HINTS := [
	"Илья ждёт под вывеской мастерской.",
	"Илья: «Мне ролик тоже переслали. Оригинал ищи на регистраторе у верстака».",
	"В начале файла метка ДВ-03. Найди камеру с этим номером у выхода из бокса.",
	"На шильдике ДВ-03. Камера смотрит в соседний двор, а на записи горит деревня. Кто-то зовёт от дома.",
	"Соседка: «Саша пошёл за ключами. Слышу его за дверью, а домой он так и не вернулся». Найди девятый подъезд.",
	"Дверь захлопнулась. У почтовых ящиков мелькнула тень. Дойди до площадки.",
	"Голос соседки доносится из квартиры справа. Включи фонарь и осмотри замок.",
	"У двери нет замочной скважины. Слева под краской видна насечка. Зарисуй её.",
	"Из квартиры зовут голосом соседки. Отойди от двери и прислушайся к трубам.",
	"Александр: «Два коротких, один длинный. Это я. Засов снаружи!» Можно открыть или вызвать помощь.",
	"Во дворе у лавки ждёт журналистка Марина Белова.",
	"На столе разложена старая карта. Сопоставь отметку у часовни с зарисовкой."

]
const WALK_RADIUS := 0.36
var yard_obstacles: Array[Rect2] = []
var landing_obstacles: Array[Rect2] = []
var stage := Stage.ILYA
var evidence := ""
var avatar := Node3D.new()
var outside := Node3D.new()
var inside := Node3D.new()
var marker := Label3D.new()
var shadow := Node3D.new()
var torch := OmniLight3D.new()
var hud := Label.new()
var action: Button
var logbook := Label.new()
var keys := {}
var slot_path := Slot.PATH
var finished := false
var tenant := ""
var alternative: Button
var knock := preload("res://Source/pipe_knock.gd").new()
var archive_sheet := Node3D.new()
var knock_wait := 0.0
var survivor := Node3D.new()
var notice := ""
var sign_mark := Node3D.new()

func obstacle(layer: Node3D, center: Vector3, size: Vector3) -> void:
	var extent := Vector2(size.x, size.z)
	var bounds := Rect2(Vector2(center.x, center.z) - extent * .5, extent).grow(WALK_RADIUS)
	if layer == outside:
		yard_obstacles.append(bounds)
	else:
		landing_obstacles.append(bounds)

func solid_box(layer: Node3D, center: Vector3, size: Vector3, color: String) -> void:
	box(layer, center, size, color)
	obstacle(layer, center, size)

func build_bench(center: Vector3) -> void:
	var bench := Node3D.new()
	outside.add_child(bench)
	bench.position = center
	for end in [-.9, .9]:
		for side in [-.22, .22]:
			box(bench,Vector3(end,.22,side),Vector3(.08,.44,.08),"484e50")
		box(bench,Vector3(end,.65,-.23),Vector3(.08,.70,.08),"484e50")
	for slat in [-.18,0,.18]:
		box(bench,Vector3(0,.47,slat),Vector3(2.2,.07,.15),"89735c")
	for height in [.75,.94]:
		box(bench,Vector3(0,height,-.24),Vector3(2.2,.14,.06),"79634e")
	obstacle(outside,center,Vector3(2.2,1,.56))

# Sweep one horizontal axis at a time so contact allows sliding along a face.
func walk_axis(point: Vector2, axis: int, distance: float, bounds: Array[Rect2]) -> Vector2:
	var across := 1 - axis
	var destination := point[axis] + distance
	for area in bounds:
		if point[across] <= area.position[across] or point[across] >= area.end[across]:
			continue
		if distance > 0 and point[axis] <= area.position[axis]:
			destination = minf(destination, area.position[axis])
		elif distance < 0 and point[axis] >= area.end[axis]:
			destination = maxf(destination, area.end[axis])
	point[axis] = destination
	return point

func make_world() -> void:
	for node in [outside,inside,avatar,marker]:
		add_child(node)
	box(outside,Vector3(0,-.2,-15),Vector3(30,.4,42),"464c53")
	for side in [-1,1]:
		box(outside,Vector3(side*10,4,-17),Vector3(6,8,36),"786e65")
		for floor_id in range(3):
			for col in range(10):
				box(outside,Vector3(side*6.95,1.6+floor_id*2.4,-col*3.3),Vector3(.09,1.2,1.3),"d9bd86")
	label(outside,Vector3(0,3,-7),"АВТОСЕРВИС")
	person(outside,TARGETS[0],"556c7d")
	solid_box(outside,TARGETS[1]+Vector3.UP*.8,Vector3(2,1.6,1),"635047")
	box(outside,TARGETS[1]+Vector3.UP*1.8,Vector3(.7,.45,.35),"25323c")
	solid_box(outside,TARGETS[2]+Vector3.UP*1.6,Vector3(.18,3.2,.18),"4d5661")
	label(outside,TARGETS[2]+Vector3.UP*3.3,"ДВ-03")
	person(outside,TARGETS[3],"8e534d")
	box(outside,Vector3(0,2,-31),Vector3(8,4,1),"7d7267")
	box(outside,Vector3(0,1.35,-30.4),Vector3(1.8,2.7,.2),"345456")
	label(outside,Vector3(0,3.2,-30.3),"9")
	box(inside,Vector3(0,-.2,-9),Vector3(13,.4,24),"77726b")
	for side in [-1,1]:
		box(inside,Vector3(side*6,1.6,-9),Vector3(.5,3.2,23),"66786e")
	for i in range(5):
		box(inside,Vector3(-3+i*.6,1.4,-7),Vector3(.53,.75,.3),"737e86")
	for door_x in [-3,3]:
		solid_box(inside,Vector3(door_x,1.4,-16.8),Vector3(1.75,2.8,.32),"675451")
	inside.add_child(sign_mark)
	sign_mark.position = Vector3(-3,1.45,-16.58)
	var ring := TorusMesh.new()
	ring.inner_radius = .22
	ring.outer_radius = .29
	var seal := MeshInstance3D.new()
	seal.mesh = ring
	seal.rotation.x = PI/2
	sign_mark.add_child(seal)
	box(sign_mark,Vector3(0,0,.02),Vector3(.045,.75,.05),"d5bf97").rotation.z = -.45
	inside.add_child(knock)
	knock.position = TARGETS[Stage.LISTEN] + Vector3.UP
	solid_box(inside,knock.position,Vector3(.22,2.7,.22),"8b7158")
	outside.add_child(survivor)
	person(survivor,Vector3(-1,0,-20),"4f6454")
	build_bench(Vector3(3.7,0,-21.4))
	person(outside,TARGETS[Stage.MARINA],"99663d")
	solid_box(outside,TARGETS[Stage.ARCHIVE]+Vector3.UP*.76,Vector3(2,.14,1.1),"765943")
	box(outside,TARGETS[Stage.ARCHIVE]+Vector3.UP*.85,Vector3(1.4,.025,.8),"dcc69f")
	outside.add_child(archive_sheet)
	archive_sheet.position = TARGETS[Stage.ARCHIVE]+Vector3.UP*.875
	for offset in [-.37,0,.37]:
		box(archive_sheet,Vector3(offset,0,0),Vector3(.022,.012,.67),"655d50")
	box(archive_sheet,Vector3(0,0,.14),Vector3(1.19,.012,.022),"655d50")
	box(archive_sheet,Vector3(.23,.008,-.13),Vector3(.19,.012,.14),"884235")
	inside.add_child(shadow)
	person(shadow,Vector3.ZERO,"15121e")
	shadow.position = Vector3(2.8,0,-9)
	person(avatar,Vector3.ZERO,"475b68")
	avatar.add_child(torch)
	torch.position = Vector3(0,1.4,-1)
	torch.omni_range = 9
	torch.light_color = Color("#fff1cc")
	torch.light_energy = 4
	torch.hide()
	var camera := Camera3D.new()
	avatar.add_child(camera)
	camera.position = Vector3(0,5.5,6.5)
	camera.rotation.x = -.57
	var sun := DirectionalLight3D.new()
	add_child(sun)
	sun.rotation_degrees = Vector3(-58,-25,0)
	marker.billboard = BaseMaterial3D.BILLBOARD_ENABLED
	marker.pixel_size = .014
	marker.modulate = Color("#f4c480")

func _ready() -> void:
	make_world()
	var ui := CanvasLayer.new()
	add_child(ui)
	ui.add_child(hud)
	hud.position = Vector2(32,24)
	hud.add_theme_font_size_override("font_size",27)
	hud.add_theme_constant_override("outline_size",6)
	ui.add_child(logbook)
	logbook.position = Vector2(32,180)
	logbook.add_theme_font_size_override("font_size",26)
	logbook.add_theme_constant_override("outline_size",6)
	logbook.hide()
	var row := HBoxContainer.new()
	ui.add_child(row)
	row.set_anchors_and_offsets_preset(Control.PRESET_BOTTOM_LEFT)
	row.position += Vector2(28,-104)
	for pair in [["ui_left","◀"],["ui_up","▲"],["ui_down","▼"],["ui_right","▶"]]:
		keys[pair[0]] = control(row,pair[1])
	action = control(row,"Осмотреть [E]")
	action.pressed.connect(interact)
	alternative = control(row,"Вызвать помощь [R]")
	alternative.pressed.connect(call_help)
	control(row,"Фонарь [F]").pressed.connect(toggle_light)
	control(row,"Журнал").pressed.connect(func(): logbook.visible = not logbook.visible)
	var state: Dictionary = get_tree().get_meta("district_entry",{})
	if get_tree().has_meta("district_entry"):
		get_tree().remove_meta("district_entry")
	slot_path = state.get("slot_path",slot_path)
	stage = state.get("stage",0)
	evidence = state.get("evidence","")
	tenant = state.get("tenant","")
	finished = state.get("finished",false)
	if stage > 0:
		avatar.position = TARGETS[stage] + Vector3(0,0,2.8)
	set_view()

func set_view() -> void:
	outside.visible = stage < Stage.LANDING or stage >= Stage.MARINA
	survivor.visible = tenant == "rescued"
	archive_sheet.visible = stage == Stage.ARCHIVE
	inside.visible = not outside.visible
	sign_mark.visible = stage >= Stage.SIGN
	shadow.visible = stage == Stage.LANDING
	marker.position = TARGETS[stage] + Vector3(0,2.75,0)
	marker.text = ACTIONS[stage]
	marker.visible = not finished and stage != Stage.LISTEN
	hud.text = "Москва · " + ("Девятый подъезд" if inside.visible else "Двор автосервиса") + "\n"
	hud.text += "Архивная отметка совпала: старая часовня стояла на месте котельной. Адрес записан." if finished else HINTS[stage]
	if stage == Stage.ARCHIVE:
		hud.text += "\n" + ("Марина: «Саша подтвердил твои слова. Бери мой оригинал плана»." if tenant == "rescued" else "Марина: «Пока бригада разбирается, сверим только копию».")
	if not notice.is_empty():
		hud.text += "\n" + notice
	logbook.text = "Телефон Кирилла\n" + evidence
	if stage >= Stage.NEIGHBOR:
		logbook.text += "\nДВ-03 снимала двор. В исходном файле — пожар."
	if stage >= Stage.ENTRANCE:
		logbook.text += "\nПропал Александр, жилец девятого подъезда."
	if stage > Stage.SIGN:
		logbook.text += "\nНа косяке: разомкнутое кольцо, перечёркнутое чертой."
	if stage >= Stage.MARINA:
		logbook.text += "\n" + ("Александр вышел со мной." if tenant == "rescued" else "Спасателям передан номер подъезда.")
	if finished:
		logbook.text += "\nАрхив: под котельной сохранился подвал часовни."
		if tenant == "rescued":
			logbook.text += "\nПомета на обороте оригинала: второй спуск за угольным люком."
	update_action()

func toggle_light() -> void:
	torch.visible = not torch.visible
	notice = "Фонарь включён." if torch.visible else "Фонарь выключен."
	set_view()

func update_action() -> void:
	action.visible = avatar.position.distance_to(TARGETS[stage]) < 2.7
	action.text = ACTIONS[stage] + " [E]"
	if finished:
		action.text = "К часовне [E]"
	alternative.visible = action.visible and stage == Stage.TENANT

func interact() -> void:
	if avatar.position.distance_to(TARGETS[stage]) >= 2.7:
		return
	if finished:
		if Slot.store_checkpoint(stage,evidence,slot_path,true,tenant,0) == OK:
			var entry := Slot.resume(slot_path)
			entry["slot_path"] = slot_path
			get_tree().set_meta("crossing_entry",entry)
			get_tree().change_scene_to_file("res://Content/Crossing.tscn")
		else:
			notice = "Не удалось записать контрольную точку."
			set_view()
		return
	if stage == Stage.FALSE_DOOR and not torch.visible:
		notice = "В темноте не видно замка. Нажми F или «Фонарь»."
		set_view()
		return
	notice = ""
	if stage == Stage.TENANT and tenant.is_empty():
		tenant = "rescued"
	if stage == Stage.ARCHIVE:
		finished = true
	else:
		stage += 1
		if stage == Stage.LANDING:
			avatar.position = Vector3.ZERO
		elif stage == Stage.MARINA:
			avatar.position = TARGETS[Stage.ENTRANCE]
	if Slot.store_checkpoint(stage,evidence,slot_path,finished,tenant) != OK:
		notice = "Не удалось записать контрольную точку."
	set_view()

func call_help() -> void:
	if stage == Stage.TENANT and avatar.position.distance_to(TARGETS[stage]) < 2.7:
		tenant = "called"
		interact()

func _unhandled_key_input(key: InputEvent) -> void:
	if key is InputEventKey and key.pressed and not key.echo:
		match key.physical_keycode:
			KEY_E: interact()
			KEY_F: toggle_light()
			KEY_R: call_help()

func _physics_process(delta: float) -> void:
	var direction := Input.get_vector("ui_left","ui_right","ui_up","ui_down")
	direction += Vector2(float(keys.ui_right.button_pressed)-float(keys.ui_left.button_pressed),float(keys.ui_down.button_pressed)-float(keys.ui_up.button_pressed))
	direction = direction.limit_length()
	var bounds := landing_obstacles if inside.visible else yard_obstacles
	var point := Vector2(avatar.position.x, avatar.position.z)
	point = walk_axis(point, 0, direction.x * delta * 4.6, bounds)
	point = walk_axis(point, 1, direction.y * delta * 4.6, bounds)
	avatar.position = Vector3(point.x, avatar.position.y, point.y)
	avatar.position.x = clampf(avatar.position.x,-5,5)
	avatar.position.z = clampf(avatar.position.z,-19 if inside.visible else -30,1)
	if stage == Stage.LISTEN:
		knock_wait -= delta
		if knock_wait <= 0:
			knock.play()
			knock_wait = 3.0
		var near_pipe := avatar.position.distance_to(TARGETS[stage]) < 5.5
		hud.text = HINTS[stage] + ("\n[Рядом: три удара по трубе.]" if near_pipe else "\n[Глухой стук у левой стены, ближе к почтовым ящикам.]")
	if shadow.visible:
		shadow.position.x = 2.8*sin(Time.get_ticks_msec()*.001)
	update_action()
