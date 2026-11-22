extends Node3D
## Prototype gameplay loop: explore -> scavenge -> survive -> shelter -> save.
## Procedural placeholder environment; NOT final concept-quality art.

const PlayerScript = preload("res://scripts/player.gd")
const EnemyScript = preload("res://scripts/enemy.gd")
const SAVE_PATH = "user://razlom_zona_save_v1.json"
const WORLD_HALF_SIZE = 46.0

var player
var inventory: Dictionary = {"medkit": 0, "battery": 0, "scrap": 0}
var collected_ids: Dictionary = {}
var pickups: Array[Node3D] = []
var enemies: Array[CharacterBody3D] = []
var anomaly_zones: Array[Dictionary] = []
var shelter_interaction := Vector3(-11.0, 0.7, 5.65)
var anomaly_damage_clock: float = 0.0
var message_clock: float = 0.0
var is_dead: bool = false
var touch_pressed: Dictionary = {}

var hud: CanvasLayer
var stats_label: Label
var tip_label: Label
var status_label: Label
var touch_controls: Control

func _ready() -> void:
    _setup_environment()
    _create_world()
    player = PlayerScript.new()
    player.position = Vector3(-39.0, 0.25, 2.0)
    player.rotation.y = -PI / 2.0
    add_child(player)
    _spawn_enemy(Vector3(21.0, 0.3, -17.0), 0)
    _spawn_enemy(Vector3(-26.0, 0.3, -24.0), 1)
    _create_hud()
    _load_save()
    _show_message("Залесье   /   найти припасы", 4.5)

func _process(delta: float) -> void:
    if not is_instance_valid(player):
        return
    _animate_pickups(delta)
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
    sky_material.shader = preload("res://assets/village/storm.gdshader")
    sky_material.set_shader_parameter("panorama", preload("res://assets/village/storm.jpg"))
    var sky := Sky.new()
    sky.sky_material = sky_material
    env.environment.sky = sky
    env.environment.background_mode = Environment.BG_SKY
    env.environment.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
    env.environment.ambient_light_color = Color("a99da5")
    env.environment.ambient_light_energy = 0.78
    env.environment.reflected_light_source = Environment.REFLECTION_SOURCE_SKY
    env.environment.fog_enabled = true
    env.environment.fog_light_color = Color("975e5e")
    env.environment.fog_density = 0.0055
    env.environment.fog_sky_affect = 0.0
    add_child(env)
    var moon := DirectionalLight3D.new()
    moon.rotation_degrees = Vector3(-19.0, 69.0, 0.0)
    moon.light_color = Color("ffac74")
    moon.light_energy = 1.25
    moon.shadow_enabled = true
    moon.directional_shadow_max_distance = 35.0
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
    var earth := _material("26332d")
    var pavement := _material("374146")
    var cement := _material("596161")
    var roof := _material("283a43")
    var wood := _material("4b4239")
    var moss := _material("516d4d")
    var scrap := _material("756b5c")
    var dark := _material("172329")
    var mint := _material("6cf0bb", true)
    var cyan := _material("43c8c8", true)

    # Four terrain slabs leave the descent open from z=-20 to z=-40.
    _box("WestTerrain", Vector3(-12, -0.6, 0), Vector3(68, 1.2, 92), earth)
    _box("EastTerrain", Vector3(36, -0.6, 0), Vector3(20, 1.2, 92), earth)
    _box("Approach", Vector3(24, -0.6, 13), Vector3(4, 1.2, 66), earth)
    _box("FarTerrain", Vector3(24, -0.6, -43), Vector3(4, 1.2, 6), earth)
    _box("BuriedRoof", Vector3(24, -0.12, -37), Vector3(4, 0.24, 6), earth)
    _box("MainRoad", Vector3(0, 0.025, 2), Vector3(88, 0.06, 7), pavement)
    _box("SideRoad", Vector3(18, 0.03, -12), Vector3(6, 0.05, 48), pavement)
    # A fictional abandoned settlement, not a real location.
    var street := preload("res://scripts/crimson_street.gd").new()
    street.name = "CrimsonStreet"
    street.position = Vector3(-39, 0, 2)
    street.rotation.y = -PI / 2.0
    add_child(street)
    _house(Vector3(-3, 0, -20), Vector3(11, 8, 9), cement, roof, dark)
    _house(Vector3(30, 0, -6), Vector3(9, 6, 10), wood, roof, dark)
    _house(Vector3(28, 0, 22), Vector3(8, 5, 8), cement, roof, dark)
    _house(Vector3(-30, 0, 22), Vector3(10, 5, 8), wood, roof, dark)
    _box("OldWarehouse", Vector3(1, 4, 29), Vector3(15, 8, 12), _material("414f50"))
    _box("WarehouseRoof", Vector3(1, 8.2, 29), Vector3(16, 0.55, 13), roof)
    var gallery := preload("res://scripts/bunker.gd").new()
    gallery.name = "BunkerGallery"
    gallery.position = Vector3(24, 0, -20)
    add_child(gallery)
    _box("GalleryEnd", Vector3(24, -3.55, -40.13), Vector3(4.1, 3.2, 0.26), cement)
    _spawn_pickup("bunker_supplies", "medkit", Vector3(24, -4.10, -38.8))
    _box("WatchPlatform", Vector3(-37, 5, -30), Vector3(5, 0.7, 5), wood)
    for x in [-39.0, -35.0]:
        for z in [-32.0, -28.0]:
            _box("WatchLeg", Vector3(x, 2.45, z), Vector3(0.45, 4.9, 0.45), wood)
    # Deterministic decoration positions; avoids endless terrain generation on mobile.
    var trees: Array[Vector3] = [Vector3(-36,0,-8), Vector3(-41,0,17), Vector3(-24,0,36), Vector3(17,0,37), Vector3(38,0,37), Vector3(40,0,8), Vector3(38,0,-34), Vector3(-17,0,-37), Vector3(8,0,-38), Vector3(-41,0,-39)]
    for pos in trees:
        if pos.distance_to(Vector3(-39, 0, 2)) > 18.0:
            _tree(pos, wood, moss)
    for i in range(12):
        var x := float((i * 19) % 79 - 39)
        var z := float((i * 37) % 77 - 38)
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

