extends SceneTree
const Slot = preload("res://Source/campaign_slot.gd")

func _initialize() -> void:
	call_deferred("run")

func run() -> void:
	var ok := true
	for verdict in range(22,26):
		var path := "user://grove-%d.cfg" % Time.get_ticks_usec()
		set_meta("crossing_entry",{"crossing":verdict,"slot_path":path,"tenant":"called","evidence":"след"})
		change_scene_to_file("res://Content/Crossing.tscn")
		await scene_changed
		current_scene.interact()
		await scene_changed
		var grove = current_scene
		grove.interact()
		ok = ok and not grove.trained
		Input.action_press("ui_up")
		var frames := 0
		var actor = grove.mount.get_instance_id()
		for i in 840:
			grove._physics_process(1.0/60)
			if grove.action.visible and not grove.trained: grove.action.pressed.emit()
			if grove.car.visible: frames += 1
		Input.action_release("ui_up")
		ok = ok and absi(frames-300)<=1 and grove.horse.visible and grove.mount.get_instance_id()==actor
		grove.interact()
		var saved := Slot.resume(path)
		ok = ok and grove.complete and saved.crossing==verdict+8 and saved.evidence=="след"
		change_scene_to_file("res://Content/NightRoad.tscn")
		await scene_changed
		current_scene.slot_path = path
		current_scene.resume_district()
		await scene_changed
		ok = ok and current_scene.complete and current_scene.horse.visible
		DirAccess.remove_absolute(ProjectSettings.globalize_path(path))
	current_scene.queue_free()
	await create_timer(.35).timeout
	print("Grove pursuit, five-second glimpse and four verdicts: ",ok)
	quit(int(not ok))
