extends SceneTree
## Walk from a new game's bus to the postal counter using the normal player controller.
var game
var quarter
var film := "--film" in OS.get_cmdline_user_args()

func _initialize() -> void:
    run.call_deferred()

func require(ok: bool, detail: String) -> bool:
    if not ok:
        push_error(detail)
        quit(1)
    return ok

func face(target: Vector3, rate: float) -> void:
    var player = game.player
    var offset: Vector3 = target - player.camera.global_position
    player.rotation.y = lerp_angle(player.rotation.y,atan2(-offset.x,-offset.z),rate)
    player.camera.rotation.x = lerpf(player.camera.rotation.x,atan2(offset.y,Vector2(offset.x,offset.z).length()),rate)

func watch(target: Vector3, frames: int) -> void:
    for i in frames:
        face(target,.069)
        await physics_frame

func walk(target: Vector3, look_at_point: Vector3) -> bool:
    var player = game.player
    for i in 360:
        var delta: Vector3 = target-player.global_position
        delta.y = 0
        if delta.length() < .105:
            player.virtual_move = Vector2.ZERO
            return true
        face(look_at_point,.065)
        var local: Vector3 = player.basis.inverse()*delta.normalized()
        player.virtual_move = Vector2(local.x,local.z)*(.71 if film else 1.)
        await physics_frame
    return require(false,"Blocked walking to %s from %s (postal coordinates %s)" % [target,player.global_position,quarter.post.to_local(player.global_position)])

func run() -> void:
    if not require(not FileAccess.file_exists("user://razlom_zona_save_v1.json"),"Use a fresh data directory"):
        return
    game = load("res://scenes/main.tscn").instantiate()
    root.add_child(game)
    quarter = game.street.quarter
    var post: Node3D = quarter.post
    var finishes: Array[ShaderMaterial] = []
    for landmark in ["PostOffice", "WidowsCottage", "WaterTower", "BusShelter", "TracksideCottage"]:
        var building := quarter.get_node_or_null(landmark) as Node3D
        if not require(building != null, "Missing architectural landmark " + landmark):
            return
        var meshes := building.find_children("*", "MeshInstance3D", true, false)
        if not require(not meshes.is_empty(), "Missing visible landmark " + landmark):
            return
        var finish := (meshes[0] as MeshInstance3D).material_override as ShaderMaterial
        if not require(finish != null and finish.shader != null and finish.get_shader_parameter("district") != null, "Missing architecture finish " + landmark):
            return
        finishes.append(finish)
    if not require(finishes[1] != finishes[4] and finishes[1].get_shader_parameter("timber_tint") != finishes[4].get_shader_parameter("timber_tint"), "Repeated cottages must have separate finishes"):
        return
    var door_meshes: Array[Node] = quarter.door.find_children("*", "MeshInstance3D", true, false)
    if not require(not door_meshes.is_empty() and (door_meshes[0] as MeshInstance3D).material_override == null, "Door atlas unexpectedly replaced"):
        return
    var door_view := post.to_global(Vector3(2.19,1.68,2.45))
    var note := post.to_global(Vector3(-1.03,1.02,-.44))
    if film:
        await watch(game.street.to_global(Vector3(0,2.7,-25)),85)
    for stop in [Vector3(1.8,0,-5.6),Vector3(4.9,0,-5.55)]:
        if not await walk(game.street.to_global(stop),post.to_global(Vector3(-.24,2.46,2.5))):
            return
    var outside := post.to_global(Vector3(2.29,0,3.66))
    if not await walk(outside,door_view):
        return
    # Drive into the shut leaf. It must physically block the capsule.
    var player = game.player
    var forward: Vector3 = -post.global_basis.z
    var direction: Vector3 = player.basis.inverse()*forward
    player.virtual_move = Vector2(direction.x,direction.z)
    for i in 35:
        await physics_frame
    player.virtual_move = Vector2.ZERO
    if not require(post.to_local(player.global_position).z>2.76,"Closed door allowed passage"):
        return
    if not await walk(outside,door_view):
        return
    if not require(game._nearest_interactable().get("kind")=="door","Missing door action from public entrance"):
        return
    game._interact()
    await watch(door_view,48)
    if not require(quarter.open and quarter.door.rotation.y>1.45,"Door did not swing open"):
        return
    if not await walk(post.to_global(Vector3(2.31,0,.71)),note+Vector3.UP*.6):
        return
    if not await walk(post.to_global(Vector3(-.91,0,.38)),note):
        return
    if not require(game._nearest_interactable().get("kind")=="letter","Letter inaccessible at sorting counter"):
        return
    game._interact()
    if not require(game.letter_read,"Postal document was not read"):
        return
    if film:
        await watch(note,105)
    else:
        game.free()
        game = load("res://scenes/main.tscn").instantiate()
        root.add_child(game)
        quarter = game.street.quarter
        post = quarter.post
        if not require(game.letter_read and quarter.open and quarter.door.rotation.y>1.45,"Interior save did not restore"):
            return
    if not await walk(post.to_global(Vector3(2.31,0,.71)),door_view):
        return
    if not await walk(outside,post.to_global(Vector3(2.29,1.75,5.28))):
        return
    if not require(game.player.is_on_floor(),"Lost floor leaving the post office"):
        return
    if film:
        await watch(game.street.to_global(Vector3(-4,5.0,-32)),70)
    print("PASS new game, blocked closed leaf, open, enter, read, exit", "" if film else ", restore letter and door")
    quit()
