extends SceneTree
const Slot = preload("res://Source/campaign_slot.gd")
func _initialize() -> void:
	call_deferred("run")
func run() -> void:
	var ok := true
	for outcome in range(30,34):
		var path := "user://hunt-%d.cfg" % Time.get_ticks_usec()
		Slot.store_checkpoint(11,"след",path,true,"rescued",outcome)
		change_scene_to_file("res://Content/NightRoad.tscn")
		await scene_changed
		current_scene.slot_path = path
		current_scene.resume_district()
		await scene_changed
		current_scene.action.pressed.emit()
		await scene_changed
		var hunt = current_scene
		hunt.interact()
		ok = ok and hunt.phase == 0
		for point in [hunt.PAW,hunt.KIT,hunt.DEN]:
			hunt.hero.position = point
			hunt.interact()
		for i in 100: hunt._physics_process(1.0/60)
		ok = ok and hunt.phase == hunt.Phase.LURE and hunt.hero.position == hunt.KIT
		hunt.hero.position = hunt.DEN
		hunt.interact()
		Input.action_press("ui_down")
		Input.action_press("ui_right")
		for i in 37: hunt._physics_process(1.0/60)
		Input.action_release("ui_right")
		for i in 240: hunt._physics_process(1.0/60)
		Input.action_release("ui_down")
		var saved := Slot.resume(path)
		ok = ok and saved.hunt == 4 and saved.crossing == outcome and saved.evidence=="след"
		change_scene_to_file("res://Content/NightRoad.tscn")
		await scene_changed
		current_scene.slot_path = path
		current_scene.resume_district()
		await scene_changed
		ok = ok and current_scene.phase == 4 and current_scene.ward.visible
		DirAccess.remove_absolute(ProjectSettings.globalize_path(path))
	current_scene.queue_free()
	await create_timer(.35).timeout
	print("Contract recovery, lure, capture and four outcomes: ",ok)
	quit(int(not ok))
