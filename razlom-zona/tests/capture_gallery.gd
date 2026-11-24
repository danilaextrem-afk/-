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
    for name in ["village", "last_bus", "bus_return", "forest_base", "post_office", "porch", "rift_checkpoint", "approach", "underground", "g4_final_station"]:
        if name == "village":
            player.position = Vector3(-39, 0.1, 2)
            player.camera.look_at(Vector3(-18, 2.65, 2))
        elif name == "last_bus":
            player.position = Vector3(-38.3, .12, 1.9)
            player.camera.look_at(Vector3(-32.8, 1.60, -1.5))
        elif name == "bus_return":
            player.position = Vector3(-27.1, .1, 1.0)
            player.camera.look_at(Vector3(-32.0, 1.50, -1.95))
        elif name == "forest_base":
            player.position = Vector3(18.6,.10,-3.1)
            player.camera.look_at(Vector3(25.6,1.84,-10.6))
        elif name == "post_office":
            player.position = Vector3(-38.4, 0.1, 4.6)
            player.camera.look_at(Vector3(-33.8, 2.0, 8.3))
        elif name == "porch":
            player.position = Vector3(-28, 0.1, 4)
            player.camera.look_at(Vector3(-32.6, 2.15, 9.5))
        elif name == "rift_checkpoint":
            # Genuine engine capture of the newly added checkpoint 07.
            player.position = Vector3(-1.5, 0.08, 2.0)
            player.camera.look_at(Vector3(8.0, 2.35, 2.0))
        elif name == "approach":
            player.position = Vector3(24.7, 0.0, -16.7)
            player.camera.look_at(Vector3(24, 0.9, -24))
        elif name == "g4_final_station":
            player.position = Vector3(24.0, -3.05, -32.7)
            player.camera.look_at(Vector3(24.0, -3.05, -39.7))
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
