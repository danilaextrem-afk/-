extends Node3D
## The last service left a narrow-band link from Zalesye to the G-4 gallery.
const RECEIVER := preload("res://assets/transport/field_receiver.glb")
const CALL := preload("res://assets/sound/relay_call.ogg")
const CONTACT := preload("res://assets/sound/relay_contact.ogg")
const SITES := [
    Vector3(-33.50, 1.21, -1.39), Vector3(6.55, 1.12, 3.72), Vector3(25.38, -2.83, -35.7)
]
const YAW := [-.40, -1.80, -1.57]
const CLUES := [
    "У автобуса работает приёмник",
    "Передать позывной через КПП-07",
    "Найти дежурный пост внизу Г-4",
    "Связь с Г-4 восстановлена"
]
const ACTIONS := ["Настроить приёмник", "Передать позывной", "Ответить дежурному"]
const MESSAGES := [
    "«Седьмой, ответьте». На шкале отмечена частота КПП-07.",
    "«Слышу. Ищи Г-4». Ответ идёт из-под земли, с северо-востока.",
    "«Ты первый, кого слышно с поверхности». Канал Г-4 открыт."
]

var audio_active := DisplayServer.get_name() != "headless"
var stage := 0
var beacon := AudioStreamPlayer3D.new()
var wind := AudioStreamPlayer.new()
var click := AudioStreamPlayer.new()
var call_timer := 2.0
var indicators: Array[StandardMaterial3D] = []

func _ready() -> void:
    for index in SITES.size():
        var receiver := RECEIVER.instantiate() as Node3D
        receiver.position = SITES[index]
        receiver.rotation.y = YAW[index]
        add_child(receiver)
        if index > 0:
            var post := MeshInstance3D.new()
            var pole := CylinderMesh.new()
            pole.top_radius = .052
            pole.bottom_radius = .074
            pole.height = 1.18
            pole.radial_segments = 8
            post.mesh = pole
            post.position = Vector3(0,-.59,-.06)
            var iron := StandardMaterial3D.new()
            iron.albedo_color = Color("53473c")
            post.material_override = iron
            receiver.add_child(post)
        var lamp := MeshInstance3D.new()
        var lens := BoxMesh.new()
        lens.size = Vector3(.058,.024,.013)
        lamp.mesh = lens
        lamp.position = Vector3(.192,.115,.15)
        var mat := StandardMaterial3D.new()
        mat.emission_enabled = true
        lamp.material_override = mat
        receiver.add_child(lamp)
        indicators.append(mat)
    beacon.stream = CALL
    beacon.unit_size = 4.0
    beacon.max_distance = 19
    beacon.volume_db = -4
    add_child(beacon)
    click.stream = CONTACT
    add_child(click)
    wind.stream = preload("res://assets/sound/wind_in_wire.ogg")
    wind.volume_db = -11
    add_child(wind)
    if audio_active:
        wind.play()
    restore(stage)

func restore(value: int) -> void:
    stage = clampi(value,0,SITES.size())
    beacon.stop()
    if stage < SITES.size():
        beacon.position = SITES[stage]
    for index in indicators.size():
        var color := Color("ffad44") if index == stage else Color("3c4d44")
        indicators[index].albedo_color = color
        indicators[index].emission = color
        indicators[index].emission_energy_multiplier = 1.8 if index == stage else 0.08
    call_timer = 1.4

func update_sound(delta: float, listener: Vector3) -> void:
    wind.volume_db = lerpf(-11,-29,clampf(-listener.y/4.0,0,1))
    if stage >= SITES.size():
        return
    call_timer -= delta
    if call_timer <= 0:
        call_timer = 7.7
        if audio_active and listener.distance_to(SITES[stage]) < beacon.max_distance:
            beacon.play()

func tune() -> String:
    if stage >= SITES.size():
        return ""
    var message: String = MESSAGES[stage]
    restore(stage+1)
    if audio_active:
        click.play()
    return message
