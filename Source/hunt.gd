extends "res://Source/walk_scene.gd"

const Slot = preload("res://Source/campaign_slot.gd")
enum Phase { TRACK, SILVER, LURE, CHASE, DONE }
const PAW := Vector3(-2,0,-6)
const KIT := Vector3(3,0,-10)
const DEN := Vector3(-2,0,-18)
const WARD := Vector3(0,0,-12)
var phase := Phase.TRACK
var hero := Node3D.new()
var beast := Node3D.new()
var ward := Node3D.new()
var hud := Label.new()
var action: Button
var keys := {}
var entry: Dictionary
var note := ""

func _ready() -> void:
	entry = get_tree().get_meta("hunt_entry",{})
	if get_tree().has_meta("hunt_entry"): get_tree().remove_meta("hunt_entry")
	phase = entry.get("hunt",Phase.TRACK)
	if phase == Phase.CHASE: phase = Phase.LURE
	add_child(hero)
	person(hero,Vector3.ZERO,"57616a")
	var camera := Camera3D.new()
	hero.add_child(camera)
	camera.position = Vector3(0,6.3,7.0)
	camera.rotation.x = -.64
	box(self,Vector3(0,-.2,-10),Vector3(22,.4,30),"59644c")
	var crown := CylinderMesh.new()
	crown.top_radius = 0
	crown.bottom_radius = 1.45
	crown.height = 3.2
	crown.radial_segments = 9
	crown.rings = 1
	var needles := StandardMaterial3D.new()
	needles.albedo_color = Color("#364e40")
	crown.material = needles
	for side in [-1,1]:
		for i in 7:
			var base := Vector3(side*(7.1+cos(i*1.8)),0,2-i*3.7)
			box(self,base+Vector3.UP,Vector3(.36,2,.36),"595040")
			for tier in 3:
				var branch := MeshInstance3D.new()
				branch.mesh = crown
				add_child(branch)
				branch.position = base+Vector3.UP*(2.1+tier*1.15)
				branch.scale = Vector3.ONE*(1.0-.17*tier)
				branch.rotation.y = i*.62+tier*.3
	person(self,Vector3(2,0,-1),"776340")
	for i in 5:
		box(self,PAW+Vector3(.2 if i%2 else -.2,.03,-.4*i),Vector3(.24,.05,.32),"40392c")
	for side in [-1,1]:
		box(self,DEN+Vector3(side*1.7,1,0),Vector3(1.4,2,2),"5d5449").rotation.z = side*.18
	box(self,DEN+Vector3(0,2.2,0),Vector3(4,.8,2),"514b40")
	box(self,KIT+Vector3.UP*.38,Vector3(1.1,.76,.7),"60432f")
	for i in 3:
		box(self,KIT+Vector3(i*.2-.2,.83,0),Vector3(.13,.12,.46),"c2ccd2")
	add_child(ward)
	ward.position = WARD
	for i in 12:
		var angle := i*TAU/12
		box(ward,Vector3(sin(angle),.06,cos(angle)),Vector3(.55,.09,.09),"c6d0cf").rotation.y = angle
	add_child(beast)
	box(beast,Vector3(0,1.06,0),Vector3(.67,.74,1.48),"48484a")
	box(beast,Vector3(0,1.41,-.78),Vector3(.55,.57,.7),"545155")
	for side in [-1,1]:
		box(beast,Vector3(side*.19,1.77,-.67),Vector3(.13,.29,.18),"403d40")
		for axle in [-.45,.46]:
			box(beast,Vector3(side*.25,.48,axle),Vector3(.2,.96,.24),"423d3f")
	var sun := DirectionalLight3D.new()
	add_child(sun)
	sun.rotation_degrees = Vector3(-52,-25,0)
	var ui := CanvasLayer.new()
	add_child(ui)
	ui.add_child(hud)
	hud.position = Vector2(32,24)
	hud.add_theme_font_size_override("font_size",28)
	hud.add_theme_constant_override("outline_size",5)
	var row := HBoxContainer.new()
	ui.add_child(row)
	row.set_anchors_and_offsets_preset(Control.PRESET_BOTTOM_LEFT)
	row.position += Vector2(28,-104)
	for pair in [["ui_left","◀"],["ui_up","▲"],["ui_down","▼"],["ui_right","▶"]]:
		keys[pair[0]] = control(row,pair[1])
	action = control(row,"")
	action.pressed.connect(interact)
	refresh()

