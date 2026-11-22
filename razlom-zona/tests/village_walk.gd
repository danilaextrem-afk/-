extends SceneTree

func _initialize() -> void:
    walk.call_deferred()

func walk() -> void:
    if FileAccess.file_exists("user://razlom_zona_save_v1.json"):
        push_error("Use a fresh test data directory")
        quit(1)
        return
    var game = load("res://scenes/main.tscn").instantiate()
    root.add_child(game)
    var player = game.player
    game._touch_down("forward")
    for frame in 370:
        await physics_frame
    game._touch_up("forward")
    if player.position.x < -13 or absf(player.position.z - 2) > .6 or not player.is_on_floor():
        push_error("Street obstructed: %s" % player.position)
        quit(1)
        return
    print("PASS new-game walk through Zalesye: ", player.position)
    quit()
