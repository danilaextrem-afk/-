extends Node3D
## Prototype gameplay loop: explore -> scavenge -> survive -> shelter -> save.
## Procedural placeholder environment; NOT final concept-quality art.

const PlayerScript = preload("res://scripts/player.gd")
const EnemyScript = preload("res://scripts/enemy.gd")
const MobileControlsScript = preload("res://scripts/mobile_controls.gd")
const SAVE_PATH = "user://razlom_zona_save_v1.json"
const WORLD_HALF_SIZE = 46.0

var player
var street := preload("res://scripts/crimson_street.gd").new()
var letter_read := false
var bulkhead_material: ShaderMaterial
var bulkhead_status: Label3D
var radio := preload("res://scripts/radio_route.gd").new()
var inventory: Dictionary = {"medkit": 0, "battery": 0, "scrap": 0}
var collected_ids: Dictionary = {}
var pickups: Array[Node3D] = []
var enemies: Array[CharacterBody3D] = []
var anomaly_zones: Array[Dictionary] = []
var shelter_interaction := Vector3(-11.0, 0.7, 5.65)
var anomaly_damage_clock: float = 0.0
var message_clock: float = 0.0
var is_dead: bool = false
var legacy_test_buttons: Dictionary = {}

var hud: CanvasLayer
var stats_label: Label
var tip_label: Label
var status_label: Label
var touch_controls: Control

func _ready() -> void:
    _setup_environment()
    _create_world()
    add_child(radio)
    player = PlayerScript.new()
    player.position = Vector3(-39.0, 0.25, 2.0)
    player.rotation.y = -PI / 2.0
    add_child(player)
    _spawn_enemy(Vector3(12.0, 0.3, -13.5), 0)
    _create_hud()
    _load_save()
    _show_message("ЗАЛЕСЬЕ   /   Последний рейс", 4.5)

func _process(delta: float) -> void:
    if not is_instance_valid(player):
        return
    radio.update_sound(delta, player.global_position)
    _check_anomalies(delta)
    _check_death()
    if Input.is_action_just_pressed("interact"):
        _interact()
    if Input.is_action_just_pressed("toggle_flashlight"):
        player.toggle_flashlight()
    if Input.is_action_just_pressed("quick_save"):
        _save_game()
    _refresh_hud()
    message_clock -= delta
    if message_clock <= 0.0 and status_label != null:
        status_label.text = ""

func _setup_environment() -> void:
    var env := WorldEnvironment.new()
    env.environment = Environment.new()
    var sky_material := ShaderMaterial.new()
    sky_material.shader = preload("res://assets/anomaly/nocturne_sky.gdshader")
    sky_material.set_shader_parameter("panorama", preload("res://assets/zalesye/evening.jpg"))
    var sky := Sky.new()
    sky.sky_material = sky_material
    env.environment.sky = sky
    env.environment.background_mode = Environment.BG_SKY
    env.environment.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
    env.environment.ambient_light_color = Color("b5b8b0")
    env.environment.ambient_light_energy = 0.50
    env.environment.reflected_light_source = Environment.REFLECTION_SOURCE_SKY
    env.environment.fog_enabled = true
    env.environment.fog_light_color = Color("8e9793")
    env.environment.fog_density = 0.0085
    env.environment.fog_sky_affect = 0.0
    add_child(env)
    var moon := DirectionalLight3D.new()
    moon.rotation_degrees = Vector3(-26.0, 59.0, 0.0)
    moon.light_color = Color("f2d4b0")
    moon.light_energy = 0.87
    moon.shadow_enabled = true
    moon.directional_shadow_max_distance = 36.0
    add_child(moon)

func _material(hex: String, glowing: bool = false, transparent: bool = false) -> StandardMaterial3D:
    var mat := StandardMaterial3D.new()
    mat.albedo_color = Color(hex)
    mat.roughness = 1.0
    if glowing:
        mat.emission_enabled = true
        mat.emission = Color(hex)
        mat.emission_energy_multiplier = 1.3
    if transparent:
        mat.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
        mat.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
        mat.albedo_color.a = 0.22
    return mat

