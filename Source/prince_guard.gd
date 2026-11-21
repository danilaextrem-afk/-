extends "res://Source/walk_scene.gd"

const Slot = preload("res://Source/campaign_slot.gd")
enum Phase { SUMMONS, ALARM, RETREAT, SAVED, FLED }
const CAPTAIN := Vector3(0,0,-3)
const BEAM := Vector3(-3,0,-11)
const GATE := Vector3(0,0,-18)
const POSTERN := Vector3(5,0,-9)
const HINTS := [
	"Ратибор требует отдать клинок. Подойди к княжескому стягу.",
	"Упырь идёт к воротам. Ударь в било слева или беги через калитку справа.",
	"Жители укрылись. У ворот опусти засов, пока упырь снаружи.",
	"Засов выдержал. Ратибор велит проводить тебя в Велеград как свидетеля.",
	"За калиткой Мира уводит тебя к реке. Стража разослала приметы беглеца."
]
var entry: Dictionary
var phase: int
var hero := Node3D.new()
var raider := preload("res://Source/forge_ghoul.gd").new()
var bar: MeshInstance3D
var board: MeshInstance3D
var hud := Label.new()
var action: Button
var keys := {}
var note := ""
var alarm := AudioStreamPlayer3D.new()
var strike_time := 0.0

func _ready() -> void:
	entry = get_tree().get_meta("guard_entry",{})
	if get_tree().has_meta("guard_entry"): get_tree().remove_meta("guard_entry")
	phase = entry.get("guard",Phase.SUMMONS)
	var village := preload("res://Source/village.gd").new()
	add_child(village)
	village.show_stage(0)
	person(self,CAPTAIN,"7f493b")
	box(self,CAPTAIN+Vector3(1,1.7,0),Vector3(.10,3.4,.10),"615339")
	box(self,CAPTAIN+Vector3(1.4,2.6,0),Vector3(.8,.94,.06),"923c34")
	box(self,CAPTAIN+Vector3(1.4,2.6,.04),Vector3(.13,.65,.04),"d4ac71")
	for side in [-1,1]:
		box(self,BEAM+Vector3(side*.92,1,0),Vector3(.16,2,.16),"5d4939")
		box(self,BEAM+Vector3(side*.55,1.64,0),Vector3(.045,.6,.05),"817254")
		box(self,GATE+Vector3(side*1.9,1,0),Vector3(.42,2,.42),"504535")
	box(self,BEAM+Vector3.UP*2,Vector3(2.1,.14,.20),"685640")
	add_child(alarm)
	alarm.position = BEAM+Vector3.UP*1.3
	alarm.unit_size = 6
	alarm.stream = make_alarm()
	board = box(self,BEAM+Vector3.UP*1.29,Vector3(1.65,.37,.12),"866544")
	bar = box(self,GATE+Vector3.UP,Vector3(3.9,.22,.26),"65513b")
	box(self,POSTERN,Vector3(1.4,.2,1),"6e5e48")
	add_child(raider)
	reset_raider()
	add_child(hero)
	person(hero,Vector3.ZERO,"566674")
	if phase == Phase.RETREAT: hero.position = BEAM
	var camera := Camera3D.new()
	hero.add_child(camera)
	camera.position = Vector3(0,5.7,7)
	camera.rotation.x = -.57
	var light := DirectionalLight3D.new()
	add_child(light)
	light.rotation_degrees = Vector3(-55,-30,0)
	var ui := CanvasLayer.new()
	add_child(ui)
	ui.add_child(hud)
	hud.position = Vector2(32,24)
	hud.add_theme_font_size_override("font_size",28)
	hud.add_theme_constant_override("outline_size",5)
	var row := HBoxContainer.new()
	ui.add_child(row)
	row.set_anchors_and_offsets_preset(Control.PRESET_BOTTOM_LEFT)
	row.position += Vector2(28,-104)
	for pair in [["ui_left","Влево"],["ui_up","Вперёд"],["ui_down","Назад"],["ui_right","Вправо"]]:
		keys[pair[0]] = control(row,pair[1])
	action = control(row,"")
	action.pressed.connect(interact)
	refresh()

