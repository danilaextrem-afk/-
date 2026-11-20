extends Node3D

enum Stage { RIDE, FIND_MARK, ASK_GUARD, PASS_GATE, COMPLETE }
const TABLET := Vector3(-3.5, 0, -99)
const GUARD := Vector3(3.5, 0, -99)
const REACH := 3.8
var stage := Stage.RIDE
var gate: MeshInstance3D
var bell_player := AudioStreamPlayer.new()

func piece(parent: Node3D, shape: Mesh, position_in_world: Vector3, color: Color) -> MeshInstance3D:
	var instance := MeshInstance3D.new()
	var material := StandardMaterial3D.new()
	material.albedo_color = color
	instance.material_override = material
	instance.mesh = shape
	parent.add_child(instance)
	instance.position = position_in_world
	return instance

func bell(parent: Node3D, where: Vector3, scale_factor: float) -> void:
	var body := CylinderMesh.new()
	body.top_radius = 0.19
	body.bottom_radius = 0.42
	body.height = 0.56
	var node := piece(parent, body, where, Color("#cf994c"))
	node.scale = Vector3.ONE * scale_factor
	var clapper := SphereMesh.new()
	clapper.radius = 0.13
	clapper.height = 0.26
	piece(node, clapper, Vector3(0,-0.31,0), Color("#d8b789"))

func build(forest: Node3D, city: Node3D) -> void:
	# The two matching bells share a silhouette even at a distance.
	bell(city, Vector3(-8.5,4.6,-44), 2.8)
	var stone := BoxMesh.new()
	stone.size = Vector3(1.3,2.2,0.65)
	piece(forest, stone, TABLET + Vector3.UP*1.1, Color("#555354"))
	bell(forest, TABLET + Vector3(0,1.5,0.5), 1.2)
	var coat := CapsuleMesh.new()
	coat.radius = 0.36
	coat.height = 1.55
	piece(forest, coat, GUARD+Vector3.UP*0.85, Color("#913d32"))
	var head := SphereMesh.new()
	head.radius = 0.23
	head.height = 0.46
	piece(forest, head, GUARD+Vector3.UP*1.8, Color("#c0a58b"))
	var door := BoxMesh.new()
	door.size = Vector3(6,6,0.6)
	gate = piece(forest, door, Vector3(0,3,-104), Color("#553c2f"))
	add_child(bell_player)
	bell_player.stream = bell_sound()

static func bell_sound() -> AudioStreamWAV:
	var sample_rate := 22050
	var frames := sample_rate * 2
	var samples := PackedByteArray()
	samples.resize(frames * 2)
	for i in frames:
		var t := float(i)/sample_rate
		var tone := sin(TAU*217*t) + 0.4*sin(TAU*569*t)*exp(-2*t)
		var envelope := minf(1,t*90)*exp(-3*t)
		samples.encode_s16(i*2, roundi(8000*tone*envelope))
	var sound := AudioStreamWAV.new()
	sound.format = AudioStreamWAV.FORMAT_16_BITS
	sound.mix_rate = sample_rate
	sound.data = samples
	return sound

func glimpse_started() -> void:
	bell_player.play()

func glimpse_ended() -> void:
	if stage == Stage.RIDE:
		stage = Stage.FIND_MARK

func action_at(pos: Vector3) -> String:
	if stage == Stage.FIND_MARK and pos.distance_to(TABLET) <= REACH:
		return "Осмотреть знак"
	if stage == Stage.ASK_GUARD and pos.distance_to(GUARD) <= REACH:
		return "Поговорить"
	return ""

func interact(pos: Vector3) -> void:
	if action_at(pos).is_empty():
		return
	if stage == Stage.FIND_MARK:
		stage = Stage.ASK_GUARD
		bell_player.play()
	elif stage == Stage.ASK_GUARD:
		stage = Stage.PASS_GATE
		gate.position.y = 10

func finish_at(pos: Vector3) -> void:
	if stage == Stage.PASS_GATE and pos.z <= -111:
		stage = Stage.COMPLETE

func reset() -> void:
	stage = Stage.RIDE
	gate.position.y = 3
	bell_player.stop()

func objective() -> String:
	match stage:
		Stage.FIND_MARK:
			return "У левой башни стоит камень. Сверь знак с тем, что видел в окне."
		Stage.ASK_GUARD:
			return "На камне тот же колокол. Спроси стражника в красном у правой башни."
		Stage.PASS_GATE:
			return "Стражник: «Язык сняли ещё зимой. Кто же звонит?»\nВорота открыты. Проезжай между башнями."
		Stage.COMPLETE:
			return "Расследование завершено · Немой колокол\nВ журнале: колокол без языка звонил в обеих эпохах."
	return "Езжай к воротам. Управление: стрелки или кнопки."