func _box(name: String, center: Vector3, size: Vector3, material: Material, solid: bool = true) -> Node3D:
    var root := Node3D.new()
    root.name = name
    root.position = center
    add_child(root)
    var visual := MeshInstance3D.new()
    var mesh := BoxMesh.new()
    mesh.size = size
    visual.mesh = mesh
    visual.material_override = material
    root.add_child(visual)
    if solid:
        var static_body := StaticBody3D.new()
        var shape := CollisionShape3D.new()
        var box_shape := BoxShape3D.new()
        box_shape.size = size
        shape.shape = box_shape
        static_body.add_child(shape)
        root.add_child(static_body)
    return root

func _create_world() -> void:
    var earth := _material("b6af9a")
    earth.albedo_texture = preload("res://assets/village/leaf_litter.jpg")
    earth.uv1_triplanar = true
    earth.uv1_world_triplanar = true
    earth.uv1_scale = Vector3.ONE*.48
    var pavement := _material("858586")
    pavement.albedo_texture = earth.albedo_texture
    pavement.uv1_triplanar = true
    pavement.uv1_world_triplanar = true
    pavement.uv1_scale = Vector3.ONE*.61
    var cement := _material("596161")
    var roof := _material("283a43")
    var wood := _material("4b4239")
    var scrap := _material("756b5c")
    var dark := _material("172329")
    var cyan := _material("43c8c8", true)

    # Four terrain slabs leave the descent open from z=-20 to z=-40.
    _box("WestTerrain", Vector3(-12, -0.6, 0), Vector3(68, 1.2, 92), earth)
    _box("EastTerrain", Vector3(36, -0.6, 0), Vector3(20, 1.2, 92), earth)
    _box("Approach", Vector3(24, -0.6, 13), Vector3(4, 1.2, 66), earth)
    _box("FarTerrain", Vector3(24, -0.6, -43), Vector3(4, 1.2, 6), earth)
    _box("BuriedRoof", Vector3(24, -0.12, -37), Vector3(4, 0.24, 6), earth)
    _box("MainRoad", Vector3(0, 0.025, 2), Vector3(88, 0.06, 7), pavement)
    _box("SideRoad", Vector3(18, 0.03, -12), Vector3(6, 0.05, 48), pavement)
    # The forest access road uses the same world-space pixel material as Zalesye.
    # This visual layer leaves the existing walkable collision and ramp untouched.
    var forest_lane := MeshInstance3D.new()
    forest_lane.name = "ForestAccessRoadSurface"
    var lane_mesh := PlaneMesh.new()
    lane_mesh.size = Vector2(6.0, 48.0)
    forest_lane.mesh = lane_mesh
    forest_lane.position = Vector3(18.0, 0.061, -12.0)
    var lane_material := ShaderMaterial.new()
    lane_material.shader = preload("res://assets/village/road.gdshader")
    lane_material.set_shader_parameter("ground_map", preload("res://assets/village/road.png"))
    lane_material.set_shader_parameter("soil_detail", preload("res://assets/village/leaf_litter.jpg"))
    lane_material.set_shader_parameter("sky_map", preload("res://assets/zalesye/evening.jpg"))
    lane_material.set_shader_parameter("shoulder_start", 1.85)
    lane_material.set_shader_parameter("shoulder_end", 2.85)
    forest_lane.material_override = lane_material
    forest_lane.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
    add_child(forest_lane)
    add_child(preload("res://scripts/forest_access_landform.gd").new())
    # A fictional abandoned settlement, not a real location.
    street.name = "CrimsonStreet"
    street.position = Vector3(-39, 0, 2)
    street.rotation.y = -PI / 2.0
    add_child(street)

    # Original hero landmark added AFTER Astra's street, before the bunker.
    # Based on the actual playable 3D world coordinates; open walk-through
    # aperture and lightweight physical collision only on both pylons.
    var hero_gate := preload("res://scripts/rift_gate_hero.gd").new()
    hero_gate.name = "ZONA07_RiftCheckpoint_Hero"
    hero_gate.position = Vector3(8.0, 0.0, 2.0)
    hero_gate.rotation.y = -PI / 2.0
    add_child(hero_gate)

    _house(Vector3(-3, 0, -20), Vector3(11, 8, 9), cement, roof, dark)
    var guardhouse := preload("res://assets/base/guardhouse04.gltf").instantiate() as Node3D
    guardhouse.position = Vector3(26.6, 0, -9.5)
    guardhouse.rotation.y = -PI/2
    add_child(guardhouse)
    var shell := StaticBody3D.new()
    var collider := CollisionShape3D.new()
    var shape := BoxShape3D.new()
    shape.size = Vector3(6.15, 3.38, 4.76)
    collider.shape = shape
    collider.position.y = 1.69
    shell.add_child(collider)
    guardhouse.add_child(shell)
    add_child(preload("res://scripts/base_perimeter.gd").new())
    _house(Vector3(28, 0, 22), Vector3(8, 5, 8), cement, roof, dark)
    _box("OldWarehouse", Vector3(1, 4, 29), Vector3(15, 8, 12), _material("414f50"))
    _box("WarehouseRoof", Vector3(1, 8.2, 29), Vector3(16, 0.55, 13), roof)
    var gallery := preload("res://scripts/bunker.gd").new()
    gallery.name = "BunkerGallery"
    gallery.position = Vector3(24, 0, -20)
    add_child(gallery)
    # Real 3D exterior forecourt kit, framing the same tested bunker ramp.
    var forecourt := preload("res://scripts/bunker_forecourt_grade.gd").new()
    forecourt.name = "ZONA07_BunkerForecourt"
    forecourt.position = Vector3(24, 0, -20)
    add_child(forecourt)
    _create_gallery_bulkhead(cement)
    _spawn_pickup("bunker_supplies", "medkit", Vector3(24, -4.10, -38.8))
    _box("WatchPlatform", Vector3(-37, 5, -30), Vector3(5, 0.7, 5), wood)
    for x in [-39.0, -35.0]:
        for z in [-32.0, -28.0]:
            _box("WatchLeg", Vector3(x, 2.45, z), Vector3(0.45, 4.9, 0.45), wood)
    for i in range(12):
        var x := float((i * 19) % 79 - 39)
        var z := float((i * 37) % 77 - 38)
        if absf(x-18.0) < 2.5 and z < 0:
            x -= 4.5
        _box("Rubble_%d" % i, Vector3(x, 0.14, z), Vector3(0.8 + float(i % 3), 0.28, 0.8), scrap)
    for i in range(8):
        var z := -33.0 + 8.0 * float(i)
        if absf(z) < 8.0:
            continue
        _box("FenceEast_%d" % i, Vector3(44, 1.0, z), Vector3(0.25, 2.0, 6.0), wood)
    # A translucent anomalous field is a danger, NOT a boss arena.
    _anomaly(Vector3(13, 0, 19), 4.6, cyan)
    _anomaly(Vector3(-21, 0, -27), 3.8, _material("c75a9e", true))
    _spawn_pickup("medkit_1", "medkit", Vector3(-2, 0.55, 5))
    _spawn_pickup("medkit_2", "medkit", Vector3(31, 0.55, 12))
    _spawn_pickup("battery_1", "battery", Vector3(9, 0.55, 28))
    _spawn_pickup("battery_2", "battery", Vector3(-26, 0.55, 14))
    for i in range(5):
        var point := Vector3(-32 + i * 15, 0.55, -3.0 + float(i % 2) * 10.0)
        _spawn_pickup("scrap_%d" % i, "scrap", point)

