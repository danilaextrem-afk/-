extends SceneTree

func _initialize() -> void:
	call_deferred("run")

func run() -> void:
	var yard = preload("res://Content/District.tscn").instantiate()
	root.add_child(yard)
	yard.avatar.position = Vector3(3.7,0,-18)
	Input.action_press("ui_up")
	yard._physics_process(2)
	var stop: float = yard.avatar.position.z
	var ok := is_equal_approx(stop,-21.4+.28+.36)
	Input.action_press("ui_left")
	yard._physics_process(.2)
	ok = ok and yard.avatar.position.x < 3.7 and is_equal_approx(yard.avatar.position.z,stop)
	yard._physics_process(.4)
	ok = ok and yard.avatar.position.z < stop
	Input.action_release("ui_left")
	yard.stage = yard.Stage.LANDING
	yard.set_view()
	yard.avatar.position = Vector3(3,0,-14)
	yard._physics_process(2)
	ok = ok and is_equal_approx(yard.avatar.position.z,-16.8+.16+.36)
	Input.action_release("ui_up")
	yard.free()
	print("Bench contact, slide, bypass and landing door: ",ok)
	quit(int(not ok))
