extends SceneTree

# Run with a graphical driver and pass an absolute PNG destination after --.
func _initialize() -> void:
	call_deferred("capture")

func capture() -> void:
	var args := OS.get_cmdline_user_args()
	if args.size() != 1 or DisplayServer.get_name() == "headless":
		push_error("Provide a graphical driver and one output PNG path")
		quit(1)
		return
	var road = preload("res://Content/NightRoad.tscn").instantiate()
	root.add_child(road)
	road.set_physics_process(false)
	road.action.pressed.emit()
	Input.action_press("ui_up")
	Input.action_press("ui_right")
	for i in 120:
		if i == 65: Input.action_release("ui_right")
		road._physics_process(1.0/60)
	Input.action_release("ui_up")
	await RenderingServer.frame_post_draw
	await RenderingServer.frame_post_draw
	var frame := root.get_texture().get_image()
	var error := frame.save_png(args[0])
	road.queue_free()
	await create_timer(.4).timeout
	quit(int(error != OK))