func _house(pos: Vector3, size: Vector3, wall: Material, top: Material, window: Material) -> void:
    _box("Building", pos + Vector3(0,size.y/2,0), size, wall)
    _box("FlatRoof", pos + Vector3(0,size.y+0.25,0), Vector3(size.x+0.7,0.5,size.z+0.7), top)
    for side in [-1.0, 1.0]:
        _box("DarkWindow", pos + Vector3(side * size.x * 0.3, 2.5, -size.z * 0.51), Vector3(1.1,1.3,0.06), window, false)

func _tree(pos: Vector3, trunk: Material, crown: Material) -> void:
    _box("TreeTrunk", pos + Vector3(0, 2.0, 0), Vector3(0.7,4,0.7), trunk)
    _box("TreeCrown", pos + Vector3(0, 5.0, 0), Vector3(3.2,3.0,3.2), crown, false)
    _box("TreeCrownUpper", pos + Vector3(0, 7.0, 0), Vector3(2.2,2.0,2.2), crown, false)

func _anomaly(pos: Vector3, radius: float, glow_material: Material) -> void:
    var anomaly := MeshInstance3D.new()
    anomaly.name = "AnomalyField"
    var mesh := SphereMesh.new()
    mesh.radius = radius
    mesh.height = radius * 2.0
    mesh.radial_segments = 8
    mesh.rings = 4
    anomaly.mesh = mesh
    var m: StandardMaterial3D = (glow_material as StandardMaterial3D).duplicate() as StandardMaterial3D
    m.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
    m.albedo_color.a = 0.13
    m.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
    anomaly.material_override = m
    anomaly.position = pos + Vector3(0, radius * 0.40, 0)
    add_child(anomaly)
    anomaly_zones.append({"position": pos, "radius": radius, "visual": anomaly})

func _spawn_pickup(id: String, kind: String, pos: Vector3) -> void:
    var node := Node3D.new()
    node.name = id
    node.position = pos
    node.set_meta("loot_id", id)
    node.set_meta("kind", kind)
    var cube := MeshInstance3D.new()
    var boxmesh := BoxMesh.new()
    boxmesh.size = Vector3(0.42, 0.42, 0.42)
    cube.mesh = boxmesh
    cube.material_override = _material("6cf0bb", true) if kind == "medkit" else (_material("72bad6", true) if kind == "battery" else _material("b6a476"))
    node.add_child(cube)
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