func _create_gallery_bulkhead(cement: Material) -> void:
    # Retain the existing wall and its collider; this is a sealed terminus,
    # not a newly promised playable doorway.
    var wall := _box("GalleryEnd", Vector3(24, -3.55, -40.13), Vector3(4.1, 3.2, 0.26), cement)
    var mesh := wall.get_child(0) as MeshInstance3D
    bulkhead_material = ShaderMaterial.new()
    bulkhead_material.shader = preload("res://assets/bunker/bulkhead.gdshader")
    bulkhead_material.set_shader_parameter("atlas", preload("res://assets/bunker/gallery_atlas.png"))
    mesh.material_override = bulkhead_material
    var stencil := Label3D.new()
    stencil.name = "SectorStencil"
    stencil.text = "Г-4   /   ГЕРМОШЛЮЗ"
    stencil.position = Vector3(0, 0.78, 0.144)
    stencil.font_size = 64
    stencil.pixel_size = 0.0032
    stencil.modulate = Color("c2c4ae")
    stencil.outline_size = 0
    wall.add_child(stencil)
    bulkhead_status = Label3D.new()
    bulkhead_status.name = "LinkStatus"
    bulkhead_status.position = Vector3(0, 0.50, 0.144)
    bulkhead_status.font_size = 48
    bulkhead_status.pixel_size = 0.0028
    bulkhead_status.outline_size = 0
    wall.add_child(bulkhead_status)
    _sync_gallery_bulkhead()

