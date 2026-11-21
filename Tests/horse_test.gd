extends SceneTree

func _initialize() -> void:
	call_deferred("run")

func run() -> void:
	var horse := preload("res://Source/road_horse.gd").new()
	root.add_child(horse)
	horse.pose(.8)
	var ok := horse.hips.size() == 4 and horse.knees.size() == 4
	ok = ok and horse.hips[0].rotation.x > .49 and horse.hips[1].rotation.x < -.49
	ok = ok and is_equal_approx(horse.hips[0].rotation.x,horse.hips[3].rotation.x)
	ok = ok and horse.knees[0].rotation.x < 0 and horse.position == Vector3.ZERO
	horse.pose(0)
	for i in horse.hips.size():
		ok = ok and horse.hips[i].rotation.is_zero_approx() and horse.knees[i].rotation.is_zero_approx()
	print("Horse diagonal gait, stationary pose and fixed root: ",ok)
	quit(int(not ok))
