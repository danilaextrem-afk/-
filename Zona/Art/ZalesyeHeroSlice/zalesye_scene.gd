# Godot 4 editor script. Attach to Node3D in an isolated test scene; run scene to build geometry.
# Original geometry only. Not an accepted visual target. Issue #141 art blocker.
extends Node3D
const PALETTE = {
 "road": Color("#353337"), "stone": Color("#5b5859"), "plaster": Color("#857970"),
 "wood": Color("#4f3a37"), "roof": Color("#4b262e"), "rust": Color("#7a3a2b"),
 "glass": Color("#254c50"), "foliage": Color("#374441"), "glow": Color("#34d7be")
}
func block(label: String, pos: Vector3, size: Vector3, mat: String) -> void:
 var body := StaticBody3D.new()
 body.name = label
 body.position = pos
 var visual := MeshInstance3D.new()
 var mesh := BoxMesh.new()
 mesh.size = size
 var material := StandardMaterial3D.new()
 material.albedo_color = PALETTE[mat]
 material.roughness = 0.92
 if mat == "glow":
  material.emission_enabled = true
  material.emission = PALETTE["glow"]
 mesh.material = material
 visual.mesh = mesh
 body.add_child(visual)
 var collider := CollisionShape3D.new()
 var shape := BoxShape3D.new()
 shape.size = size
 collider.shape = shape
 body.add_child(collider)
 add_child(body)
func house(id: String, x: float, z: float, w: float, h: float, roof: String) -> void:
 block(id+"_walls",Vector3(x,h/2,z),Vector3(w,h,6),"plaster")
 block(id+"_roof",Vector3(x,h+0.22,z),Vector3(w+0.65,0.45,6.8),roof)
 for side in [-1,1]:
  block(id+"_window"+str(side),Vector3(x+side*w*0.27,1.8,z-3.07),Vector3(0.95,1.1,0.12),"glass")
 block(id+"_door",Vector3(x,1.15,z-3.12),Vector3(1.2,2.3,0.12),"wood")
func _ready() -> void:
 block("road",Vector3(0,-0.13,-24),Vector3(5.4,0.22,49),"road")
 for side in [-1,1]:
  block("sidewalk"+str(side),Vector3(side*3.65,-0.08,-24),Vector3(1.8,0.18,49),"stone")
  for j in 20:
   block("weed"+str(side)+"_"+str(j),Vector3(side*4.4,0.3,-1-j*2.4),Vector3(0.1,0.6,0.12),"foliage")
 house("store",-7,-9,6.5,4.2,"rust")
 house("house",7,-21,7,5.6,"roof")
 house("workshop",-7,-34,8,3.9,"wood")
 block("store_sign",Vector3(-7,3.1,-12.25),Vector3(3,0.65,0.16),"wood")
 block("house_balcony",Vector3(7,3.1,-24.5),Vector3(3,0.25,1.1),"stone")
 for i in 4:
  var z := -4.0-i*13.0
  var x := 4.6 if i%2==0 else -4.6
  block("pole"+str(i),Vector3(x,3.1,z),Vector3(0.17,6.2,0.17),"wood")
 for i in 9:
  var a := float(i)*TAU/9.0
  block("rift_shard"+str(i),Vector3(sin(a)*2.3,3.1+cos(a)*2.3,-48),Vector3(0.22,1.3,0.3),"glow")
 block("rift_core",Vector3(0,3.1,-48),Vector3(0.8,4,0.18),"glow")
