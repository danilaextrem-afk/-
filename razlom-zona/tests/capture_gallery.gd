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
    for name in ["village", "last_bus", "bus_return", "forest_base", "post_office", "porch", "rift_checkpoint", "approach", "underground", "g4_final_station", "g4_radio_close", "zalesye_contours", "trackside_workshop", "watchman_encounter", "anomaly_before", "anomaly_after", "forest_road_depth", "base_perimeter", "industrial_admin", "industrial_warehouse"]:
        if name == "village":
            player.position = Vector3(-39, 0.1, 2)
            player.camera.look_at(Vector3(-18, 2.65, 2))
        elif name == "last_bus":
            player.position = Vector3(-38.3, .12, 1.9)
            player.camera.look_at(Vector3(-32.8, 1.60, -1.5))
        elif name == "bus_return":
            player.position = Vector3(-27.1, .1, 1.0)
            player.camera.look_at(Vector3(-32.0, 1.50, -1.95))
        elif name == "industrial_admin":
            player.position = Vector3(-3.0,.10,-8.7)
            player.camera.look_at(Vector3(-3.0,4.1,-20.0))
        elif name == "industrial_warehouse":
            player.position = Vector3(1.0,.10,12.0)
            player.camera.look_at(Vector3(1.0,4.3,29.0))
        elif name == "base_perimeter":
            player.position = Vector3(21.0,.10,-3.0)
            player.camera.look_at(Vector3(26.7,1.6,-7.0))
        elif name == "forest_road_depth":
            player.position = Vector3(18.7,.10,-5.0)
            player.camera.look_at(Vector3(18.0,1.8,-23.0))
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
        elif name == "anomaly_before" or name == "anomaly_after":
            player.position = Vector3(13.0, .10, 11.6)
            player.camera.look_at(Vector3(13.0, 1.55, 19.0))
            var zone: Dictionary = game.anomaly_zones[0]
            var field := zone["visual"] as MeshInstance3D
            var boundary := zone["boundary"] as MeshInstance3D
            if name == "anomaly_before":
                field.set_meta("new_material",field.material_override)
                field.set_meta("new_mesh",field.mesh)
                var old_mesh := SphereMesh.new()
                old_mesh.radius = float(zone["radius"])
                old_mesh.height = old_mesh.radius * 2.0
                old_mesh.radial_segments = 8
                old_mesh.rings = 4
                var old_material := game._material("43c8c8", true) as StandardMaterial3D
                old_material.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
                old_material.albedo_color.a = .13
                old_material.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
                field.mesh = old_mesh
                field.material_override = old_material
                boundary.visible = false
            else:
                field.mesh = field.get_meta("new_mesh") as Mesh
                field.material_override = field.get_meta("new_material") as Material
                boundary.visible = true
        elif name == "watchman_encounter":
            var watchman = game.enemies[0]
            watchman.set_physics_process(false)
            watchman.position = Vector3(12.0,0.08,-13.5)
            watchman.rotation.y = PI
            watchman.windup = watchman.WINDUP_TIME * .25
            for pose_frame in 12:
                watchman.animate(1.0/60.0,0.0)
            player.position = Vector3(12.0,0.1,-9.7)
            player.camera.look_at(Vector3(12.0,1.2,-13.5))
        elif name == "trackside_workshop":
            player.position = Vector3(-12.5, 0.08, -2.5)
            player.camera.look_at(Vector3(-7.8, 2.8, -11.5))
        elif name == "zalesye_contours":
            player.position = Vector3(-29.0, 0.08, 13.0)
            player.camera.look_at(Vector3(-18.0, 2.35, 26.0))
        elif name == "g4_radio_close":
            player.position = Vector3(24.15, -3.60, -34.3)
            player.camera.look_at(Vector3(25.38, -3.05, -35.7))
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
