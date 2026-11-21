extends SceneTree
func _initialize() -> void:
	call_deferred("run")

func run() -> void:
	var car := preload("res://Source/road_sedan.gd").new()
	root.add_child(car)
	var body: ArrayMesh = car.get_child(0).mesh
	var arrays := body.surface_get_arrays(0)
	var normals: PackedVector3Array = arrays[Mesh.ARRAY_NORMAL]
	var outward := normals[12].y > .8
	var wheels_ok := car.wheels.size() == 4
	car.roll(.34)
	for wheel in car.wheels:
		wheels_ok = wheels_ok and is_equal_approx(wheel.rotation.x,-1)
	print("Sedan geometry and axles: ",[outward,wheels_ok])
	quit(int(not(outward and wheels_ok)))
