extends "res://Source/two_epoch_road.gd"

enum Phase { GARAGE, DRIVE, GLIMPSE, ROADSIDE, MESSAGE, END }
const SIGHTINGS := [82.0, 101.0, 120.0]
var phase := Phase.GARAGE
var speed := 0.0
var sightings := 0
var evidence := ""
var figure := Node3D.new()
var brake: Button
var rear := false
var recording := SubViewportContainer.new()
var flame_nodes: Array[MeshInstance3D] = []
var traffic := Node3D.new()
var trip_time := 0.0

func build_location() -> void:
	var night := WorldEnvironment.new()
	var sky := Environment.new()
	sky.background_mode = Environment.BG_COLOR
	sky.background_color = Color("#182339")
	sky.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
	sky.ambient_light_color = Color("#b1bad6")
	sky.ambient_light_energy = 0.5
	night.environment = sky
	add_child(night)
	for i in range(7):
		var z := -16.0 - 18*i
		block(city,Vector3(6.5,2.6,z),Vector3(.14,5.2,.14),"535561")
		var fixture := block(city,Vector3(6.05,5.2,z),Vector3(1.1,.13,.36),"ffe6ad")
		var glow := fixture.mesh.surface_get_material(0) as StandardMaterial3D
		glow.emission_enabled = true
		glow.emission = Color("#ffe1a2")
	city.add_child(figure)
	block(figure,Vector3(0,1.0,0),Vector3(.48,1.6,.28),"202027")
	block(figure,Vector3(0,1.98,0),Vector3(.32,.38,.32),"36303b")
	city.add_child(traffic)
	block(traffic,Vector3(0,.55,0),Vector3(1.7,.8,3.4),"775546")
	block(traffic,Vector3(0,1.1,0),Vector3(1.45,.55,1.65),"899ca5")
	for x in [-.6,.6]:
		block(traffic,Vector3(x,.56,1.72),Vector3(.33,.16,.05),"ffe7ae")
		block(car,Vector3(x,.72,1.83),Vector3(.42,.14,.06),"dd432a")
	for side in [-1,1]:
		for axle in [-1,1]:
			block(car,Vector3(side*.94,.37,axle*1.12),Vector3(.24,.58,.59),"23252a")
	block(city,Vector3(5.4,2,-5),Vector3(.16,4,.16),"4b5258")
	var sign := Label3D.new()
	sign.text = "АВТОСЕРВИС\nИЛЬЯ"
	sign.font_size = 72
	sign.pixel_size = 0.009
	sign.position = Vector3(4.5,3.6,-5)
	city.add_child(sign)

func add_controls(row: HBoxContainer) -> void:
	build_recording(row.get_parent())
	brake = button(row,"Тормоз ↓")
	button(row,"Зеркало [M]").pressed.connect(look_back)
	ride.text = "Газ ↑"

func set_rear(value: bool) -> void:
	rear = value
	camera.position = Vector3(0,2.0,-1.7) if rear else Vector3(0,4.5,7.4)
	camera.rotation = Vector3(-0.08,PI,0) if rear else Vector3(-0.32,0,0)

func look_back() -> void:
	if phase in [Phase.DRIVE,Phase.ROADSIDE]:
		set_rear(not rear)
		if rear and phase == Phase.ROADSIDE and sightings > 0 and evidence.is_empty():
			var passed: float = -mount.position.z - SIGHTINGS[sightings-1]
			if passed >= 0 and passed < 9:
				evidence = "В зеркале у фигуры нет отражения."
				figure.hide()
		update_view()

func restart() -> void:
	phase = Phase.GARAGE
	speed = 0
	mount.position = Vector3.ZERO
	sightings = 0
	trip_time = 0
	evidence = ""
	clock = Glimpse.new()
	journal.hide()
	set_rear(false)
	update_view()

func interact() -> void:
	match phase:
		Phase.GARAGE: phase = Phase.DRIVE
		Phase.ROADSIDE:
			if figure.visible and speed < .5 and mount.position.distance_to(figure.position) < 9 and evidence.is_empty():
				evidence = "На сырой обочине нет следов. Человек исчез."
				figure.hide()
		Phase.MESSAGE: phase = Phase.END
	update_view()

func _unhandled_key_input(key: InputEvent) -> void:
	if key is InputEventKey and key.pressed and not key.echo:
		match key.physical_keycode:
			KEY_M: look_back()
			KEY_E: interact()

