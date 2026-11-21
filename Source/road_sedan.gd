extends Node3D

var paint_color := Color("#597991")
var tail_lens: StandardMaterial3D
var high_stop: MeshInstance3D
var braking := false
var wheels: Array[Node3D] = []

func finish(color: Color, metal: float, roughness: float) -> StandardMaterial3D:
	var mat := StandardMaterial3D.new()
	mat.albedo_color = color
	mat.metallic = metal
	mat.roughness = roughness
	return mat

func attach(parent: Node3D, mesh: Mesh, mat: Material, pos: Vector3) -> MeshInstance3D:
	var instance := MeshInstance3D.new()
	instance.mesh = mesh
	instance.material_override = mat
	parent.add_child(instance)
	instance.position = pos
	return instance

# Each station runs from the nose to the tail: length, half-width, sill and shoulder.
func shell(stations: Array, top_ratio: float, mat: Material) -> void:
	var rings: Array = []
	for s: Vector4 in stations:
		var w := s.y
		var shoulder := w*top_ratio
		rings.append([
			Vector3(-w*.88,s.z,s.x),Vector3(-w*.98,s.z+.055,s.x),
			Vector3(-w,s.z+.13,s.x),Vector3(-shoulder,s.w-.055,s.x),
			Vector3(-shoulder*.95,s.w-.012,s.x),Vector3(-shoulder*.76,s.w+.012,s.x),
			Vector3(0,s.w+.036,s.x),Vector3(shoulder*.76,s.w+.012,s.x),
			Vector3(shoulder*.95,s.w-.012,s.x),Vector3(shoulder,s.w-.055,s.x),
			Vector3(w,s.z+.13,s.x),Vector3(w*.98,s.z+.055,s.x),Vector3(w*.88,s.z,s.x)])
	var surface := SurfaceTool.new()
	surface.begin(Mesh.PRIMITIVE_TRIANGLES)
	surface.set_smooth_group(-1)
	for i in range(rings.size()-1):
		for j in rings[i].size():
			var k: int = (j+1)%rings[i].size()
			for point in [rings[i][j],rings[i][k],rings[i+1][j],rings[i][k],rings[i+1][k],rings[i+1][j]]:
				surface.add_vertex(point)
	for j in range(1,rings[0].size()-1):
		for point in [rings[0][0],rings[0][j+1],rings[0][j],rings[-1][0],rings[-1][j],rings[-1][j+1]]:
			surface.add_vertex(point)
	surface.generate_normals()
	attach(self,surface.commit(),mat,Vector3.ZERO)

func trim(pos: Vector3, size: Vector3, mat: Material) -> MeshInstance3D:
	var mesh := BoxMesh.new()
	mesh.size = size
	return attach(self,mesh,mat,pos)

func disc(parent: Node3D, radius: float, width: float, mat: Material, offset: float) -> void:
	var mesh := CylinderMesh.new()
	mesh.bottom_radius = radius
	mesh.top_radius = radius
	mesh.height = width
	mesh.radial_segments = 20
	mesh.rings = 1
	attach(parent,mesh,mat,Vector3(offset,0,0)).rotation.z = PI/2

func _ready() -> void:
	var paint := finish(paint_color,.32,.28)
	var glass := finish(Color("#303e49"),.30,.16)
	var rubber := finish(Color("#24272b"),.0,.94)
	var alloy := finish(Color("#899399"),.62,.35)
	shell([Vector4(-2.10,.79,.37,.73),Vector4(-1.74,.92,.33,.85),
		Vector4(-1.20,.94,.33,.91),Vector4(-.80,.94,.34,.99),Vector4(.75,.94,.34,1.01),
		Vector4(1.22,.925,.34,.98),Vector4(1.60,.90,.34,.95),Vector4(1.92,.87,.36,.91),Vector4(2.03,.83,.38,.85)],.96,paint)
	shell([Vector4(-.93,.84,.88,1.01),Vector4(-.31,.81,.94,1.45),
		Vector4(.59,.82,.95,1.46),Vector4(1.32,.83,.90,1.04)],.83,glass)
	trim(Vector3(0,1.484,.14),Vector3(1.13,.03,.86),paint)
	var heater := finish(Color("#6b574e"),.12,.51)
	for row in 6:
		var fraction := (row+1)/8.0
		var z := lerpf(.59,1.32,fraction)
		var y := lerpf(1.46,1.04,fraction)+.042
		trim(Vector3(0,y,z),Vector3(1.02,.008,.015),heater)
	for side in [-1,1]:
		var pillar := trim(Vector3(side*.674,1.29,.956),Vector3(.11,.085,.85),paint)
		pillar.rotation.x = .52
		trim(Vector3(side*.76,1.21,.17),Vector3(.07,.49,.085),rubber).rotation.z = side*.26
		trim(Vector3(side*.97,1.04,-.57),Vector3(.21,.14,.24),paint)
		for z in [-.16,.69]:
			trim(Vector3(side*.924,.85,z),Vector3(.04,.055,.16),alloy)
		for axle in [-1.29,1.26]:
			var wheel := Node3D.new()
			add_child(wheel)
			wheel.position = Vector3(side*.90,.34,axle)
			wheels.append(wheel)
			disc(wheel,.34,.21,rubber,0)
			disc(wheel,.235,.025,alloy,.11*side)
			for n in 5:
				var spoke := BoxMesh.new()
				spoke.size = Vector3(.016,.07,.095)
				var angle := TAU*n/5
				attach(wheel,spoke,rubber,Vector3(.127*side,.14*sin(angle),.14*cos(angle))).rotation.x = -angle
	for end in [-1,1]:
		var lens := finish(Color("#ffdfad") if end < 0 else Color("#df3a30"),.05,.26)
		lens.emission_enabled = true
		lens.emission = lens.albedo_color
		if end > 0:
			tail_lens = lens
			tail_lens.emission_energy_multiplier = .45
		for side in [-1,1]:
			trim(Vector3(side*.60,.68, -2.111 if end < 0 else 2.041),Vector3(.34,.115,.04),lens)
	high_stop = trim(Vector3(0,1.102,1.28),Vector3(.40,.045,.055),tail_lens)
	high_stop.hide()
	for side in [-1,1]:
		trim(Vector3(side*.78,.695,2.035),Vector3(.055,.145,.12),tail_lens)
	trim(Vector3(0,.57,-2.12),Vector3(.70,.17,.045),rubber)
	trim(Vector3(0,.61,2.055),Vector3(.46,.13,.025),finish(Color("#dbd8cb"),.08,.47))
	trim(Vector3(0,.43,2.04),Vector3(1.62,.095,.105),rubber)
	trim(Vector3(0,.79,2.05),Vector3(1.25,.022,.035),alloy)
	var registration := Label3D.new()
	registration.text = "К 204 ВМ"
	registration.pixel_size = .0016
	registration.font_size = 46
	registration.modulate = Color("#151923")
	registration.outline_size = 0
	registration.position = Vector3(0,.611,2.074)
	add_child(registration)

func roll(distance: float) -> void:
	for wheel in wheels:
		wheel.rotate_x(-distance/.34)

func set_braking(active: bool) -> void:
	if active == braking: return
	braking = active
	tail_lens.emission_energy_multiplier = 3.0 if active else .45
	high_stop.visible = active