func _animate_pickups(delta: float) -> void:
    for node in pickups:
        if not is_instance_valid(node):
            continue
        node.rotate_y(0.8 * delta)

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
    return candidate

func _interact() -> void:
    if is_dead:
        _respawn()
        return
    var target := _nearest_interactable()
    if target.is_empty():
        if inventory["medkit"] > 0 and player.health < 100:
            inventory["medkit"] -= 1
            player.health = minf(100.0, player.health + 45.0)
            _show_message("Использована аптечка (+45 здоровья)", 3.0)
        else:
            _show_message("Подойди к предмету, убежищу или используй аптечку", 2.0)
        return
    if target["kind"] == "shelter":
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
        "inventory":inventory, "collected_ids":collected_ids}
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
    if not DisplayServer.is_touchscreen_available():
        return
    _touch_button("▲", Vector2(124, -222), Vector2(68, 68), "forward")
    _touch_button("▼", Vector2(124, -84), Vector2(68, 68), "back")
    _touch_button("◀", Vector2(47, -153), Vector2(68, 68), "left")
    _touch_button("▶", Vector2(201, -153), Vector2(68, 68), "right")
    _touch_button("БЕГ", Vector2(-350, -82), Vector2(80, 60), "run", true)
    _touch_button("ДЕЙСТВИЕ", Vector2(-252, -82), Vector2(124, 60), "use", true)
    _touch_button("СВЕТ", Vector2(-114, -82), Vector2(95, 60), "torch", true)

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

func _touch_button(caption: String, offset: Vector2, size: Vector2, button_id: String, right_anchor: bool = false) -> void:
    var b := Button.new()
    b.text = caption
    b.name = button_id
    b.custom_minimum_size = size
    touch_controls.add_child(b)
    if right_anchor:
        b.anchor_left = 1.0
        b.anchor_right = 1.0
    else:
        b.anchor_left = 0.0
        b.anchor_right = 0.0
    b.anchor_top = 1.0
    b.anchor_bottom = 1.0
    b.offset_left = offset.x
    b.offset_right = offset.x + size.x
    b.offset_top = offset.y
    b.offset_bottom = offset.y + size.y
    b.modulate = Color(0.77, 0.98, 0.86, 0.80)
    b.add_theme_font_size_override("font_size", 17)
    b.button_down.connect(func(): _touch_down(button_id))
    b.button_up.connect(func(): _touch_up(button_id))

func _touch_down(button_id: String) -> void:
    if button_id == "use":
        _interact()
    elif button_id == "torch":
        player.toggle_flashlight()
    else:
        touch_pressed[button_id] = true
        _recompute_virtual_move()

func _touch_up(button_id: String) -> void:
    touch_pressed.erase(button_id)
    _recompute_virtual_move()

func _recompute_virtual_move() -> void:
    var x := int(touch_pressed.has("right")) - int(touch_pressed.has("left"))
    var y := int(touch_pressed.has("back")) - int(touch_pressed.has("forward"))
    player.virtual_move = Vector2(x, y).limit_length()
    player.virtual_sprint = touch_pressed.has("run")

func _refresh_hud() -> void:
    stats_label.text = "ЗДР  %d     ВЫН  %d     СВЕТ  %d%%     ✚  %d     ЛОМ  %d" % [int(player.health), int(player.stamina), int(player.battery), int(inventory["medkit"]), int(inventory["scrap"])]
    if is_dead:
        tip_label.text = "E / ДЕЙСТВИЕ — возродиться"
        return
    var target := _nearest_interactable()
    if target.is_empty():
        tip_label.text = ""
    else:
        tip_label.text = "E / ДЕЙСТВИЕ — %s" % ("убежище: лечение и сохранение" if target["kind"] == "shelter" else "подобрать предмет")

func _show_message(text_value: String, duration: float) -> void:
    if status_label != null:
        status_label.text = text_value
        message_clock = duration
