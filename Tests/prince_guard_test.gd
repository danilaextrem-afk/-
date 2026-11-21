extends SceneTree
const Slot = preload("res://Source/campaign_slot.gd")
func _initialize() -> void:
	call_deferred("run")
func walk(scene, destination: Vector3) -> void:
	for frame in 600:
		var offset: Vector3 = destination-scene.hero.position
		if offset.length() < .3: return
		var xkey := "ui_right" if offset.x > 0 else "ui_left"
		var zkey := "ui_down" if offset.z > 0 else "ui_up"
		if absf(offset.x) > .12: Input.action_press(xkey)
		if absf(offset.z) > .12: Input.action_press(zkey)
		scene._physics_process(1.0/60)
		Input.action_release(xkey)
		Input.action_release(zkey)

func run() -> void:
	var ok := true
	for escape in [false,true]:
		var path := "user://guard-%d.cfg" % Time.get_ticks_usec()
		Slot.store_checkpoint(11,"улика",path,true,"called",32,4)
		change_scene_to_file("res://Content/NightRoad.tscn")
		await scene_changed
		current_scene.slot_path = path
		current_scene.resume_district()
		await scene_changed
		current_scene.hero.position = Vector3(2,0,-1)
		current_scene.interact()
		await scene_changed
		var guard = current_scene
		walk(guard,guard.CAPTAIN)
		guard.interact()
		guard._physics_process(12)
		ok = ok and guard.phase == 1 and "прорвался" in guard.note
		if escape:
			walk(guard,guard.POSTERN)
			guard.interact()
		else:
			walk(guard,guard.GATE)
			guard.interact()
			ok = ok and guard.phase == 1
			walk(guard,guard.BEAM)
			guard.interact()
			ok = ok and guard.alarm.playing
			walk(guard,guard.GATE)
			guard.interact()
		var saved := Slot.resume(path)
		ok = ok and saved.guard == (4 if escape else 3) and saved.crossing == 32 and saved.evidence=="улика"
		change_scene_to_file("res://Content/NightRoad.tscn")
		await scene_changed
		current_scene.slot_path = path
		current_scene.resume_district()
		await scene_changed
		ok = ok and current_scene.phase == saved.guard and current_scene.bar.visible != escape
		DirAccess.remove_absolute(ProjectSettings.globalize_path(path))
	current_scene.queue_free()
	await create_timer(.35).timeout
	print("Prince Guard, retry and both saved outcomes: ",ok)
	quit(int(not ok))
