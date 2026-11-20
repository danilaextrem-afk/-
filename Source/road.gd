extends Node3D

const Glimpse = preload("res://Source/time_glimpse.gd")
const Investigation = preload("res://Source/gate_investigation.gd")
var quest := Investigation.new()
var journal := Label.new()
var action: Button
var clock := Glimpse.new()
var event: Dictionary = JSON.parse_string(FileAccess.get_file_as_string("res://GameData/road_glimpse.json"))
var forest := Node3D.new()
var city := Node3D.new()
var mount := Node3D.new()
var horse := Node3D.new()
var car := Node3D.new()
var caption := Label.new()
var triggered := false
var seen := false
var left: Button
var right: Button
var ride: Button

func block(parent: Node3D, pos: Vector3, size: Vector3, color: String) -> MeshInstance3D:
	var part := MeshInstance3D.new()
	var box := BoxMesh.new()
	box.size = size
	var mat := StandardMaterial3D.new()
	mat.albedo_color = Color(color)
	box.material = mat
	part.mesh = box
	parent.add_child(part)
	part.position = pos
	return part

func button(row: HBoxContainer, label: String) -> Button:
	var b := Button.new()
	b.text = label
	b.custom_minimum_size = Vector2(172,86)
	row.add_child(b)
	return b

func _ready() -> void:
	for root in [forest, city, mount]:
		add_child(root)
	mount.add_child(horse)
	mount.add_child(car)
	for world in [forest,city]:
		block(world,Vector3(0,-0.6,-55),Vector3(180,1,230),"40533d" if world == forest else "717675")
		block(world,Vector3(0,-0.08,-55),Vector3(12,0.16,160),"85664a" if world == forest else "434952")
	for i in range(18):
		var z := -7.0*i
		for side in [-1,1]:
			var x: float = side*(9+i%3)
			block(forest,Vector3(x,2,z),Vector3(1,4,1),"554132")
			block(forest,Vector3(x,5,z),Vector3(4,5+i%3,4),"284637")
			if i%3 == 0:
				block(city,Vector3(side*15,6,z),Vector3(12,12,15),"888482")
				for floor_no in range(4):
					for window_no in range(4):
						block(city,Vector3(side*8.94,2+floor_no*2.6,z-5+3*window_no),Vector3(0.1,1.3,1.5),"d6c897")
		block(city,Vector3(0,0.02,z),Vector3(0.14,0.04,3),"e7ddb2")
	for side in [-1,1]:
		block(forest,Vector3(side*6,5,-107),Vector3(6,10,7),"726d64")
	block(forest,Vector3(0,8,-107),Vector3(10,2,3),"726d64")
	block(horse,Vector3(0,1.2,0),Vector3(0.9,0.9,2.1),"68422e")
	block(horse,Vector3(0,1.92,-0.95),Vector3(0.56,1.0,0.65),"775137")
	for x in [-0.31,0.31]:
		for z in [-0.7,0.7]:
			block(horse,Vector3(x,0.43,z),Vector3(0.22,0.86,0.26),"443026")
	block(horse,Vector3(0,2.05,0.15),Vector3(0.55,0.85,0.45),"474d58")
	block(horse,Vector3(0,2.63,0.15),Vector3(0.37,0.37,0.37),"c6a18b")
	block(car,Vector3(0,0.65,0),Vector3(1.9,0.8,3.6),"39485c")
	block(car,Vector3(0,1.19,0.2),Vector3(1.55,0.55,1.9),"73909e")
	add_child(quest)
	quest.build(forest,city)
	var camera := Camera3D.new()
	mount.add_child(camera)
	camera.position = Vector3(0,4.5,7.4)
	camera.rotation.x = -0.32
	var sun := DirectionalLight3D.new()
	add_child(sun)
	sun.rotation_degrees = Vector3(-55,-25,0)
	var ui := CanvasLayer.new()
	add_child(ui)
	ui.add_child(caption)
	caption.position = Vector2(42,36)
	caption.add_theme_font_size_override("font_size",32)
	caption.add_theme_constant_override("outline_size",6)
	ui.add_child(journal)
	journal.position = Vector2(42,190)
	journal.add_theme_font_size_override("font_size",30)
	journal.add_theme_constant_override("outline_size",6)
	journal.hide()
	var row := HBoxContainer.new()
	ui.add_child(row)
	row.set_anchors_and_offsets_preset(Control.PRESET_BOTTOM_LEFT)
	row.position += Vector2(42,-110)
	left = button(row,"◀")
	ride = button(row,"Ехать ▲")
	right = button(row,"▶")
	action = button(row,"Осмотреть знак")
	action.pressed.connect(interact)
	button(row,"Журнал").pressed.connect(toggle_journal)
	button(row,"Заново").pressed.connect(restart)
	update_view()

func toggle_journal() -> void:
	journal.visible = not journal.visible

func restart() -> void:
	journal.hide()
	mount.position = Vector3.ZERO
	clock = Glimpse.new()
	triggered = false
	seen = false
	quest.reset()
	update_view()

func update_view() -> void:
	var active := clock.remaining > 0
	forest.visible = not active
	horse.visible = not active
	city.visible = active
	car.visible = active
	if active:
		caption.text = "2026 · Москва\n" + str(event.journal_clue)
	else:
		caption.text = "1348 · Крепостная дорога\n" + quest.objective()
	journal.text = "Журнал Кирилла"
	if seen:
		journal.text += "\n• " + str(event.journal_clue)
	if quest.stage >= Investigation.Stage.ASK_GUARD:
		journal.text += "\n• На придорожном камне высечен такой же знак."
	if quest.stage >= Investigation.Stage.PASS_GATE:
		journal.text += "\n• Стражник слышал звон после того, как сняли язык."
	if not seen:
		journal.text += "\nЗаписей пока нет."
	refresh_action()

func refresh_action() -> void:
	var prompt := quest.action_at(mount.position)
	action.visible = not prompt.is_empty()
	if action.visible:
		action.text = prompt + " [E]"

func interact() -> void:
	quest.interact(mount.position)
	update_view()

func _unhandled_key_input(input_event: InputEvent) -> void:
	if input_event is InputEventKey and input_event.physical_keycode == KEY_E and input_event.pressed and not input_event.echo:
		interact()

func _physics_process(delta: float) -> void:
	var steering := Input.get_axis("ui_left","ui_right") + float(right.button_pressed) - float(left.button_pressed)
	var half_width := 2.5 if mount.position.z < -102 else 4.5
	mount.position.x = clampf(mount.position.x + clampf(steering,-1,1)*delta*4,-half_width,half_width)
	if ride.button_pressed or Input.is_action_pressed("ui_up"):
		var through_gate := quest.stage >= Investigation.Stage.PASS_GATE and absf(mount.position.x) <= 2.5
		var road_end := -113.0 if through_gate else -102.0
		mount.position.z = maxf(road_end, mount.position.z - delta*9)
	var active := clock.remaining > 0
	clock.advance(delta)
	if not triggered and mount.position.z <= -20:
		triggered = clock.begin(event.duration_sec,event.cooldown)
		seen = triggered
		if triggered:
			quest.glimpse_started()
	if active and clock.remaining == 0:
		quest.glimpse_ended()
	var previous_stage := quest.stage
	quest.finish_at(mount.position)
	if active != (clock.remaining > 0) or previous_stage != quest.stage:
		update_view()
	else:
		refresh_action()
