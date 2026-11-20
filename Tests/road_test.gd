extends SceneTree

func _initialize() -> void:
	call_deferred("run")

func run() -> void:
	var road = load("res://Content/Road.tscn").instantiate()
	root.add_child(road)
	var id = road.mount.get_instance_id()
	var results := []
	results.append(road.horse.visible and not road.city.visible)
	road.mount.position = Vector3(2,0,-21)
	road._physics_process(0)
	results.append(road.car.visible and road.city.visible and not road.horse.visible)
	results.append(not road.clock.begin(4,15))
	road._physics_process(3.99)
	results.append(road.city.visible)
	road._physics_process(0.02)
	results.append(road.horse.visible and not road.car.visible and road.seen)
	results.append(road.mount.get_instance_id() == id and road.mount.position == Vector3(2,0,-21))
	results.append(not road.clock.begin(4,15))
	road._physics_process(15)
	results.append(not road.clock.begin(6,0) and road.clock.begin(3,0))
	road.restart()
	results.append(road.mount.position == Vector3.ZERO and not road.triggered and road.horse.visible)
	print("Road checks: ",results)
	road.queue_free()
	await create_timer(0.35).timeout
	quit(1 if false in results else 0)