func update_view() -> void:
	forest.visible = phase == Phase.GLIMPSE
	horse.visible = forest.visible
	city.visible = not forest.visible
	car.visible = city.visible
	figure.visible = phase == Phase.ROADSIDE and sightings > 0 and evidence.is_empty()
	recording.visible = phase == Phase.END
	action.visible = false
	match phase:
		Phase.GARAGE:
			caption.text = "Москва · 2026 · Автосервис Ильи\nИлья: «Кирилл, как доедешь — маякни. На поворотах сегодня туман»."
			action.text = "За руль [E]"
			action.show()
		Phase.DRIVE: caption.text = "Возвращение домой\nГаз — ↑, тормоз — ↓. Держись правой стороны."
		Phase.GLIMPSE: caption.text = "Пять секунд чужой жизни\nВ руках поводья. Где-то за стволами звенит железо."
		Phase.ROADSIDE:
			caption.text = "Человек у обочины\n" + ("Можно остановиться рядом, проверить зеркало или уехать." if evidence.is_empty() else evidence)
		Phase.MESSAGE:
			caption.text = "Телефон · Входящее сообщение от Ильи"
			action.text = "Открыть [E]"
			action.show()
		Phase.END: caption.text = "Илья: «Кир, откуда у тебя доспехи? И что там за пожар?»\nЗапись с неизвестного номера · 00:04"
	journal.text = "Телефон Кирилла\nИлья · Автосервис"
	if phase >= Phase.ROADSIDE:
		journal.text += "\nНа четыре секунды вместо трассы появился лес."
	if sightings > 0:
		journal.text += "\nВстреч с одним силуэтом: %d." % sightings
	if not evidence.is_empty():
		journal.text += "\n" + evidence

func _physics_process(delta: float) -> void:
	if phase == Phase.END:
		trip_time += delta
		for i in flame_nodes.size():
			flame_nodes[i].scale.y = 1 + 0.28*sin(trip_time*7.0 + i)
		return
	if phase == Phase.GARAGE or phase >= Phase.MESSAGE:
		return
	var gas := ride.button_pressed or Input.is_action_pressed("ui_up")
	var braking := brake.button_pressed or Input.is_action_pressed("ui_down")
	speed = move_toward(speed,14 if gas and not braking else 0,delta*(18 if braking else 4))
	var steer := Input.get_axis("ui_left","ui_right") + float(right.button_pressed) - float(left.button_pressed)
	mount.position.x = clampf(mount.position.x + clampf(steer,-1,1)*minf(speed,5)*delta,-4.4,4.4)
	mount.position.z -= speed*delta
	trip_time += delta
	traffic.position = Vector3(-3.1,0,-130+fmod(trip_time*8,155))
	var distance := -mount.position.z
	if phase == Phase.DRIVE and distance >= 17:
		clock.begin(4,15)
		set_rear(false)
		phase = Phase.GLIMPSE
		update_view()
	elif phase == Phase.GLIMPSE:
		clock.advance(delta)
		if clock.remaining <= 0:
			phase = Phase.ROADSIDE
			update_view()
	if phase == Phase.ROADSIDE:
		if evidence.is_empty() and sightings < SIGHTINGS.size() and distance >= SIGHTINGS[sightings]-10:
			sightings += 1
			figure.position = Vector3(4.9,0,-SIGHTINGS[sightings-1])
			update_view()
		if sightings > 0 and distance > SIGHTINGS[sightings-1]+9:
			figure.hide()
		action.visible = figure.visible and speed < .5 and mount.position.distance_to(figure.position) < 9
		action.text = "Осмотреть обочину [E]"
		if distance >= 131:
			if evidence.is_empty():
				evidence = "Кирилл проехал мимо. Происхождение силуэта неизвестно."
			speed = 0
			set_rear(false)
			phase = Phase.MESSAGE
			update_view()

func build_recording(ui: Node) -> void:
	ui.add_child(recording)
	recording.position = Vector2(42,350)
	recording.size = Vector2(520,300)
	var screen := SubViewport.new()
	screen.size = Vector2i(520,300)
	screen.own_world_3d = true
	recording.add_child(screen)
	var village := Node3D.new()
	screen.add_child(village)
	block(village,Vector3(0,-0.3,0),Vector3(50,0.5,50),"45404a")
	for x in [-4,0,4]:
		block(village,Vector3(x,1,-6),Vector3(3,2,3),"684533")
		for n in range(3):
			var flame := block(village,Vector3(x-0.7+n*0.7,2.3,-5.4),Vector3(.45,1.6+n*.3,.4),"e29132")
			var mat := flame.mesh.surface_get_material(0) as StandardMaterial3D
			mat.emission_enabled = true
			mat.emission = Color("#ff7731")
			flame_nodes.append(flame)
	block(village,Vector3(0,1.15,0),Vector3(.86,1.28,.5),"858c91")
	block(village,Vector3(0,2.09,0),Vector3(.52,.56,.48),"ab9586")
	for side in [-1,1]:
		block(village,Vector3(side*.53,1.4,0),Vector3(.3,.5,.58),"adb1ad")
		block(village,Vector3(side*.24,.35,0),Vector3(.34,.7,.44),"4e4947")
	var lamp := DirectionalLight3D.new()
	village.add_child(lamp)
	lamp.rotation_degrees = Vector3(-35,-25,0)
	var lens := Camera3D.new()
	village.add_child(lens)
	lens.position = Vector3(1.0,2.3,4)
	lens.look_at(Vector3(0,1.5,0))
