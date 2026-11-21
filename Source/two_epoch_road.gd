extends Node3D

const Glimpse = preload("res://Source/time_glimpse.gd")
const FOREST_SOUND := preload("res://Content/Audio/forest_1348.tres")
const CITY_SOUND := preload("res://Content/Audio/traffic_2026.tres")
var ambience := AudioStreamPlayer.new()
var journal := Label.new()
var action: Button
var clock := Glimpse.new()
var forest := Node3D.new()
var city := Node3D.new()
var mount := Node3D.new()
var horse := preload("res://Source/road_horse.gd").new()
var car := preload("res://Source/road_sedan.gd").new()
var daylight := DirectionalLight3D.new()
var pavement: MeshInstance3D
var camera := Camera3D.new()
var caption := Label.new()
var left: Button
var right: Button
var ride: Button

func block(parent: Node3D, pos: Vector3, size: Vector3, color: String) -> MeshInstance3D:
	var part := MeshInstance3D.new()
	var box := BoxMesh.new()
	box.size = size
	var mat := StandardMaterial3D.new()
	mat.albedo_color = Color(color)
	box.material = mat
	part.mesh = box
	parent.add_child(part)
	part.position = pos
	return part

func button(row: HBoxContainer, label: String) -> Button:
	var b := Button.new()
	b.text = label
	b.add_theme_font_size_override("font_size",23)
	b.custom_minimum_size = Vector2(172,86)
	row.add_child(b)
	return b

func _ready() -> void:
	add_child(ambience)
	ambience.volume_db = -9.0
	for root in [forest, city, mount]:
		add_child(root)
	mount.add_child(horse)
	mount.add_child(car)
	for world in [forest,city]:
		block(world,Vector3(0,-0.6,-55),Vector3(180,1,230),"40533d" if world == forest else "717675")
		var surface := block(world,Vector3(0,-0.08,-55),Vector3(12,0.16,160),"85664a" if world == forest else "434952")
		if world == city: pavement = surface
	var cube := BoxMesh.new()
	cube.size = Vector3.ONE
	var masonry := StandardMaterial3D.new()
	masonry.vertex_color_use_as_albedo = true
	var windows := masonry.duplicate() as StandardMaterial3D
	windows.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
	for i in range(18):
		var z := -7.0*i
		for side in [-1,1]:
			var x: float = side*(9+i%3)
			block(forest,Vector3(x,2,z),Vector3(1,4,1),"554132")
			block(forest,Vector3(x,5,z),Vector3(4,5+i%3,4),"284637")
			if i%3 == 0:
				var house := preload("res://Source/panel_house.gd").new()
				city.add_child(house)
				house.position = Vector3(side*15,0,z)
				house.rotation.y = -side*PI/2
				house.build(i + (1 if side > 0 else 0),cube,masonry,windows)
		block(city,Vector3(0,0.02,z),Vector3(0.14,0.04,3),"e7ddb2")
	for side in [-1,1]:
		block(forest,Vector3(side*6,5,-107),Vector3(6,10,7),"726d64")
	block(forest,Vector3(0,8,-107),Vector3(10,2,3),"726d64")
	build_location()
	mount.add_child(camera)
	camera.position = Vector3(0,4.5,7.4)
	camera.rotation.x = -0.32
	add_child(daylight)
	daylight.rotation_degrees = Vector3(-55,-25,0)
	var ui := CanvasLayer.new()
	add_child(ui)
	ui.add_child(caption)
	caption.position = Vector2(42,36)
	caption.add_theme_font_size_override("font_size",32)
	caption.add_theme_constant_override("outline_size",6)
	ui.add_child(journal)
	journal.position = Vector2(42,190)
	journal.add_theme_font_size_override("font_size",30)
	journal.add_theme_constant_override("outline_size",6)
	journal.hide()
	var row := HBoxContainer.new()
	ui.add_child(row)
	row.set_anchors_and_offsets_preset(Control.PRESET_BOTTOM_LEFT)
	row.position += Vector2(42,-110)
	left = button(row,"Влево")
	ride = button(row,"Ехать ▲")
	right = button(row,"Вправо")
	action = button(row,"Осмотреть знак")
	action.pressed.connect(interact)
	button(row,"Журнал").pressed.connect(toggle_journal)
	button(row,"Заново").pressed.connect(restart)
	add_controls(row)
	update_view()

func build_location() -> void:
	pass

func add_controls(_row: HBoxContainer) -> void:
	pass

func update_view() -> void:
	pass

func restart() -> void:
	pass

func interact() -> void:
	pass

func toggle_journal() -> void:
	journal.visible = not journal.visible

func sync_road_sound() -> void:
	var layer := FOREST_SOUND if forest.visible else CITY_SOUND
	if ambience.stream != layer:
		ambience.stream = layer
		ambience.play()
