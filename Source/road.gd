extends "res://Source/two_epoch_road.gd"

const Investigation = preload("res://Source/gate_investigation.gd")
var quest := Investigation.new()
var event: Dictionary = JSON.parse_string(FileAccess.get_file_as_string("res://GameData/road_glimpse.json"))
var triggered := false
var seen := false

func build_location() -> void:
	add_child(quest)
	quest.build(forest,city)

func restart() -> void:
	journal.hide()
	mount.position = Vector3.ZERO
	horse.pose(0)
	clock = Glimpse.new()
	triggered = false
	seen = false
	quest.reset()
	update_view()

func update_view() -> void:
	var active := clock.remaining > 0
	forest.visible = not active
	horse.visible = not active
	city.visible = active
	car.visible = active
	if active:
		caption.text = "2026 · Москва\n" + str(event.journal_clue)
	else:
		caption.text = "1348 · Крепостная дорога\n" + quest.objective()
	journal.text = "Журнал Кирилла"
	if seen:
		journal.text += "\n• " + str(event.journal_clue)
	if quest.stage >= Investigation.Stage.ASK_GUARD:
		journal.text += "\n• На придорожном камне высечен такой же знак."
	if quest.stage >= Investigation.Stage.PASS_GATE:
		journal.text += "\n• Стражник слышал звон после того, как сняли язык."
	if not seen:
		journal.text += "\nЗаписей пока нет."
	refresh_action()

func refresh_action() -> void:
	var prompt := quest.action_at(mount.position)
	action.visible = not prompt.is_empty()
	if action.visible:
		action.text = prompt + " [E]"

func interact() -> void:
	quest.interact(mount.position)
	update_view()

func _unhandled_key_input(input_event: InputEvent) -> void:
	if input_event is InputEventKey and input_event.physical_keycode == KEY_E and input_event.pressed and not input_event.echo:
		interact()

func _physics_process(delta: float) -> void:
	var steering := Input.get_axis("ui_left","ui_right") + float(right.button_pressed) - float(left.button_pressed)
	var half_width := 2.5 if mount.position.z < -102 else 4.5
	mount.position.x = clampf(mount.position.x + clampf(steering,-1,1)*delta*4,-half_width,half_width)
	var previous_z := mount.position.z
	if ride.button_pressed or Input.is_action_pressed("ui_up"):
		var through_gate := quest.stage >= Investigation.Stage.PASS_GATE and absf(mount.position.x) <= 2.5
		var road_end := -113.0 if through_gate else -102.0
		mount.position.z = maxf(road_end, mount.position.z - delta*9)
		if car.visible:
			car.roll(previous_z - mount.position.z)
	horse.pose(previous_z - mount.position.z)
	var active := clock.remaining > 0
	clock.advance(delta)
	if not triggered and mount.position.z <= -20:
		triggered = clock.begin(event.duration_sec,event.cooldown)
		seen = triggered
		if triggered:
			quest.glimpse_started()
	if active and clock.remaining == 0:
		quest.glimpse_ended()
	var previous_stage := quest.stage
	quest.finish_at(mount.position)
	if active != (clock.remaining > 0) or previous_stage != quest.stage:
		update_view()
	else:
		refresh_action()
