extends SceneTree
## Run with an isolated XDG_DATA_HOME and --fixed-fps 60.
func _initialize() -> void:
    run.call_deferred()

func require(ok: bool, detail: String) -> bool:
    if not ok:
        push_error(detail)
        quit(1)
    return ok

func run() -> void:
    if FileAccess.file_exists("user://razlom_zona_save_v1.json"):
        push_error("Use an empty test data directory.")
        quit(1)
        return
    var game = load("res://scenes/main.tscn").instantiate()
    root.add_child(game)
    var player = game.player
    var gallery := game.get_node_or_null("BunkerGallery") as Node3D
    if not require(gallery != null and gallery.get_child_count() == 5, "Five gallery bays required"):
        return
    for index in range(5):
        var bay := gallery.get_node_or_null("GalleryBay_%02d" % index) as Node3D
        if not require(bay != null, "Missing bay"):
            return
        var meshes := bay.find_children("*", "MeshInstance3D", true, false)
        if not require(not meshes.is_empty(), "Missing original gallery geometry"):
            return
        var visual := meshes[0] as MeshInstance3D
        var weather := visual.material_override as ShaderMaterial
        if not require(weather != null, "Missing weathered atlas"):
            return
        if not require(is_equal_approx(float(weather.get_shader_parameter("section_id")), float(index)), "Incorrect section material"):
            return
        if not require((bay.get_node_or_null("ServiceLight") != null) == (index % 2 == 0), "Unexpected light count"):
            return
        if index % 2 == 1:
            if not require(is_zero_approx(float(weather.get_shader_parameter("lamp_power"))), "Failed lamp must not emit"):
                return
    for enemy in game.enemies:
        enemy.queue_free()
    player.rotation.y = 0.0
    player.position = Vector3(24, 0.2, -18)
    game._touch_down("forward")
    for frame in 285:
        await physics_frame
    game._touch_up("forward")
    print("End of gallery ", player.position)
    if not require(player.position.z < -38.5 and player.position.y < -4.5 and player.is_on_floor(), "Descent failed"):
        return
    game._touch_down("use")
    if not require(game.collected_ids.has("bunker_supplies"), "End cache unreachable"):
        return
    var old_pos: Vector3 = player.position
    game._save_game()
    game.free()
    game = load("res://scenes/main.tscn").instantiate()
    root.add_child(game)
    player = game.player
    for enemy in game.enemies:
        enemy.queue_free()
    if not require(player.position.distance_to(old_pos) < 0.01 and game.inventory.medkit == 1, "Save restore failed"):
        return
    await process_frame
    await process_frame
    if not require(not game.has_node("bunker_supplies"), "Cache reappeared"):
        return
    game._touch_down("back")
    for frame in 330:
        await physics_frame
    game._touch_up("back")
    print("Back on surface ", player.position)
    if not require(player.position.z > -19.5 and player.position.y >= -0.05, "Return blocked"):
        return
    print("PASS approach, descent, collision, pickup, reload, ascent")
    quit()
