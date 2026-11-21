extends SceneTree

func _initialize() -> void:
	call_deferred("run")

func run() -> void:
	var ok := true
	for scene in ["NightRoad","Road"]:
		var road = load("res://Content/%s.tscn" % scene).instantiate()
		root.add_child(road)
		var houses := 0
		var batches := 0
		var parts := 0
		var shared_mesh: Mesh
		for node in road.city.get_children():
			if node.get_script() != preload("res://Source/panel_house.gd"): continue
			houses += 1
			var entrance: Vector3 = node.to_global(Vector3(0,1,6))
			ok = ok and absf(absf(entrance.x)-9.0) < .001
			for batch_node in node.get_children():
				var group: MultiMesh = batch_node.multimesh
				if not shared_mesh: shared_mesh = group.mesh
				ok = ok and group.mesh == shared_mesh and group.use_colors and group.instance_count > 0
				parts += group.instance_count
				batches += 1
		ok = ok and houses == 12 and batches == 24 and parts > 1200
		print(scene,": houses=",houses,", render nodes=",batches,", instances=",parts)
		road.free()
	quit(int(not ok))
