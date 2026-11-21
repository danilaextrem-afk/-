extends SceneTree
const Slot = preload("res://Source/campaign_slot.gd")
var failed := []

func check(ok: bool, title: String) -> void:
	if not ok: failed.append(title)

func _initialize() -> void:
	call_deferred("run")

func run() -> void:
	for blade in [12,13]:
		var path := "user://forge-%s.cfg" % Time.get_ticks_usec()
		set_meta("crossing_entry",{"slot_path":path,"crossing":blade,"evidence":"улика","tenant":"called"})
		change_scene_to_file("res://Content/Crossing.tscn")
		await scene_changed
		var scene = current_scene
		scene.hero.position = scene.village.FORGE
		scene.interact()
		var g = scene.ghoul
		scene.strike_button.pressed.emit()
		check(g.hits_left == 3 and g.stamina == 76,"miss consumes stamina")
		for i in 800: scene._physics_process(1.0/60)
		check(g.phase == g.Phase.LOST and scene.action.visible,"defeat and retry")
		scene.action.pressed.emit()
		for i in 300:
			scene._physics_process(1.0/60)
			if g.phase == g.Phase.WINDUP and g.phase_time < .4: break
		scene.guard_button.pressed.emit()
		for i in 30: scene._physics_process(1.0/60)
		check(g.health == 3 and g.phase == g.Phase.RECOVER,"timed block")
		for i in 220:
			scene._physics_process(1.0/60)
			if g.fighting(): scene.strike_button.pressed.emit()
		var save := Slot.resume(path)
		check(g.phase == g.Phase.WON and save.get("crossing") == blade+2,"victory checkpoint")
		change_scene_to_file("res://Content/NightRoad.tscn")
		await scene_changed
		current_scene.slot_path = path
		current_scene.resume_district()
		await scene_changed
		check(current_scene.step == blade+2 and not current_scene.ghoul.visible and current_scene.village.witness.visible == (blade==13),"resume preserves outcome")
		DirAccess.remove_absolute(ProjectSettings.globalize_path(path))
	print("Forge: ", "PASS" if failed.is_empty() else str(failed))
	quit(int(not failed.is_empty()))
