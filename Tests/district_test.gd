extends SceneTree
const Slot = preload("res://Source/campaign_slot.gd")
var failures := []
var slot := "user://district-test-%d.cfg" % Time.get_ticks_usec()

func _initialize() -> void:
	call_deferred("run")

func check(ok: bool, description: String) -> void:
	if not ok:
		failures.append(description)

func run() -> void:
	change_scene_to_file("res://Content/NightRoad.tscn")
	await scene_changed
	var road = current_scene
	road.slot_path = slot
	road.action.pressed.emit()
	Input.action_press("ui_up")
	for frame in 3000:
		road._physics_process(1.0/60)
		if road.phase == road.Phase.MESSAGE:
			break
	Input.action_release("ui_up")
	var clue: String = road.evidence
	road.action.pressed.emit()
	road.action.pressed.emit()
	await scene_changed
	var district = current_scene
	district.slot_path = slot
	check(not clue.is_empty() and district.evidence == clue,"road evidence transfer")
	district.interact()
	check(district.stage == 0,"cannot use distant targets")
	for i in 12:
		var target: Vector3 = district.TARGETS[i]
		for frame in 900:
			var delta_pos: Vector3 = target-district.avatar.position
			if delta_pos.length() < .2:
				break
			var xkey := "ui_right" if delta_pos.x > 0 else "ui_left"
			var zkey := "ui_down" if delta_pos.z > 0 else "ui_up"
			if abs(delta_pos.x) > .12: Input.action_press(xkey)
			if abs(delta_pos.z) > .12: Input.action_press(zkey)
			district._physics_process(1.0/60)
			Input.action_release(xkey)
			Input.action_release(zkey)
		check(district.action.visible,"action reachable %d" % i)
		if i == 6:
			district.interact()
			check(district.stage == 6,"darkness blocks inspection")
			district.toggle_light()
		district.action.pressed.emit()
		var stored := Slot.resume(slot)
		check(stored.get("stage") == mini(11,i+1),"checkpoint %d" % i)
		if i == 4:
			change_scene_to_file("res://Content/NightRoad.tscn")
			await scene_changed
			road = current_scene
			road.slot_path = slot
			road.update_view()
			check(road.resume_button.visible,"continue button")
			road.resume_button.pressed.emit()
			await scene_changed
			district = current_scene
			district.slot_path = slot
			check(district.stage == 5 and district.evidence == clue and district.inside.visible,"resume interior")
	check(district.finished and Slot.resume(slot).finished and district.tenant == "rescued" and "угольным" in district.logbook.text,"finish archive and keep witness note")
	DirAccess.remove_absolute(ProjectSettings.globalize_path(slot))
	print("District campaign: ", "PASS" if failures.is_empty() else str(failures))
	current_scene.queue_free()
	await create_timer(0.35).timeout
	quit(int(not failures.is_empty()))
