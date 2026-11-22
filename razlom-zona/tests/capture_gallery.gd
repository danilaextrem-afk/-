extends SceneTree

func _initialize() -> void:
    capture.call_deferred()

func capture() -> void:
    var game = load("res://scenes/main.tscn").instantiate()
    root.add_child(game)
    var player = game.player
    player.set_physics_process(false)
    var folder := ProjectSettings.globalize_path("res://captures")
    DirAccess.make_dir_recursive_absolute(folder)
    for name in ["village", "porch", "approach", "underground"]:
        if name == "village":
            player.position = Vector3(-39, 0.1, 2)
            player.camera.look_at(Vector3(-18, 2.65, 2))
        elif name == "porch":
            player.position = Vector3(-28, 0.1, 4)
            player.camera.look_at(Vector3(-34, 2.15, 9.4))
        elif name == "approach":
            player.position = Vector3(24.7, 0.0, -16.7)
            player.camera.look_at(Vector3(24, 0.9, -24))
        else:
            player.position = Vector3(23.5, -3.5, -34)
            player.camera.look_at(Vector3(24.4, -0.3, -26))
        for frame in 3:
            await RenderingServer.frame_post_draw
        var frame_image := root.get_texture().get_image()
        if frame_image == null or frame_image.is_empty() or frame_image.save_png(folder.path_join(name + ".png")) != OK:
            push_error("Frame capture failed at " + name)
            quit(1)
            return
    quit()
