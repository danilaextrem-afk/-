extends Node3D

const Slot = preload("res://Source/campaign_slot.gd")
enum Stage { ILYA, RECORDER, CAMERA, NEIGHBOR, ENTRANCE, LANDING, FALSE_DOOR, SIGN }
const TARGETS := [Vector3(0,0,-6), Vector3(4,0,-8), Vector3(-4,0,-13), Vector3(3,0,-23), Vector3(0,0,-29), Vector3(0,0,-6), Vector3(3,0,-13), Vector3(-3,0,-16)]
const ACTIONS := ["Поговорить с Ильёй","Проверить запись","Прочитать шильдик","Выслушать соседку","Войти в подъезд № 9","Осмотреть площадку","Посветить на замок","Зарисовать знак"]
const HINTS := [
	"Илья ждёт под вывеской мастерской.",
	"Илья: «Мне ролик тоже переслали. Оригинал ищи на регистраторе у верстака».",
	"В начале файла метка ДВ-03. Найди камеру с этим номером у выхода из бокса.",
	"На шильдике ДВ-03. Камера смотрит в соседний двор, а на записи горит деревня. Кто-то зовёт от дома.",
	"Соседка: «Саша пошёл за ключами. Слышу его за дверью, а домой он так и не вернулся». Найди девятый подъезд.",
	"Дверь захлопнулась. У почтовых ящиков мелькнула тень. Дойди до площадки.",
	"Голос соседки доносится из квартиры справа. Включи фонарь и осмотри замок.",
	"У двери нет замочной скважины. Слева под краской видна насечка. Зарисуй её."
]
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
var notice := ""
var sign_mark := Node3D.new()

func box(parent: Node3D, pos: Vector3, size: Vector3, color: String) -> MeshInstance3D:
	var m := BoxMesh.new()
	m.size = size
	var part := MeshInstance3D.new()
	part.mesh = m
	var mat := StandardMaterial3D.new()
	mat.albedo_color = Color(color)
	part.material_override = mat
	parent.add_child(part)
	part.position = pos
	return part

func label(parent: Node3D, pos: Vector3, s: String) -> Label3D:
	var l := Label3D.new()
	l.text = s
	l.position = pos
	l.font_size = 40
	l.pixel_size = 0.014
	parent.add_child(l)
	return l

func person(parent: Node3D, pos: Vector3, coat: String) -> void:
	box(parent,pos+Vector3.UP,Vector3(.72,1.3,.5),coat)
	box(parent,pos+Vector3.UP*1.93,Vector3(.43,.49,.42),"b18c74")
	for side in [-1,1]:
		box(parent,pos+Vector3(side*.18,.25,0),Vector3(.27,.5,.36),"282931")

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
	box(outside,TARGETS[1]+Vector3.UP*.8,Vector3(2,1.6,1),"635047")
	box(outside,TARGETS[1]+Vector3.UP*1.8,Vector3(.7,.45,.35),"25323c")
	box(outside,TARGETS[2]+Vector3.UP*1.6,Vector3(.18,3.2,.18),"4d5661")
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
		box(inside,Vector3(door_x,1.4,-16.8),Vector3(1.75,2.8,.32),"675451")
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

func control(row: HBoxContainer, title: String) -> Button:
	var b := Button.new()
	b.text = title
	b.custom_minimum_size = Vector2(140,84)
	b.focus_mode = Control.FOCUS_NONE
	row.add_child(b)
	return b

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
	control(row,"Фонарь [F]").pressed.connect(toggle_light)
	control(row,"Журнал").pressed.connect(func(): logbook.visible = not logbook.visible)
	var state: Dictionary = get_tree().get_meta("district_entry",{})
	if get_tree().has_meta("district_entry"):
		get_tree().remove_meta("district_entry")
	stage = state.get("stage",0)
	evidence = state.get("evidence","")
	finished = state.get("finished",false)
	if stage > 0:
		avatar.position = TARGETS[stage] + Vector3(0,0,2.8)
	set_view()

func set_view() -> void:
	outside.visible = stage < Stage.LANDING
	inside.visible = not outside.visible
	sign_mark.visible = stage == Stage.SIGN
	shadow.visible = stage == Stage.LANDING
	marker.position = TARGETS[stage] + Vector3(0,2.75,0)
	marker.text = ACTIONS[stage]
	marker.visible = not finished
	hud.text = "Москва · " + ("Девятый подъезд" if inside.visible else "Двор автосервиса") + "\n"
	hud.text += "Первый знак Завесы зарисован. След в журнале." if finished else HINTS[stage]
	if not notice.is_empty():
		hud.text += "\n" + notice
	logbook.text = "Телефон Кирилла\n" + evidence
	if stage >= Stage.NEIGHBOR:
		logbook.text += "\nДВ-03 снимала двор. В исходном файле — пожар."
	if stage >= Stage.ENTRANCE:
		logbook.text += "\nПропал Александр, жилец девятого подъезда."
	if finished:
		logbook.text += "\nНа косяке: разомкнутое кольцо, перечёркнутое чертой."
	update_action()

func toggle_light() -> void:
	torch.visible = not torch.visible
	notice = "Фонарь включён." if torch.visible else "Фонарь выключен."
	set_view()

func update_action() -> void:
	action.visible = not finished and avatar.position.distance_to(TARGETS[stage]) < 2.7
	action.text = ACTIONS[stage] + " [E]"

func interact() -> void:
	if finished or avatar.position.distance_to(TARGETS[stage]) >= 2.7:
		return
	if stage == Stage.FALSE_DOOR and not torch.visible:
		notice = "В темноте не видно замка. Нажми F или «Фонарь»."
		set_view()
		return
	notice = ""
	if stage == Stage.SIGN:
		finished = true
	else:
		stage += 1
		if stage == Stage.LANDING:
			avatar.position = Vector3.ZERO
	if Slot.store_checkpoint(stage,evidence,slot_path,finished) != OK:
		notice = "Не удалось записать контрольную точку."
	set_view()

func _unhandled_key_input(key: InputEvent) -> void:
	if key is InputEventKey and key.pressed and not key.echo:
		match key.physical_keycode:
			KEY_E: interact()
			KEY_F: toggle_light()

func _physics_process(delta: float) -> void:
	var direction := Input.get_vector("ui_left","ui_right","ui_up","ui_down")
	direction += Vector2(float(keys.ui_right.button_pressed)-float(keys.ui_left.button_pressed),float(keys.ui_down.button_pressed)-float(keys.ui_up.button_pressed))
	direction = direction.limit_length()
	avatar.position += Vector3(direction.x,0,direction.y)*delta*4.6
	avatar.position.x = clampf(avatar.position.x,-5,5)
	avatar.position.z = clampf(avatar.position.z,-19 if inside.visible else -30,1)
	if shadow.visible:
		shadow.position.x = 2.8*sin(Time.get_ticks_msec()*.001)
	update_action()
