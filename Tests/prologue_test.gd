extends SceneTree

func _initialize() -> void:
	call_deferred("run")

func run() -> void:
	var failures := []
	for choice in ["stop","mirror","drive"]:
		var road = load(ProjectSettings.get_setting("application/run/main_scene")).instantiate()
		root.add_child(road)
		var actor_id = road.mount.get_instance_id()
		road.action.pressed.emit()
		Input.action_press("ui_up")
		var forest_frames := 0
		var used := false
		for frame in 4000:
			road._physics_process(1.0/60)
			if road.horse.visible:
				forest_frames += 1
			if road.sightings == 3 and not used:
				var distance: float = -road.mount.position.z
				if choice == "mirror" and distance >= 120:
					road.look_back()
					used = not road.evidence.is_empty()
				if choice == "stop" and distance >= 115:
					Input.action_press("ui_down")
					if road.action.visible:
						road.action.pressed.emit()
						Input.action_release("ui_down")
						used = true
			if road.phase == road.Phase.MESSAGE:
				break
		Input.action_release("ui_up")
		Input.action_release("ui_down")
		var expected := {"stop":"следов","mirror":"отражения","drive":"мимо"}
		if not (road.sightings == 3 and abs(forest_frames-240)<=1 and road.mount.get_instance_id()==actor_id and expected[choice] in road.evidence and road.phase == road.Phase.MESSAGE):
			failures.append([choice,forest_frames,road.evidence,road.phase])
		road.action.pressed.emit()
		if not (road.recording.visible and road.evidence in road.journal.text):
			failures.append("phone or journal")
		road.restart()
		if road.recording.visible or not road.evidence.is_empty() or road.phase != road.Phase.GARAGE:
			failures.append("restart")
		road.free()
	print("Prologue routes: ", "PASS" if failures.is_empty() else str(failures))
	quit(int(not failures.is_empty()))
