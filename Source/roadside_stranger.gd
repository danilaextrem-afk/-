extends Node3D

# Elliptical sections run from the hem upward. The same construction gives the
# coat its folds and the hood a forward overhang above the recessed face.
func garment(parent: Node3D, rings: Array, color: Color, folds := 0.0) -> MeshInstance3D:
	var mesh := SurfaceTool.new()
	mesh.begin(Mesh.PRIMITIVE_TRIANGLES)
	var rows: Array = []
	const SIDES := 18
	for ring: Vector4 in rings:
		var row := PackedVector3Array()
		for i in SIDES:
			var angle := TAU*i/SIDES
			var pleat := 1.0+folds*cos(angle*6.0+.4*ring.y)
			row.append(Vector3(ring.x*pleat*cos(angle),ring.y,ring.w+ring.z*pleat*sin(angle)))
		rows.append(row)
	for i in SIDES:
		var j := (i+1)%SIDES
		for level in rows.size()-1:
			var low: PackedVector3Array = rows[level]
			var high: PackedVector3Array = rows[level+1]
			for point in [low[i],low[j],high[i],low[j],high[j],high[i]]:
				mesh.add_vertex(point)
		for point in [rows[0][j],rows[0][i],Vector3(0,rings[0].y,rings[0].w),
				rows[-1][i],rows[-1][j],Vector3(0,rings[-1].y,rings[-1].w)]:
			mesh.add_vertex(point)
	mesh.generate_normals()
	mesh.index()
	var cloth := StandardMaterial3D.new()
	cloth.albedo_color = color
	cloth.roughness = .86
	var part := MeshInstance3D.new()
	part.mesh = mesh.commit()
	part.material_override = cloth
	parent.add_child(part)
	return part

func _ready() -> void:
	var coat := Color("#48494c")
	garment(self,[Vector4(.33,.39,.23,-.025),Vector4(.30,.73,.20,0),
		Vector4(.22,1.05,.15,0),Vector4(.27,1.35,.18,.01),
		Vector4(.30,1.50,.15,0),Vector4(.15,1.60,.13,0)],coat,.065)
	for side in [-1,1]:
		var boot := garment(self,[Vector4(.082,.015,.145,.05),Vector4(.086,.09,.15,.045),
			Vector4(.069,.19,.085,0),Vector4(.09,.53,.087,-.015)],Color("#323237"))
		boot.position = Vector3(side*.12,0,side*.035)
		var sleeve := garment(self,[Vector4(.07,.71,.078,.025),Vector4(.08,.89,.085,.012),
			Vector4(.10,1.15,.10,-.02),Vector4(.103,1.43,.11,0),Vector4(.07,1.51,.08,0)],coat.darkened(.10))
		sleeve.position.x = side*.39
		sleeve.rotation.z = side*.105
		var hand := garment(self,[Vector4(.042,.61,.05,.035),Vector4(.06,.70,.062,.035),
			Vector4(.045,.77,.047,.025)],Color("#706961"))
		hand.position.x = side*.31
	garment(self,[Vector4(.16,1.52,.13,-.02),Vector4(.207,1.65,.16,-.035),
		Vector4(.20,1.84,.17,-.02),Vector4(.146,1.97,.144,.01),
		Vector4(.065,2.013,.087,-.012)],coat.lightened(.08))
	garment(self,[Vector4(.046,1.59,.032,.116),Vector4(.127,1.70,.045,.122),
		Vector4(.137,1.82,.038,.126),Vector4(.10,1.874,.03,.119)],Color("#29292d"))
	rotation.y = -.32
