extends SceneTree

var game
var filming := "--film" in OS.get_cmdline_user_args()

func _initialize() -> void:
    run.call_deferred()

func check(ok: bool, message: String) -> bool:
    if not ok:
        push_error(message)
        quit(1)
    return ok

func walk_to(point: Vector2) -> bool:
    var player = game.player
    for frame in 1300:
        var offset := point - Vector2(player.position.x,player.position.z)
        if offset.length() < .17:
            player.virtual_move = Vector2.ZERO
            return true
        if filming:
            face_point(Vector3(point.x,player.position.y+1.75,point.y))
        var local: Vector3 = player.basis.inverse()*Vector3(offset.x,0,offset.y).normalized()
        player.virtual_move = Vector2(local.x,local.z)
        await physics_frame
    return check(false,"Blocked at %s on approach to %s" % [player.position,point])

func open_game() -> void:
    game = load("res://scenes/main.tscn").instantiate()
    root.add_child(game)
    if not filming:
        for enemy in game.enemies:
            enemy.queue_free()

func face_point(point: Vector3) -> void:
    var player = game.player
    var dir: Vector3 = point-player.camera.global_position
    player.rotation.y = lerp_angle(player.rotation.y, atan2(-dir.x,-dir.z), .06)
    player.camera.rotation.x = lerpf(player.camera.rotation.x, atan2(dir.y,Vector2(dir.x,dir.z).length()),.065)

func listen(point: Vector3, frames: int) -> void:
    if filming:
        for frame in frames:
            face_point(point)
            await physics_frame

func tune(expected: int) -> bool:
    var nearest: Dictionary = game._nearest_interactable()
    if not check(nearest.get("kind") == "radio","Receiver is unreachable at %s" % game.player.position):
        return false
    game._interact()
    return check(game.radio.stage == expected,"Incorrect radio stage")

func run() -> void:
    if not check(not FileAccess.file_exists("user://razlom_zona_save_v1.json"),"Use an empty data directory"):
        return
    seed(2717)
    open_game()
    await listen(Vector3(-19,2.65,2), 48)
    await listen(game.radio.SITES[0]+Vector3(0,.6,0), 52)
    if not await walk_to(Vector2(-32.6,1.1)):
        return
    await listen(game.radio.SITES[0], 36)
    if not tune(1):
        return
    if not filming:
        game.free()
        open_game()
    if not check(game.radio.stage == 1,"Radio journal lost on reload"):
        return
    await listen(Vector3(-22,2.5,2.5), 80)
    for point in [Vector2(-28,2),Vector2(5.1,2.9),Vector2(6.5,2.9)]:
        if not await walk_to(point):
            return
    await listen(game.radio.SITES[1], 36)
    if not tune(2):
        return
    for point in [Vector2(9,2.6),Vector2(20.8,2.6),Vector2(20.8,-18.0),Vector2(24,-18),Vector2(24,-35.0),Vector2(24.5,-35.5)]:
        if not await walk_to(point):
            return
    await listen(game.radio.SITES[2], 50)
    if not tune(3):
        return
    if not filming:
        game.free()
        open_game()
    await listen(game.radio.SITES[2], 105)
    if check(game.radio.stage == 3,"G-4 completion lost on reload"):
        print("PASS continuous live route" if filming else "PASS bus, checkpoint and G-4; restored two saves")
        quit()
