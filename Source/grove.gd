extends "res://Source/two_epoch_road.gd"

const Slot = preload("res://Source/campaign_slot.gd")
const TARGET := Vector3(2,0,-17)
var entry: Dictionary
var dummy := Node3D.new()
var scout := preload("res://Source/road_horse.gd").new()
var blade := Node3D.new()
var swing_left := 0.0
var trained := false
var seen := false
var complete := false
var notice := ""

func build_location() -> void:
	entry = get_tree().get_meta("grove_entry",{})
	if get_tree().has_meta("grove_entry"):
		get_tree().remove_meta("grove_entry")
	complete = entry.get("crossing",0) >= Slot.GROVE_END
	trained = complete
	seen = complete
	forest.add_child(dummy)
	dummy.position = TARGET
	block(dummy,Vector3(0,1,0),Vector3(.15,2,.15),"70553b")
	block(dummy,Vector3(0,1.65,0),Vector3(.85,.69,.42),"bca36c")
	for y in [1.47,1.83]:
		block(dummy,Vector3(0,y,0),Vector3(.88,.055,.45),"6a503c")
	forest.add_child(scout)
	scout.position = Vector3(-1.8,0,-24)
	if complete: mount.position.z = -100
	horse.add_child(blade)
	blade.position = Vector3(.38,1.95,-.18)
	block(blade,Vector3(0,.35,0),Vector3(.08,.7,.035),"969da1")
	block(blade,Vector3.ZERO,Vector3(.23,.045,.055),"57493d")

func add_controls(_row: HBoxContainer) -> void:
	ride.text = "Скакать ↑"

func restart() -> void:
	if complete: return
	trained = false
	seen = false
	notice = ""
	clock = Glimpse.new()
	mount.position = Vector3.ZERO
	scout.position.z = -24
	horse.pose(0)
	scout.pose(0)
	update_view()

func interact() -> void:
	if complete and not entry.is_empty():
		var outcome: int = entry.crossing
		if outcome < Slot.GROVE_END: outcome += 4
		if Slot.store_checkpoint(11,entry.evidence,entry.slot_path,true,entry.tenant,outcome,0) != OK:
			notice = "Не удалось сохранить договор."
			update_view()
			return
		var saved := Slot.resume(entry.slot_path)
		saved["slot_path"] = entry.slot_path
		get_tree().set_meta("hunt_entry",saved)
		get_tree().change_scene_to_file("res://Content/Hunt.tscn")
		return
	if not trained and mount.position.distance_to(TARGET) <= 3.0:
		trained = true
		swing_left = .35
	elif trained and seen and scout.position.z <= -100 and clock.remaining == 0 and mount.position.distance_to(scout.position) <= 3 and not complete:
		if not entry.is_empty():
			var result := Slot.store_checkpoint(11,entry.evidence,entry.slot_path,true,entry.tenant,entry.crossing+4)
			if result != OK:
				notice = "Не удалось сохранить. Повтори действие у разведчицы."
				update_view()
				return
		complete = true
	update_view()

func _unhandled_key_input(event: InputEvent) -> void:
	if event is InputEventKey and event.pressed and not event.echo and event.physical_keycode == KEY_E:
		interact()

func update_view() -> void:
	var vision := clock.remaining > 0
	city.visible = vision
	car.visible = vision
	forest.visible = not vision
	horse.visible = not vision
	sync_road_sound()
	dummy.rotation.z = -.55 if trained else 0.0
	if complete: scout.position.z = -100
	caption.text = "Конь и роща · Русь, 1348\n"
	if complete: caption.text += "Разведчица у брода. След ведёт в чащу. Урок окончен."
	elif vision: caption.text = "Москва · 2026\nВместо поводьев — руль. Держи курс."
	elif trained: caption.text += "Догони разведчицу у брода слева."
	else: caption.text += "Мира: «Подойди верхом к соломенной мишени справа и ударь клинком»."
	if not notice.is_empty(): caption.text += "\n" + notice
	journal.text = caption.text
	action.text = "Ударить с седла [E]" if not trained else "Окликнуть [E]"
	action.visible = not complete and not vision and (not trained or scout.position.z <= -100) and mount.position.distance_to(scout.position if trained else TARGET) <= 3

	if complete and not entry.is_empty():
		action.text = "Принять договор Миры [E]"
		action.show()

func _physics_process(delta: float) -> void:
	if complete: return
	swing_left = maxf(0,swing_left-delta)
	blade.rotation.z = -0.25 - 1.7*sin(PI*swing_left/.35)
	var previous := mount.position.z
	var steer := Input.get_axis("ui_left","ui_right") + float(right.button_pressed) - float(left.button_pressed)
	mount.position.x = clampf(mount.position.x + clampf(steer,-1,1)*5*delta,-4.4,4.4)
	if ride.button_pressed or Input.is_action_pressed("ui_up"):
		mount.position.z = maxf(-101 if trained else TARGET.z,mount.position.z-9*delta)
	var distance := previous - mount.position.z
	horse.pose(distance)
	if car.visible: car.roll(distance)
	if trained:
		var old_z := scout.position.z
		scout.position.z = maxf(-100,old_z-7*delta)
		scout.pose(old_z-scout.position.z)
	clock.advance(delta)
	if trained and not seen and mount.position.z <= -32:
		seen = clock.begin(5,15)
	update_view()
