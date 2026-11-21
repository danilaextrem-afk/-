extends SceneTree
const Slot = preload("res://Source/campaign_slot.gd")

func _initialize() -> void:
	call_deferred("run")

func run() -> void:
	var ok := true
	for start in [14,15]:
		for condemn in [false,true]:
			var path := "user://snow-%d.cfg" % Time.get_ticks_usec()
			set_meta("crossing_entry",{"crossing":start,"slot_path":path,"tenant":"called","evidence":"улика"})
			change_scene_to_file("res://Content/Crossing.tscn")
			await scene_changed
			var scene = current_scene
			for point in [scene.village.FORGE,scene.village.SNOW,scene.village.LOOM]:
				scene.hero.position = point
				scene.interact()
			scene.condemn()
			ok = ok and scene.step == start+6
			scene.hero.position = scene.village.ELDER
			scene.refresh_action()
			ok = ok and scene.verdict_button.visible and scene.action.visible
			if condemn: scene.verdict_button.pressed.emit()
			else: scene.action.pressed.emit()
			var saved := Slot.resume(path)
			ok = ok and saved.crossing == start+(10 if condemn else 8) and saved.evidence == "улика"
			change_scene_to_file("res://Content/NightRoad.tscn")
			await scene_changed
			current_scene.slot_path = path
			current_scene.resume_district()
			await scene_changed
			ok = ok and current_scene.step == saved.crossing and current_scene.village.witness.visible == (start==15)
			ok = ok and current_scene.village.weaver.visible != condemn
			DirAccess.remove_absolute(ProjectSettings.globalize_path(path))
	current_scene.queue_free()
	await create_timer(.35).timeout
	print("Snow investigation and four saved outcomes: ",ok)
	quit(int(not ok))
