extends "res://Source/two_epoch_road.gd"

enum Phase { GARAGE, DRIVE, GLIMPSE, ROADSIDE, MESSAGE, END }
const SIGHTINGS := [82.0, 101.0, 120.0]
var phase := Phase.GARAGE
const Slot = preload("res://Source/campaign_slot.gd")
var resume_button: Button
var slot_path := Slot.PATH
var speed := 0.0
var sightings := 0
var evidence := ""
var figure := preload("res://Source/roadside_stranger.gd").new()
var brake: Button
var rear := false
var recording := SubViewportContainer.new()
var flame_nodes: Array[MeshInstance3D] = []
var traffic := preload("res://Source/road_sedan.gd").new()
var trip_time := 0.0

func build_location() -> void:
	var wet := ShaderMaterial.new()
	wet.shader = preload("res://Content/Materials/wet_asphalt.gdshader")
	pavement.material_override = wet
	daylight.light_color = Color("#9aaece")
	daylight.light_energy = .24
	var night := WorldEnvironment.new()
	var sky := Environment.new()
	var dusk := ProceduralSkyMaterial.new()
	dusk.sky_top_color = Color("#101a30")
	dusk.sky_horizon_color = Color("#687784")
	dusk.ground_horizon_color = dusk.sky_horizon_color
	dusk.ground_bottom_color = Color("#252a32")
	dusk.sun_angle_max = 0.0
	sky.sky = Sky.new()
	sky.sky.sky_material = dusk
	sky.background_mode = Environment.BG_SKY
	sky.background_color = Color("#182339")
	sky.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
	sky.ambient_light_color = Color("#b1bad6")
	sky.ambient_light_energy = .32
	sky.fog_enabled = true
	sky.fog_light_color = Color("#344052")
	sky.fog_density = .008
	sky.fog_sky_affect = .4
	night.environment = sky
	add_child(night)
	for i in range(7):
		var z := -16.0 - 18*i
		block(city,Vector3(6.5,2.6,z),Vector3(.14,5.2,.14),"535561")
		var fixture := block(city,Vector3(6.05,5.2,z),Vector3(1.1,.13,.36),"ffe6ad")
		var glow := fixture.mesh.surface_get_material(0) as StandardMaterial3D
		glow.emission_enabled = true
		glow.emission = Color("#ffe1a2")
		var lamp := OmniLight3D.new()
		city.add_child(lamp)
		lamp.position = Vector3(5.9,4.95,z)
		lamp.light_color = Color("#ffd4a2")
		lamp.light_energy = 4.4
		lamp.omni_range = 11
		lamp.omni_attenuation = 1.2
	for side in [-1,1]:
		var headlight := SpotLight3D.new()
		car.add_child(headlight)
		headlight.position = Vector3(side*.60,.68,-2.16)
		headlight.rotation_degrees.x = -8
		headlight.light_color = Color("#fff0c8")
		headlight.light_energy = 5.5
		headlight.spot_range = 20
		headlight.spot_angle = 26
		headlight.spot_attenuation = 1.4
		headlight.spot_angle_attenuation = 1.8
	for side in [-1,1]:
		block(city,Vector3(side*7.45,.015,-55),Vector3(2.8,.18,160),"777977")
		block(city,Vector3(side*6.05,.035,-55),Vector3(.14,.22,160),"939089")
		for i in 40:
			block(city,Vector3(side*7.45,.108,20-4*i),Vector3(2.8,.012,.032),"4e5051")
	city.add_child(figure)
	traffic.paint_color = Color("#8f5948")
	traffic.rotation.y = PI
	traffic.position = Vector3(-3.1,0,-130)
	city.add_child(traffic)
	for i in 3:
		var parked := preload("res://Source/road_sedan.gd").new()
		parked.paint_color = [Color("#a99e87"),Color("#53605a"),Color("#7f514c")][i]
		parked.rotation.y = PI-.025*i
		parked.position = Vector3(-4.9,0,-12-31*i)
		city.add_child(parked)
	block(city,Vector3(7.45,2,-11),Vector3(.16,4,.16),"4b5258")
	var sign := Label3D.new()
	sign.text = "АВТОСЕРВИС\nИЛЬЯ"
	sign.font_size = 44
	sign.pixel_size = 0.009
	sign.position = Vector3(7.45,3.4,-10.89)
	block(city,Vector3(7.45,3.4,-11),Vector3(2.95,1.1,.17),"29373e")
	city.add_child(sign)

