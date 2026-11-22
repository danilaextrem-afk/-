extends Node3D
## Each bay ends one metre below its entry socket.
const BAY := preload("res://assets/bunker/gallery.gltf")
const BAY_COUNT := 5

func _ready() -> void:
    for index in BAY_COUNT:
        var bay := BAY.instantiate() as Node3D
        bay.position = Vector3(0, -index, -4 * index)
        add_child(bay)
        for mesh in bay.find_children("*", "MeshInstance3D", true, false):
            mesh.create_trimesh_collision()
        if index % 2 == 0:
            var lamp := OmniLight3D.new()
            lamp.position = Vector3(0.49, 2.1, -1.2)
            lamp.light_color = Color("95cebd")
            lamp.light_energy = 1.6
            lamp.omni_range = 5.5
            bay.add_child(lamp)
