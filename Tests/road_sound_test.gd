extends SceneTree

func _initialize() -> void:
	call_deferred("run")

func run() -> void:
	var ok := true
	for scene in ["Road", "NightRoad"]:
		var road = load("res://Content/%s.tscn" % scene).instantiate()
		root.add_child(road)
		var forest_first: bool = scene == "Road"
		var original: AudioStreamWAV = road.ambience.stream
		ok = ok and original == (road.FOREST_SOUND if forest_first else road.CITY_SOUND)
		ok = ok and original.loop_end == original.data.size() and original.get_length() == 4.0
		ok = ok and original.data.count(0) < original.data.size()/2
		if forest_first:
			road.mount.position.z = -21
		else:
			road.interact()
			road.mount.position.z = -18
		road._physics_process(0)
		ok = ok and road.ambience.stream != original and road.ambience.playing
		road._physics_process(4.01)
		ok = ok and road.ambience.stream == original and road.ambience.playing
		road.free()
	print("Both road audio layers and both returns: ",ok)
	await create_timer(0.35).timeout
	quit(int(not ok))