func _sync_gallery_bulkhead() -> void:
    if not is_instance_valid(bulkhead_status):
        return
    var linked: bool = radio.stage >= radio.SITES.size()
    bulkhead_material.set_shader_parameter("linked", 1.0 if linked else 0.0)
    bulkhead_status.text = "КАНАЛ ВОССТАНОВЛЕН" if linked else "КАНАЛ НЕДОСТУПЕН"
    bulkhead_status.modulate = Color("a2d6aa") if linked else Color("d0a06d")

func _house(pos: Vector3, size: Vector3, wall: Material, top: Material, window: Material) -> void:
    _box("Building", pos + Vector3(0,size.y/2,0), size, wall)
    _box("FlatRoof", pos + Vector3(0,size.y+0.25,0), Vector3(size.x+0.7,0.5,size.z+0.7), top)
    for side in [-1.0, 1.0]:
        _box("DarkWindow", pos + Vector3(side * size.x * 0.3, 2.5, -size.z * 0.51), Vector3(1.1,1.3,0.06), window, false)

func _anomaly(pos: Vector3, radius: float, glow_material: Material) -> void:
    var anomaly := MeshInstance3D.new()
    anomaly.name = "AnomalyField"
    var mesh := SphereMesh.new()
    mesh.radius = radius
    mesh.height = radius * 2.0
    mesh.radial_segments = 24
    mesh.rings = 12
    anomaly.mesh = mesh
    var color := (glow_material as StandardMaterial3D).albedo_color
    var phase := pos.x * .13 + pos.z * .19
    var material := ShaderMaterial.new()
    material.shader = preload("res://assets/anomaly/field_threads.gdshader")
    material.set_shader_parameter("field_color", color)
    material.set_shader_parameter("field_radius", radius)
    material.set_shader_parameter("seed_phase", phase)
    anomaly.material_override = material
    anomaly.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
    anomaly.position = pos + Vector3(0, radius * 0.40, 0)
    add_child(anomaly)
    var marker := MeshInstance3D.new()
    marker.name = "AnomalyBoundary"
    var plane := PlaneMesh.new()
    plane.size = Vector2.ONE * radius * 2.0
    marker.mesh = plane
    var ground := ShaderMaterial.new()
    ground.shader = preload("res://assets/anomaly/field_ground.gdshader")
    ground.set_shader_parameter("field_color", color)
    ground.set_shader_parameter("seed_phase", phase)
    marker.material_override = ground
    marker.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
    marker.position = pos + Vector3.UP * .075
    add_child(marker)
    anomaly_zones.append({"position": pos, "radius": radius, "visual": anomaly, "boundary": marker})

