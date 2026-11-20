extends SceneTree
const Stages = preload("res://Source/gate_investigation.gd").Stage
var failures := PackedStringArray()

func check(condition: bool, description: String) -> void:
	if not condition:
		failures.append(description)

func _initialize() -> void:
	call_deferred("run")

func travel(road, key: String, frames: int) -> void:
	Input.action_press(key)
	for i in frames:
		road._physics_process(1.0/60)
	Input.action_release(key)

func run() -> void:
	var road = load("res://Content/Road.tscn").instantiate()
	root.add_child(road)
	var quest = road.quest
	road.interact()
	check(quest.stage == Stages.RIDE, "No interaction from spawn")
	travel(road,"ui_up",150)
	check(road.city.visible and not road.action.visible,"Glimpse from normal movement")
	travel(road,"ui_up",600)
	check(road.horse.visible and quest.stage == Stages.FIND_MARK,"Search after glimpse")
	check(is_equal_approx(road.mount.position.z,-102),"Closed gate stops rider")
	travel(road,"ui_right",65)
	road.interact()
	check(quest.stage == Stages.FIND_MARK,"Guard cannot skip clue")
	travel(road,"ui_left",135)
	check(road.action.visible,"Tablet button appears")
	road.action.pressed.emit()
	check(quest.stage == Stages.ASK_GUARD and not road.action.visible,"Inspect tablet once")
	travel(road,"ui_right",130)
	check(road.action.visible,"Guard button appears")
	var press := InputEventKey.new()
	press.physical_keycode = KEY_E
	press.pressed = true
	road._unhandled_key_input(press)
	check(quest.stage == Stages.PASS_GATE and quest.gate.position.y > 6,"Guard opens gate")
	travel(road,"ui_left",58)
	travel(road,"ui_up",90)
	check(quest.stage == Stages.COMPLETE,"Ride through gate to finish")
	road.restart()
	check(quest.stage == Stages.RIDE and quest.gate.position.y == 3 and not road.action.visible,"Restart restores quest")
	root.remove_child(road)
	road.free()
	print("Gate investigation: ", "PASS" if failures.is_empty() else str(failures))
	await create_timer(0.35).timeout
	quit(0 if failures.is_empty() else 1)
