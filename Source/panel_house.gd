extends Node3D

# Local front faces +Z. The street scene rotates the same house for either curb.
const CONCRETE := Color("#99948b")
const TRIM := Color("#655f57")
const FRAME := Color("#aba79b")
const NIGHT_GLASS := Color("#374553")
const LIT := [Color("#dbae76"), Color("#e4cc98"), Color("#bdd0c9")]
var structure: Array[Transform3D] = []
var finishes: Array[Color] = []
var panes: Array[Transform3D] = []
var lights: Array[Color] = []

func part(center: Vector3, size: Vector3, color: Color, lit := false) -> void:
	var transform_3d := Transform3D(Basis.from_scale(size),center)
	if lit:
		panes.append(transform_3d)
		lights.append(color)
	else:
		structure.append(transform_3d)
		finishes.append(color)

func batch(title: String, shapes: Array[Transform3D], palette: Array[Color], material: Material, cube: BoxMesh) -> void:
	var group := MultiMesh.new()
	group.transform_format = MultiMesh.TRANSFORM_3D
	group.use_colors = true
	group.use_custom_data = title == "WallsAndJoinery"
	group.mesh = cube
	group.instance_count = shapes.size()
	for i in shapes.size():
		group.set_instance_transform(i,shapes[i])
		group.set_instance_color(i,palette[i])
		if group.use_custom_data:
			group.set_instance_custom_data(i,Color(1 if i == 0 else 0,0,0,1))
	var instance := MultiMeshInstance3D.new()
	instance.name = title
	instance.multimesh = group
	instance.material_override = material
	add_child(instance)

func build(variant: int, cube: BoxMesh, masonry: Material, windows: Material) -> void:
	var floors := 4 + (floori(variant/3.0) + variant) % 3
	var height := 12.0 + (floors-4)*2.65
	var finish: Color = [CONCRETE,Color("#ad9888"),Color("#939792"),Color("#a89e86")][variant%4]
	part(Vector3(0,height*.5,0),Vector3(15,height,12),finish)
	part(Vector3(0,height+.1,0),Vector3(15.35,.24,12.3),TRIM)
	part(Vector3(2,height+.67,-1),Vector3(2.6,1.13,2.1),TRIM)
	for end in [-1,1]:
		for level in floors:
			var sill := 1.9+2.65*level
			for bay in [-1,1]:
				var depth: float = bay*2.7
				part(Vector3(end*7.53,sill,depth),Vector3(.13,1.48,1.22),FRAME)
				var on: bool = (level+variant+bay)%3 == 0
				part(Vector3(end*7.62,sill,depth),Vector3(.055,1.29,1.03),LIT[variant%3] if on else NIGHT_GLASS,on)
				part(Vector3(end*7.66,sill,depth),Vector3(.04,1.3,.055),FRAME)
				part(Vector3(end*7.65,sill-.76,depth),Vector3(.32,.09,1.36),TRIM)
	part(Vector3(0,.32,6.02),Vector3(15,.64,.14),TRIM)
	for floor_id in floors:
		var y := 2.15 + 2.65*floor_id
		part(Vector3(0,y-1.24,6.015),Vector3(15,.045,.055),TRIM)
		for col in 5:
			var x := (col-2)*2.83
			var seed_value := variant*31 + col*7 + floor_id*13
			if floor_id == 0 and col == 2: continue
			part(Vector3(x,y,6.045),Vector3(1.59,1.44,.12),FRAME)
			var lit := seed_value%5 < 2
			var glass: Color = LIT[seed_value%3] if lit else NIGHT_GLASS
			part(Vector3(x,y,6.12),Vector3(1.41,1.26,.055),glass,lit)
			part(Vector3(x+.14,y,6.17),Vector3(.067,1.28,.05),FRAME)
			part(Vector3(x,y-.74,6.16),Vector3(1.72,.085,.33),TRIM)
			if floor_id > 0 and col == (1 if variant%2 == 0 else 3):
				part(Vector3(x,y-.83,6.55),Vector3(2.32,.14,1.1),CONCRETE)
				part(Vector3(x,y-.37,7.04),Vector3(2.32,.86,.1),Color("#798580"))
				for edge in [-1,1]:
					part(Vector3(x+edge*1.11,y-.37,6.55),Vector3(.1,.86,.98),TRIM)
	for seam in [-1.5,-.5,.5,1.5]:
		part(Vector3(seam*2.83,height*.5,6.02),Vector3(.043,height-.5,.055),TRIM)
	part(Vector3(0,1.16,6.09),Vector3(1.50,2.32,.2),Color("#3c544f"))
	part(Vector3(.38,1.04,6.22),Vector3(.06,.32,.08),FRAME)
	part(Vector3(0,2.65,6.68),Vector3(2.72,.18,1.65),TRIM)
	part(Vector3(0,2.49,6.55),Vector3(.50,.08,.32),LIT[0],true)
	part(Vector3(0,.09,6.65),Vector3(2.2,.18,1.35),CONCRETE)
	batch("WallsAndJoinery",structure,finishes,masonry,cube)
	batch("IlluminatedPanes",panes,lights,windows,cube)
	structure.clear()
	finishes.clear()
	panes.clear()
	lights.clear()