func _spawn_pickup(id: String, kind: String, pos: Vector3) -> void:
    var node := Node3D.new()
    node.name = id
    node.position = pos
    node.set_meta("loot_id", id)
    node.set_meta("kind", kind)
    var kits := {"medkit":preload("res://assets/zalesye/medkit.glb"),
        "battery":preload("res://assets/zalesye/battery.glb"),"scrap":preload("res://assets/zalesye/scrap.glb")}
    var visual := (kits[kind] as PackedScene).instantiate() as Node3D
    visual.position.y = -.46 if id != "bunker_supplies" else -.27
    visual.rotation.y = float(id.hash() % 19)*.31
    node.add_child(visual)
    add_child(node)
    pickups.append(node)

func _spawn_enemy(pos: Vector3, kind: int) -> void:
    var enemy = EnemyScript.new()
    enemy.name = "Wanderer" if kind == 0 else "Crawler"
    enemy.creature_type = kind
    enemy.player = player
    enemy.position = pos
    add_child(enemy)
    enemies.append(enemy)

func _check_anomalies(delta: float) -> void:
    anomaly_damage_clock -= delta
    for zone in anomaly_zones:
        var visual: MeshInstance3D = zone["visual"]
        visual.rotate_y(delta * 0.11)
        var location: Vector3 = zone["position"]
        if Vector2(player.position.x - location.x, player.position.z - location.z).length() < float(zone["radius"]) * 0.80:
            if anomaly_damage_clock <= 0.0:
                player.hit(6.0)
                anomaly_damage_clock = 1.2
                _show_message("ОПАСНО: аномальное поле!", 1.4)
            return

func _nearest_interactable() -> Dictionary:
    var candidate: Dictionary = {}
    var best_distance := 2.8
    var distance_to_shelter: float = player.global_position.distance_to(shelter_interaction)
    if distance_to_shelter < best_distance:
        candidate = {"kind":"shelter", "node":null}
        best_distance = distance_to_shelter
    for node in pickups:
        if not is_instance_valid(node) or collected_ids.has(str(node.get_meta("loot_id"))):
            continue
        var distance: float = player.global_position.distance_to(node.global_position)
        if distance < best_distance:
            best_distance = distance
            candidate = {"kind":"loot", "node":node}
    if radio.stage < radio.SITES.size():
        var radio_distance: float = player.global_position.distance_to(radio.SITES[radio.stage])
        if radio_distance < minf(best_distance,2.15):
            candidate = {"kind":"radio"}
    var postal: Dictionary = street.quarter.target(player.global_position)
    if not postal.is_empty() and float(postal.distance) < best_distance:
        return postal
    return candidate

func _interact() -> void:
    if is_dead:
        _respawn()
        return
    var target := _nearest_interactable()
    if target.is_empty():
        if inventory["medkit"] > 0 and player.health < 100:
            _use_medkit()
        else:
            _show_message("Подойди к предмету или убежищу", 2.0)
        return
    if target["kind"] == "door":
        if not street.quarter.turn_door(player.global_position):
            _show_message("Шагни в сторону от двери", 1.4)
    elif target["kind"] == "letter":
        letter_read = true
        _save_game()
        _show_message("«Вера, если автобус вернётся пустым, ищи меня у дежурного в Г-4. Ключ оставил на почте». Лёша, 17 октября.", 8.0)
    elif target["kind"] == "radio":
        var transmission: String = radio.tune()
        _sync_gallery_bulkhead()
        _save_game()
        if radio.stage >= radio.SITES.size():
            _show_message(transmission + "\nГ-4 • СВЯЗЬ ВОССТАНОВЛЕНА", 9.0)
        else:
            _show_message(transmission, 6.0)
    elif target["kind"] == "shelter":
        player.restore()
        _save_game()
        _show_message("Убежище: здоровье восстановлено, игра сохранена", 4.0)
    elif target["kind"] == "loot":
        var node: Node3D = target["node"]
        var kind: String = str(node.get_meta("kind"))
        var id: String = str(node.get_meta("loot_id"))
        if collected_ids.has(id):
            return
        collected_ids[id] = true
        inventory[kind] += 1
        if kind == "battery":
            player.battery = minf(100.0, player.battery + 35.0)
        pickups.erase(node)
        node.queue_free()
        _show_message("Найдено: %s" % kind, 2.5)