func advance(next: int) -> bool:
	if not entry.is_empty() and Slot.store_checkpoint(11,entry.evidence,entry.slot_path,true,entry.tenant,entry.crossing,next) != OK:
		note = "Не удалось записать контрольную точку."
		refresh()
		return false
	phase = next
	note = ""
	refresh()
	return true

func near(point: Vector3) -> bool:
	return hero.position.distance_to(point) < 2

func refresh() -> void:
	var hints := ["Мира: «Слева от тропы след. Посмотри, куда зверь волочил лапу».","След уходит под камни. Справа у сундука возьми серебряные звенья.","Круг готов. Подойди к каменному лазу и вымани волколака.","Веди волколака через серебряный круг к броду.","Круг сомкнулся. Волколак остался у камней, Мира ждёт у начала тропы, пора вернуться в селение."]
	hud.text = "Охотничий договор · Русь, 1348\n" + str(hints[phase])
	if not note.is_empty(): hud.text += "\n"+note
	ward.visible = phase >= Phase.LURE
	beast.visible = phase >= Phase.CHASE
	if phase == Phase.DONE: beast.position = WARD
	action.visible = (phase == Phase.DONE and near(Vector3(2,0,-1)) and not entry.is_empty()) or (phase == Phase.TRACK and near(PAW)) or (phase == Phase.SILVER and near(KIT)) or (phase == Phase.LURE and near(DEN))
	action.text = ["Осмотреть след [E]","Подготовить серебро [E]","Выманить зверя [E]","","Вернуться с Мирой [E]"][phase]

func interact() -> void:
	if phase == Phase.DONE and near(Vector3(2,0,-1)) and not entry.is_empty():
		if Slot.store_checkpoint(11,entry.evidence,entry.slot_path,true,entry.tenant,entry.crossing,phase,0) != OK:
			note = "Не удалось сохранить возвращение."
			refresh()
			return
		var saved := Slot.resume(entry.slot_path)
		saved["slot_path"] = entry.slot_path
		get_tree().set_meta("guard_entry",saved)
		get_tree().change_scene_to_file("res://Content/PrinceGuard.tscn")
	elif phase == Phase.TRACK and near(PAW): advance(Phase.SILVER)
	elif phase == Phase.SILVER and near(KIT): advance(Phase.LURE)
	elif phase == Phase.LURE and near(DEN):
		beast.position = DEN + Vector3.FORWARD*2
		advance(Phase.CHASE)

func _unhandled_key_input(event: InputEvent) -> void:
	if event is InputEventKey and event.pressed and not event.echo and event.physical_keycode == KEY_E: interact()

func _physics_process(delta: float) -> void:
	var axis := Input.get_vector("ui_left","ui_right","ui_up","ui_down")
	axis += Vector2(float(keys.ui_right.button_pressed)-float(keys.ui_left.button_pressed),float(keys.ui_down.button_pressed)-float(keys.ui_up.button_pressed))
	axis = axis.limit_length()
	hero.position += Vector3(axis.x,0,axis.y)*4.6*delta
	hero.position = hero.position.clamp(Vector3(-6,0,-20),Vector3(6,0,2))
	if phase == Phase.CHASE:
		var next := beast.position.move_toward(hero.position,3.4*delta)
		if beast.position.distance_squared_to(next) > .000001: beast.look_at(next)
		beast.position = next
		if beast.position.distance_to(WARD) < 1.15:
			advance(Phase.DONE)
		elif beast.position.distance_to(hero.position) < .85 and advance(Phase.LURE):
			hero.position = KIT
			note = "Мира отвлекла зверя. В следующий раз отступай к серебряному кругу."
	refresh()
