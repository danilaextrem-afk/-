extends Node3D

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
		rings.append([Vector3(-w*.92,s.z,s.x),Vector3(-w,s.z+.08,s.x),
			Vector3(-w*top_ratio,s.w,s.x),Vector3(w*top_ratio,s.w,s.x),
			Vector3(w,s.z+.08,s.x),Vector3(w*.92,s.z,s.x)])
	var surface := SurfaceTool.new()
	surface.begin(Mesh.PRIMITIVE_TRIANGLES)
	surface.set_smooth_group(-1)
	for i in range(rings.size()-1):
		for j in 6:
			var k := (j+1)%6
			for point in [rings[i][j],rings[i][k],rings[i+1][j],rings[i][k],rings[i+1][k],rings[i+1][j]]:
				surface.add_vertex(point)
	for j in range(1,5):
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
	var paint := finish(Color("#476278"),.48,.32)
	var glass := finish(Color("#303e49"),.30,.16)
	var rubber := finish(Color("#24272b"),.0,.94)
	var alloy := finish(Color("#899399"),.62,.35)
	shell([Vector4(-2.10,.79,.37,.73),Vector4(-1.74,.92,.33,.85),
		Vector4(-.80,.94,.34,.99),Vector4(.75,.94,.34,1.01),
		Vector4(1.60,.90,.34,.95),Vector4(2.03,.83,.38,.85)],.96,paint)
	shell([Vector4(-.93,.84,.88,1.01),Vector4(-.31,.81,.94,1.45),
		Vector4(.59,.82,.95,1.46),Vector4(1.32,.83,.90,1.04)],.83,glass)
	trim(Vector3(0,1.46,.14),Vector3(1.36,.045,.93),paint)
	for side in [-1,1]:
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
		for side in [-1,1]:
			trim(Vector3(side*.60,.68, -2.111 if end < 0 else 2.041),Vector3(.34,.115,.04),lens)
	trim(Vector3(0,.57,-2.12),Vector3(.70,.17,.045),rubber)
	trim(Vector3(0,.61,2.055),Vector3(.46,.13,.025),alloy)

func roll(distance: float) -> void:
	for wheel in wheels:
		wheel.rotate_x(-distance/.34)
