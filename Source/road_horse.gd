extends Node3D

# The animation displaces only visual joints. Forward travel belongs to the road.
var barrel := Node3D.new()
var hips: Array[Node3D] = []
var knees: Array[Node3D] = []
var stride := 0.0
var oval := SphereMesh.new()
var finishes := {}

func shape(parent: Node3D, center: Vector3, size: Vector3, color: String, tilt := 0.0) -> void:
	if not finishes.has(color):
		var mat := StandardMaterial3D.new()
		mat.albedo_color = Color(color)
		finishes[color] = mat
	var piece := MeshInstance3D.new()
	piece.mesh = oval
	piece.material_override = finishes[color]
	parent.add_child(piece)
	piece.position = center
	piece.scale = size
	piece.rotation.x = tilt

func joint(parent: Node3D, center: Vector3) -> Node3D:
	var pivot := Node3D.new()
	parent.add_child(pivot)
	pivot.position = center
	return pivot

func _ready() -> void:
	oval.radial_segments = 12
	oval.rings = 6
	add_child(barrel)
	var bay := "78462e"
	var dark := "382b27"
	shape(barrel,Vector3(0,1.25,.02),Vector3(.82,.92,1.91),bay)
	shape(barrel,Vector3(0,1.74,-.77),Vector3(.5,1.1,.65),bay,-.43)
	shape(barrel,Vector3(0,2.12,-1.22),Vector3(.42,.52,.83),bay,-.28)
	shape(barrel,Vector3(0,2.03,-1.52),Vector3(.36,.33,.37),dark)
	shape(barrel,Vector3(0,1.87,-.63),Vector3(.22,.95,.30),dark,-.44)
	shape(barrel,Vector3(0,.98,1.01),Vector3(.23,.88,.31),dark,.27)
	shape(barrel,Vector3(0,1.65,.08),Vector3(.76,.24,.79),"583827")
	shape(barrel,Vector3(0,2.00,.1),Vector3(.55,.80,.39),"505963")
	shape(barrel,Vector3(0,2.51,.08),Vector3(.32,.41,.34),"bd9480")
	for side: int in [-1,1]:
		shape(barrel,Vector3(.15*side,2.39,-1.01),Vector3(.12,.28,.13),bay)
		shape(barrel,Vector3(.197*side,2.20,-1.28),Vector3(.04,.065,.07),"272321")
		shape(barrel,Vector3(.34*side,1.47,.1),Vector3(.22,.63,.28),"403d39")
		shape(barrel,Vector3(.30*side,2,-.13),Vector3(.17,.45,.18),"505963",.65)
		for axle in [-.58,.62]:
			var hip := joint(barrel,Vector3(.285*side,1.26,axle))
			hips.append(hip)
			shape(hip,Vector3(0,-.30,0),Vector3(.23,.72,.32),bay)
			var knee := joint(hip,Vector3(0,-.59,0))
			knees.append(knee)
			shape(knee,Vector3(0,-.29,0),Vector3(.14,.64,.16),dark)
			shape(knee,Vector3(0,-.58,-.035),Vector3(.18,.18,.25),"302e2d")

func pose(distance: float) -> void:
	if distance <= 0:
		stride = 0
	else:
		stride = fmod(stride + distance * TAU / 3.2, TAU)
	var moving := distance > 0
	for i in hips.size():
		var wave := sin(stride + (PI if i in [1,2] else 0.0)) if moving else 0.0
		hips[i].rotation.x = .50 * wave
		knees[i].rotation.x = -.47 * maxf(0.0,wave)
	barrel.position.y = .055 * absf(sin(stride))