func _use_medkit() -> void:
    if is_dead or not player.alive:
        return
    if int(inventory["medkit"]) <= 0:
        _show_message("Нет аптечек", 1.7)
        return
    if player.health >= 100.0:
        _show_message("Здоровье уже полное", 1.7)
        return
    inventory["medkit"] -= 1
    player.health = minf(100.0, player.health + 45.0)
    _show_message("Аптечка: +45 здоровья", 2.3)


func _check_death() -> void:
    if player.health <= 0.0 and not is_dead:
        is_dead = true
        _show_message("Ты погиб. Нажми E / ДЕЙСТВИЕ для возрождения у убежища", 10000.0)

func _respawn() -> void:
    is_dead = false
    player.position = Vector3(-11.0, 0.3, 4.9)
    player.restore()
    _show_message("Возрождение у убежища", 3.0)

func _save_game() -> void:
    var state := {"version":1, "position":[player.position.x,player.position.y,player.position.z],
        "yaw":player.rotation.y, "pitch":player.camera.rotation.x,
        "health":player.health, "stamina":player.stamina, "battery":player.battery,
        "post_open":street.quarter.open, "letter_read":letter_read,
        "radio_stage":radio.stage, "inventory":inventory, "collected_ids":collected_ids}
    var file := FileAccess.open(SAVE_PATH, FileAccess.WRITE)
    if file == null:
        _show_message("Ошибка сохранения", 3.0)
        return
    file.store_string(JSON.stringify(state))
    file.close()

func _load_save() -> void:
    if not FileAccess.file_exists(SAVE_PATH):
        return
    var file := FileAccess.open(SAVE_PATH, FileAccess.READ)
    if file == null:
        return
    var parsed: Variant = JSON.parse_string(file.get_as_text())
    if typeof(parsed) != TYPE_DICTIONARY or parsed.get("version", -1) != 1:
        return
    street.quarter.restore_door(bool(parsed.get("post_open",false)))
    letter_read = bool(parsed.get("letter_read",false))
    radio.restore(int(parsed.get("radio_stage", 0)))
    _sync_gallery_bulkhead()
    var pos: Array = parsed.get("position", [])
    if pos.size() == 3:
        var p := Vector3(float(pos[0]), float(pos[1]), float(pos[2]))
        if absf(p.x) < WORLD_HALF_SIZE and absf(p.z) < WORLD_HALF_SIZE:
            player.position = p
    player.rotation.y = float(parsed.get("yaw", 0.0))
    player.camera.rotation.x = clampf(float(parsed.get("pitch", 0.0)), -1.35, 1.35)
    player.health = clampf(float(parsed.get("health", 100.0)), 1.0, 100.0)
    player.stamina = clampf(float(parsed.get("stamina", 100.0)), 0.0, 100.0)
    player.battery = clampf(float(parsed.get("battery", 100.0)), 0.0, 100.0)
    var saved_inventory: Dictionary = parsed.get("inventory", {})
    for key in inventory.keys():
        inventory[key] = maxi(0, int(saved_inventory.get(key, 0)))
    collected_ids = parsed.get("collected_ids", {})
    for node in pickups.duplicate():
        if collected_ids.has(str(node.get_meta("loot_id"))):
            pickups.erase(node)
            node.queue_free()

