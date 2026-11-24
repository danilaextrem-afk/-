extends Node3D

const POST := preload("res://assets/zalesye/post_house.glb")
const DOOR := preload("res://assets/zalesye/postal_door.glb")
const HOUSE := preload("res://assets/zalesye/widow_house.glb")
const TOWER := preload("res://assets/zalesye/water_tower.glb")
const STOP := preload("res://assets/zalesye/bus_shelter.glb")

var post: Node3D
var door := AnimatableBody3D.new()
var open := false
var moving: Tween
var hinge := AudioStreamPlayer3D.new()
var note_point := Vector3(-1.03,1.02,-.44)

func _ready() -> void:
    post = _model(POST,"PostOffice",Vector3(8.65,0,-7.35),-.75)
    _box(post,Vector3(0,1.92,-2.45),Vector3(8.25,3.84,.21))
    for side in [-1,1]:
        _box(post,Vector3(4.03*side,1.92,0),Vector3(.19,3.84,4.88))
    # Split the frontage at the actual door. The window sills remain solid.
    _box(post,Vector3(-1.25,1.85,2.45),Vector3(5.70,3.7,.31))
    _box(post,Vector3(3.48,1.85,2.45),Vector3(1.24,3.7,.31))
    _box(post,Vector3(2.23,3.10,2.45),Vector3(1.28,1.10,.31))
    _box(post,Vector3(-1.02,.48,-.68),Vector3(4.05,.96,.83))
    _box(post,Vector3(2.80,.26,-1.23),Vector3(1.58,.52,.43))
    for x in [1.34,3.17]:
        _box(post,Vector3(x,1.31,3.50),Vector3(.13,2.62,.13))
    door.name = "PublicDoor"
    door.sync_to_physics = false
    door.position = Vector3(1.61,0,2.45)
    post.add_child(door)
    door.add_child(DOOR.instantiate())
    _shape(door,Vector3(.61,1.17,0),Vector3(1.22,2.34,.095))
    hinge.stream = preload("res://assets/zalesye/hinge.ogg")
    hinge.max_distance = 12
    hinge.unit_size = 2.4
    door.add_child(hinge)
    var house := _model(HOUSE,"WidowsCottage",Vector3(-9.1,0,-20),1.24)
    _box(house,Vector3(0,1.47,0),Vector3(5.95,2.94,4.62))
    var tower := _model(TOWER,"WaterTower",Vector3(-5.9,0,-34.0),-.4)
    var pier := StaticBody3D.new()
    tower.add_child(pier)
    var collision := CollisionShape3D.new()
    var cylinder := CylinderShape3D.new()
    cylinder.radius = 1.59
    cylinder.height = 6
    collision.shape = cylinder
    collision.position.y = 3
    pier.add_child(collision)
    var stop := _model(STOP,"BusShelter",Vector3(6.9,0,-17.65),-1.42)
    _box(stop,Vector3(0,1.28,-.87),Vector3(3.17,2.46,.13))
    _box(stop,Vector3(0,.26,-.44),Vector3(2.49,.52,.47))
    for x in [-1.52,1.52]:
        _box(stop,Vector3(x,1.28,.81),Vector3(.12,2.56,.12))
    var far_house := _model(HOUSE,"TracksideCottage",Vector3(-13.5,0,-31.2),.48)
    _box(far_house,Vector3(0,1.47,0),Vector3(5.95,2.94,4.62))
    _model(preload("res://assets/zalesye/drainage.glb"),"RoadsideDrains",Vector3.ZERO,0)
    var pools := _model(preload("res://assets/zalesye/standing_water.glb"),"WheelRutPools",Vector3.ZERO,0)
    var surface := ShaderMaterial.new()
    surface.shader = preload("res://assets/zalesye/ditch_water.gdshader")
    surface.set_shader_parameter("clouds",preload("res://assets/zalesye/evening.jpg"))
    for part in pools.find_children("*","MeshInstance3D",true,false):
        part.material_override = surface
        part.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
    add_child(preload("res://scripts/zalesye_grove.gd").new())

func _model(scene: PackedScene, label: String, at: Vector3, yaw: float) -> Node3D:
    var model := scene.instantiate() as Node3D
    model.name = label
    model.position = at
    model.rotation.y = yaw
    add_child(model)
    return model

func _shape(body: CollisionObject3D, center: Vector3, size: Vector3) -> void:
    var collision := CollisionShape3D.new()
    var shape := BoxShape3D.new()
    shape.size = size
    collision.shape = shape
    collision.position = center
    body.add_child(collision)

func _box(parent: Node3D, center: Vector3, size: Vector3) -> void:
    var body := StaticBody3D.new()
    parent.add_child(body)
    _shape(body,center,size)

func target(feet: Vector3) -> Dictionary:
    var hand := feet + Vector3.UP
    for kind in ["letter","door"]:
        var point := post.to_global(note_point) if kind == "letter" else door.to_global(Vector3(1.03,1.12,0))
        var distance := point.distance_to(hand)
        if distance < (1.48 if kind == "letter" else 1.83):
            return {"kind":kind,"distance":distance}
    return {}

func turn_door(feet: Vector3) -> bool:
    if moving and moving.is_running():
        return false
    var local_feet := post.to_local(feet) - door.position
    # Avoid sweeping the leaf through a player standing in its quarter-circle.
    if local_feet.x > -.32 and local_feet.z < .31 and Vector2(local_feet.x,local_feet.z).length() < 1.57:
        return false
    open = not open
    moving = create_tween().set_process_mode(Tween.TWEEN_PROCESS_PHYSICS)
    moving.tween_property(door,"rotation:y",1.55 if open else 0.0,.52).set_trans(Tween.TRANS_SINE)
    if DisplayServer.get_name() != "headless":
        hinge.play()
    return true

func restore_door(value: bool) -> void:
    open = value
    if moving:
        moving.kill()
    door.rotation.y = 1.55 if open else 0.0
