extends SceneTree

func _initialize() -> void:
    capture.call_deferred()

func capture() -> void:
    var studio := Node3D.new()
    root.add_child(studio)
    var env := WorldEnvironment.new()
    env.environment = Environment.new()
    env.environment.background_mode = Environment.BG_COLOR
    env.environment.background_color = Color("293442")
    env.environment.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
    env.environment.ambient_light_color = Color("c1c8d6")
    env.environment.ambient_light_energy = .95
    studio.add_child(env)
    var light := DirectionalLight3D.new()
    studio.add_child(light)
    light.rotation_degrees = Vector3(-45,-34,0)
    light.light_energy = 1.05
    var cam := Camera3D.new()
    studio.add_child(cam)
    cam.fov = 37
    cam.near = .01
    cam.far = 400
    var folder := ProjectSettings.globalize_path("res://captures/models")
    DirAccess.make_dir_recursive_absolute(folder)
    var files := ["transport/last_bus.glb","transport/field_torch.glb","transport/field_receiver.glb","transport/watchman.glb","transport/watchman_rework.gltf","transport/watchman_rework_lod.gltf","village/access_woods.glb","base/guardhouse04.gltf","bunker/g4_radio_cabinet.gltf","bunker/g4_radio_cabinet_lod.gltf","zalesye/zalesye_landform.gltf","zalesye/zalesye_landform_lod.gltf","zalesye/bramble.gltf","zalesye/bramble_lod.gltf","zalesye/trackside_workshop.gltf","zalesye/trackside_workshop_lod.gltf","zalesye/forest_access_landform.gltf","zalesye/forest_access_landform_lod.gltf","base/base_fence.gltf","base/base_fence_lod.gltf","base/base_fence_damaged.gltf","base/base_fence_damaged_lod.gltf","base/industrial_admin.gltf","base/industrial_admin_lod.gltf","base/industrial_warehouse.gltf","base/industrial_warehouse_lod.gltf","bunker/g4_switchgear.gltf","bunker/g4_switchgear_lod.gltf","bunker/g4_valve.gltf","bunker/g4_valve_lod.gltf","bunker/g4_vent.gltf","bunker/g4_vent_lod.gltf","zalesye/spruce_rework.gltf","zalesye/spruce_rework_lod.gltf","zalesye/broken_stones.gltf","zalesye/broken_stones_lod.gltf"]
    for kit in ["post_house","postal_door","bus_shelter","widow_house","water_tower","alder","birch","spruce","verge","field_lamp","battery","medkit","scrap","drainage","standing_water"]:
        files.append("zalesye/"+kit+".glb")
    for file in files:
        var model := load("res://assets/"+file).instantiate() as Node3D
        studio.add_child(model)
        var bounds := AABB()
        var first := true
        for mesh in model.find_children("*","MeshInstance3D",true,false):
            var part: AABB = mesh.global_transform * mesh.get_aabb()
            bounds = part if first else bounds.merge(part)
            first = false
        for side in [1,-1]:
            var eye := Vector3(1.15*side,.52,side).normalized()
            cam.position = bounds.get_center() + eye*bounds.size.length()*1.43
            cam.look_at(bounds.get_center())
            for frame in 3:
                await RenderingServer.frame_post_draw
            var im := root.get_texture().get_image()
            var path := folder.path_join("%s_%d.png" % [file.get_file().get_basename(),side])
            if im == null or im.save_png(path) != OK:
                push_error("Cannot capture "+path)
                quit(1)
                return
        model.free()
    quit()