func _create_hud() -> void:
    hud = CanvasLayer.new()
    add_child(hud)
    var root := Control.new()
    root.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
    root.mouse_filter = Control.MOUSE_FILTER_IGNORE
    hud.add_child(root)
    stats_label = _label(root, Vector2(22, 16), Vector2(850, 34), 20)
    tip_label = _label(root, Vector2(22, 50), Vector2(900, 34), 16)
    status_label = _label(root, Vector2(22, 84), Vector2(1100, 52), 18)
    touch_controls = root
    if DisplayServer.is_touchscreen_available() or "--touch-ui" in OS.get_cmdline_user_args():
        # Overlay reads screen touch IDs itself; camera and joystick may
        # move independently, while sprint/torch/interaction use other fingers.
        var overlay = MobileControlsScript.new()
        overlay.name = "MobileControls"
        root.add_child(overlay)
        overlay.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
        overlay.configure(player, self)
        overlay.interact_requested.connect(_interact)
        overlay.flashlight_requested.connect(player.toggle_flashlight)
        overlay.medkit_requested.connect(_use_medkit)
        overlay.save_requested.connect(_save_game)
        stats_label.visible = false
        tip_label.visible = true
        status_label.position = Vector2(22, 86)
        status_label.add_theme_font_size_override("font_size", 17)

func _label(parent: Control, pos: Vector2, size: Vector2, font_size: int) -> Label:
    var label := Label.new()
    label.position = pos
    label.custom_minimum_size = size
    label.add_theme_font_size_override("font_size", font_size)
    label.add_theme_color_override("font_color", Color("ddf8e7"))
    label.add_theme_color_override("font_shadow_color", Color(0, 0, 0, 1))
    label.add_theme_constant_override("shadow_offset_x", 2)
    label.add_theme_constant_override("shadow_offset_y", 2)
    label.mouse_filter = Control.MOUSE_FILTER_IGNORE
    parent.add_child(label)
    return label

# Compatibility layer for existing Astra automated bunker/village walk tests.
# These functions DO NOT create on-screen arrow buttons.
func _touch_down(button_id: String) -> void:
    if button_id == "use":
        _interact()
        return
    if button_id == "torch":
        player.toggle_flashlight()
        return
    legacy_test_buttons[button_id] = true
    _recompute_test_input()

func _touch_up(button_id: String) -> void:
    legacy_test_buttons.erase(button_id)
    _recompute_test_input()

func _recompute_test_input() -> void:
    var x: int = int(legacy_test_buttons.has("right")) - int(legacy_test_buttons.has("left"))
    var y: int = int(legacy_test_buttons.has("back")) - int(legacy_test_buttons.has("forward"))
    player.virtual_move = Vector2(x, y).limit_length()
    player.virtual_sprint = legacy_test_buttons.has("run")


func _refresh_hud() -> void:
    stats_label.text = "ЗДР  %d     ВЫН  %d     СВЕТ  %d%%     ✚  %d     ЛОМ  %d" % [int(player.health), int(player.stamina), int(player.battery), int(inventory["medkit"]), int(inventory["scrap"])]
    if is_dead:
        tip_label.text = "E / ДЕЙСТВИЕ — возродиться"
        return
    var target := _nearest_interactable()
    if target.is_empty():
        tip_label.text = radio.CLUES[radio.stage] if radio.stage < 3 else "Г-4: связь восстановлена · маршрут завершён"
    elif target["kind"] == "radio":
        tip_label.text = radio.ACTIONS[radio.stage]
    elif target["kind"] == "door":
        tip_label.text = "Прикрыть дверь" if street.quarter.open else "Открыть почту"
    elif target["kind"] == "letter":
        tip_label.text = "Перечитать письмо Вере" if letter_read else "Прочитать последнее письмо"
    elif target["kind"] == "shelter":
        tip_label.text = "Убежище · лечение и сохранение"
    else:
        tip_label.text = "Подобрать предмет"

func _show_message(text_value: String, duration: float) -> void:
    if status_label != null:
        status_label.text = text_value
        message_clock = duration
