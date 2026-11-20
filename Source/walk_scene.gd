extends Node3D

func box(parent: Node3D, pos: Vector3, size: Vector3, color: String) -> MeshInstance3D:
	var m := BoxMesh.new()
	m.size = size
	var part := MeshInstance3D.new()
	part.mesh = m
	var mat := StandardMaterial3D.new()
	mat.albedo_color = Color(color)
	part.material_override = mat
	parent.add_child(part)
	part.position = pos
	return part

func label(parent: Node3D, pos: Vector3, s: String) -> Label3D:
	var l := Label3D.new()
	l.text = s
	l.position = pos
	l.font_size = 40
	l.pixel_size = 0.014
	parent.add_child(l)
	return l

func person(parent: Node3D, pos: Vector3, coat: String) -> void:
	box(parent,pos+Vector3.UP,Vector3(.72,1.3,.5),coat)
	box(parent,pos+Vector3.UP*1.93,Vector3(.43,.49,.42),"b18c74")
	for side in [-1,1]:
		box(parent,pos+Vector3(side*.18,.25,0),Vector3(.27,.5,.36),"282931")

func control(row: HBoxContainer, title: String) -> Button:
	var b := Button.new()
	b.text = title
	b.custom_minimum_size = Vector2(140,84)
	b.focus_mode = Control.FOCUS_NONE
	row.add_child(b)
	return b
