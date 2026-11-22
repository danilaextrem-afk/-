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
    for enemy in game.enemies:
        enemy.queue_free()
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
