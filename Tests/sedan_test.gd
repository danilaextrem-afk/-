extends SceneTree
func _initialize() -> void:
	call_deferred("run")

func run() -> void:
	var car := preload("res://Source/road_sedan.gd").new()
	root.add_child(car)
	var body: ArrayMesh = car.get_child(0).mesh
	var arrays := body.surface_get_arrays(0)
	var normals: PackedVector3Array = arrays[Mesh.ARRAY_NORMAL]
	var outward := normals[30].y > .8
	var running_energy := car.tail_lens.emission_energy_multiplier
	car.set_braking(true)
	var lamps_ok := car.high_stop.visible and car.tail_lens.emission_energy_multiplier > running_energy
	car.set_braking(false)
	lamps_ok = lamps_ok and not car.high_stop.visible and is_equal_approx(car.tail_lens.emission_energy_multiplier,running_energy)
	var wheels_ok := car.wheels.size() == 4
	car.roll(.34)
	for wheel in car.wheels:
		wheels_ok = wheels_ok and is_equal_approx(wheel.rotation.x,-1)
	print("Sedan geometry and axles: ",[outward,wheels_ok,lamps_ok])
	quit(int(not(outward and wheels_ok and lamps_ok)))