func make_alarm() -> AudioStreamWAV:
	var sound := AudioStreamWAV.new()
	sound.mix_rate = 12000
	sound.format = AudioStreamWAV.FORMAT_16_BITS
	var samples := PackedByteArray()
	samples.resize(2*sound.mix_rate)
	for i in sound.mix_rate:
		var t := float(i)/sound.mix_rate
		var value := 0.0
		for start: float in [0.0,.34]:
			var age := t-start
			if age >= 0:
				value += sin(TAU*164*age)*exp(-10*age) + .55*sin(TAU*473*age)*exp(-19*age)
		samples.encode_s16(i*2,roundi(value*13000))
	sound.data = samples
	return sound

func reset_raider() -> void:
	raider.position = GATE+Vector3.FORWARD*13
	raider.rotation.y = PI

func transition(next: int) -> bool:
	if not entry.is_empty() and Slot.store_checkpoint(11,entry.evidence,entry.slot_path,true,entry.tenant,entry.crossing,4,next) != OK:
		note = "Не удалось сохранить. Повтори действие."
		return false
	phase = next
	note = ""
	return true

func prompt() -> String:
	match phase:
		Phase.SUMMONS:
			if hero.position.distance_to(CAPTAIN)<2: return "Выслушать Ратибора"
		Phase.ALARM:
			if hero.position.distance_to(BEAM)<2: return "Ударить в било"
			if hero.position.distance_to(POSTERN)<2: return "Бежать к реке"
		Phase.RETREAT:
			if hero.position.distance_to(GATE)<2: return "Опустить засов"
	return ""

func interact() -> void:
	if prompt().is_empty(): return
	match phase:
		Phase.SUMMONS: transition(Phase.ALARM)
		Phase.ALARM:
			if hero.position.distance_to(POSTERN)<2: transition(Phase.FLED)
			elif transition(Phase.RETREAT):
				strike_time = .7
				alarm.play()
		Phase.RETREAT: transition(Phase.SAVED)
	refresh()

func refresh() -> void:
	hud.text = "Стража князя · Русь, 1348\n"+str(HINTS[phase])
	if not note.is_empty(): hud.text += "\n"+note
	raider.visible = phase in [Phase.ALARM,Phase.RETREAT,Phase.SAVED]
	bar.visible = phase == Phase.SAVED
	var verb := prompt()
	action.visible = not verb.is_empty()
	action.text = verb+" [E]"

func _unhandled_key_input(event: InputEvent) -> void:
	if event is InputEventKey and event.pressed and not event.echo and event.physical_keycode == KEY_E: interact()

func _physics_process(delta: float) -> void:
	var axis := Input.get_vector("ui_left","ui_right","ui_up","ui_down")
	axis += Vector2(float(keys.ui_right.button_pressed)-float(keys.ui_left.button_pressed),float(keys.ui_down.button_pressed)-float(keys.ui_up.button_pressed))
	axis = axis.limit_length()
	hero.position += Vector3(axis.x,0,axis.y)*4.6*delta
	hero.position = hero.position.clamp(Vector3(-6,0,-19),Vector3(6,0,2))
	strike_time = maxf(0,strike_time-delta)
	board.rotation.x = strike_time*.5*sin(strike_time*22)
	if phase in [Phase.ALARM,Phase.RETREAT]:
		raider.position = raider.position.move_toward(GATE,1.15*delta)
		if raider.position.is_equal_approx(GATE) and transition(Phase.ALARM):
			reset_raider()
			hero.position = CAPTAIN
			note = "Упырь прорвался. Мира оттянула его от ворот. Пробуй ещё раз."
	refresh()
