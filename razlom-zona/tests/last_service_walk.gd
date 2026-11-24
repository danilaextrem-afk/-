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
    var landform := game.street.get_node_or_null("ZalesyeLandform") as Node3D
    if not check(landform != null, "Missing real landform"):
        return
    var terrain_detail := landform.get_node_or_null("Detailed")
    var distant := landform.get_node_or_null("Distant")
    if not check(terrain_detail != null and distant != null, "Missing terrain geometry or LOD"):
        return
    var near_meshes := terrain_detail.find_children("*", "MeshInstance3D", true, false)
    var far_meshes := distant.find_children("*", "MeshInstance3D", true, false)
    if not check(near_meshes.size() == 1 and far_meshes.size() == 1, "Invalid terrain mesh hierarchy"):
        return
    if not check(near_meshes[0].get_aabb().size.y > 1.0 and near_meshes[0].visibility_range_end == 65.0 and far_meshes[0].visibility_range_begin == 65.0, "Missing contour depth or LOD"):
        return
    if not check(is_zero_approx(landform.height_at(0.0,-20.0)), "Terrain intrudes on road"):
        return
    await physics_frame
    await physics_frame
    var sample := Vector3(27.0, landform.height_at(27.0,-10.0), -10.0)
    var world_point: Vector3 = landform.to_global(sample)
    var ray := PhysicsRayQueryParameters3D.create(world_point + Vector3.UP * 4.0, world_point - Vector3.UP * 3.0)
    var hit: Dictionary = game.get_world_3d().direct_space_state.intersect_ray(ray)
    if not check(not hit.is_empty() and absf(hit.position.y - world_point.y) < 0.06, "Terrain collision does not follow exported surface"):
        return
    var brush_near: Array[Node] = game.street.quarter.find_children("BrambleNear_*", "MultiMeshInstance3D", true, false)
    var brush_far: Array[Node] = game.street.quarter.find_children("BrambleFar_*", "MultiMeshInstance3D", true, false)
    if not check(not brush_near.is_empty() and brush_near.size() == brush_far.size(), "Missing paired woodland brush batches"):
        return
    var brush_count := 0
    for patch in brush_near:
        var brush_batch := patch as MultiMeshInstance3D
        brush_count += brush_batch.multimesh.instance_count
        if not check(is_equal_approx(brush_batch.visibility_range_end, 18.0), "Invalid near brush range"):
            return
    if not check(brush_count >= 30 and brush_count <= 73, "Unexpected brush density"):
        return
    if not check(game.anomaly_zones.size() == 2, "Missing anomaly fields"):
        return
    for zone in game.anomaly_zones:
        var field := zone["visual"] as MeshInstance3D
        var boundary := zone["boundary"] as MeshInstance3D
        var field_material := field.material_override as ShaderMaterial
        var boundary_plane := boundary.mesh as PlaneMesh
        if not check(field_material != null and boundary.material_override is ShaderMaterial and field.cast_shadow == GeometryInstance3D.SHADOW_CASTING_SETTING_OFF, "Missing field filament or boundary material"):
            return
        if not check(is_equal_approx(float(field_material.get_shader_parameter("field_radius")), float(zone["radius"])) and boundary_plane.size.is_equal_approx(Vector2.ONE * float(zone["radius"]) * 2.0), "Field boundary not matched to hazard radius"):
            return
    # The forest lane is rendered just above its unchanged collision slab.
    var lane := game.get_node_or_null("ForestAccessRoadSurface") as MeshInstance3D
    if not check(lane != null, "Missing forest road visual"):
        return
    var slab := game.get_node_or_null("SideRoad") as Node3D
    if not check(slab != null, "Missing forest road collision slab"):
        return
    var plane := lane.mesh as PlaneMesh
    var material := lane.material_override as ShaderMaterial
    if not check(plane != null and material != null, "Invalid forest road mesh or material"):
        return
    if not check(plane.size.is_equal_approx(Vector2(6.0, 48.0)) and absf(lane.position.y - slab.position.y - 0.031) < 0.001, "Forest road visual misaligned"):
        return
    if not check(lane.get_child_count() == 0 and material.shader != null, "Unexpected forest road collision or missing shader"):
        return
    if not check(game.bulkhead_status.text == "КАНАЛ НЕДОСТУПЕН" and float(game.bulkhead_material.get_shader_parameter("linked")) == 0.0, "Initial bulkhead state incorrect"):
        return
    var cabinets: Array[Node] = game.radio.find_children("G4RadioCabinet", "Node3D", true, false)
    if not check(cabinets.size() == 1, "Missing final radio cabinet"):
        return
    var cabinet := cabinets[0] as Node3D
    var detail := cabinet.get_node_or_null("CabinetDetail")
    var lod := cabinet.get_node_or_null("CabinetLOD")
    if not check(detail != null and lod != null and cabinet.get_node_or_null("CabinetCollision") != null, "Missing cabinet model, LOD or collision"):
        return
    var near_parts := detail.find_children("*", "MeshInstance3D", true, false)
    var far_parts := lod.find_children("*", "MeshInstance3D", true, false)
    if not check(near_parts.size() == 1 and far_parts.size() == 1, "Unexpected cabinet mesh layout"):
        return
    if not check(is_equal_approx(near_parts[0].visibility_range_end, 9.0) and is_equal_approx(far_parts[0].visibility_range_begin, 9.0), "Cabinet LOD misconfigured"):
        return
    var foley = game.player.boot_foley
    if not check(foley.bank.size() == 3, "Missing footstep surface bank"):
        return
    for surface in ["mud", "forest", "concrete"]:
        var samples: Array = foley.bank[surface]
        if not check(samples.size() == 3 and samples[0].data.size() > 8000 and samples[0].data != samples[1].data, "Missing distinct original Foley samples"):
            return
    if not check(foley.surface_at(Vector3(-30,0,2)) == "mud" and foley.surface_at(Vector3(22,0,-12)) == "forest" and foley.surface_at(Vector3(24,-3,-34)) == "concrete", "Incorrect surface classification"):
        return
    var initial_steps: int = foley.step_count
    for frame in 20:
        foley.advance(0.0, true, false, Vector3.ZERO)
        foley.advance(0.2, false, false, Vector3.ZERO)
    if not check(foley.step_count == initial_steps, "Footsteps while idle or airborne"):
        return
    await listen(Vector3(-19,2.65,2), 48)
    await listen(game.radio.SITES[0]+Vector3(0,.6,0), 52)
    if not await walk_to(Vector2(-33.4,0.02)):
        return
    if not check(game.player.boot_foley.surface_counts["mud"] > 0, "Actual movement did not produce street footsteps"):
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
    if not check(game.player.boot_foley.surface_counts["concrete"] > 0, "Actual bunker traversal did not produce concrete footsteps"):
        return
    await listen(game.radio.SITES[2], 50)
    if not tune(3):
        return
    if not check(game.bulkhead_status.text == "КАНАЛ ВОССТАНОВЛЕН" and float(game.bulkhead_material.get_shader_parameter("linked")) == 1.0, "Final radio contact did not change bulkhead"):
        return
    if not filming:
        game.free()
        open_game()
    if not check(game.bulkhead_status.text == "КАНАЛ ВОССТАНОВЛЕН" and float(game.bulkhead_material.get_shader_parameter("linked")) == 1.0, "Completed bulkhead state lost on reload"):
        return
    if not check(game.radio.indicators[2].emission_energy_multiplier > 0.5, "Completed radio should retain its green signal after reload"):
        return
    await listen(game.radio.SITES[2], 105)
    if check(game.radio.stage == 3,"G-4 completion lost on reload"):
        print("PASS continuous live route" if filming else "PASS bus, checkpoint and G-4; restored two saves")
        quit()