func add_controls(row: HBoxContainer) -> void:
	set_rear(false)
	build_recording(row.get_parent())
	brake = button(row,"Тормоз ↓")
	button(row,"Зеркало [M]").pressed.connect(look_back)
	ride.text = "Газ ↑"
	resume_button = button(row,"Продолжить")
	resume_button.pressed.connect(resume_district)

func set_rear(value: bool) -> void:
	rear = value
	camera.position = Vector3(0,2.0,-1.7) if rear else Vector3(.45,3.05,6.75)
	camera.rotation = Vector3(-0.08,PI,0) if rear else Vector3(-.215,0,0)

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
	car.set_braking(false)
	horse.pose(0)
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
		Phase.END:
			if Slot.store_checkpoint(0,evidence,slot_path) != OK:
				caption.text = "Не удалось сохранить поездку."
				return
			resume_district()
			return
	update_view()

func _unhandled_key_input(key: InputEvent) -> void:
	if key is InputEventKey and key.pressed and not key.echo:
		match key.physical_keycode:
			KEY_M: look_back()
			KEY_E: interact()

func update_view() -> void:
	resume_button.visible = phase == Phase.GARAGE and not Slot.resume(slot_path).is_empty()
	forest.visible = phase == Phase.GLIMPSE
	horse.visible = forest.visible
	city.visible = not forest.visible
	car.visible = city.visible
	sync_road_sound()
	figure.visible = phase == Phase.ROADSIDE and sightings > 0 and evidence.is_empty() and -mount.position.z <= SIGHTINGS[sightings-1]+9
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
		Phase.END:
			action.show()
			action.text = "К Илье [E]"
			caption.text = "Илья: «Кир, откуда у тебя доспехи? И что там за пожар?»\nЗапись с неизвестного номера · 00:04"
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
	car.set_braking(braking)
	speed = move_toward(speed,14 if gas and not braking else 0,delta*(18 if braking else 4))
	var steer := Input.get_axis("ui_left","ui_right") + float(right.button_pressed) - float(left.button_pressed)
	mount.position.x = clampf(mount.position.x + clampf(steer,-1,1)*minf(speed,5)*delta,-4.4,4.4)
	mount.position.z -= speed*delta
	car.roll(speed*delta)
	if horse.visible:
		horse.pose(speed*delta)
	trip_time += delta
	traffic.roll(8*delta)
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
			figure.position = Vector3(6.48,.11,-SIGHTINGS[sightings-1])
			update_view()
		if sightings > 0 and distance > SIGHTINGS[sightings-1]+9:
			figure.hide()
		action.visible = figure.visible and speed < .5 and mount.position.distance_to(figure.position) < 9
		action.text = "Осмотреть обочину [E]"
		if distance >= 131:
			if evidence.is_empty():
				evidence = "Кирилл проехал мимо. Происхождение силуэта неизвестно."
			speed = 0
			car.set_braking(false)
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

func resume_district() -> void:
	var saved := Slot.resume(slot_path)
	if saved.is_empty():
		return
	saved["slot_path"] = slot_path
	if saved.guard >= 0:
		get_tree().set_meta("guard_entry",saved)
		get_tree().change_scene_to_file("res://Content/PrinceGuard.tscn")
		return
	if saved.hunt >= 0:
		get_tree().set_meta("hunt_entry",saved)
		get_tree().change_scene_to_file("res://Content/Hunt.tscn")
		return
	if saved.crossing >= Slot.GROVE_START:
		get_tree().set_meta("grove_entry",saved)
		get_tree().change_scene_to_file("res://Content/Grove.tscn")
		return
	if saved.crossing >= 0:
		get_tree().set_meta("crossing_entry",saved)
		get_tree().change_scene_to_file("res://Content/Crossing.tscn")
		return
	get_tree().set_meta("district_entry",saved)
	get_tree().change_scene_to_file("res://Content/District.tscn")
