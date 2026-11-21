extends SceneTree

const Slot = preload("res://Source/campaign_slot.gd")
var errors := []
func _initialize() -> void:
	call_deferred("run")

func check(ok: bool, label: String) -> void:
	if not ok: errors.append(label)

func walk(scene, target: Vector3) -> void:
	for frame in 900:
		var offset: Vector3 = target - scene.hero.position
		if offset.length() < .22: return
		var horizontal := "ui_left" if offset.x < 0 else "ui_right"
		var vertical := "ui_up" if offset.z < 0 else "ui_down"
		if absf(offset.x) > .12: Input.action_press(horizontal)
		if absf(offset.z) > .12: Input.action_press(vertical)
		scene._physics_process(1.0/60)
		Input.action_release(horizontal)
		Input.action_release(vertical)
	errors.append("unreachable waypoint")

func run() -> void:
	for escape in [false,true]:
		var path := "user://village-%s.cfg" % Time.get_ticks_usec()
		var old := ConfigFile.new()
		old.set_value("campaign","version",3)
		old.set_value("district","stage",11)
		old.set_value("district","finished",true)
		old.set_value("district","tenant","rescued")
		old.set_value("campaign","crossing",6)
		old.set_value("road","evidence","улика")
		old.save(path)
		change_scene_to_file("res://Content/NightRoad.tscn")
		await scene_changed
		current_scene.slot_path = path
		current_scene.resume_district()
		await scene_changed
		var scene = current_scene
		walk(scene,scene.FIRE)
		scene.interact()
		var v = scene.village
		check(v.visible and not scene.forest.visible,"village reached")
		walk(scene,v.ELDER)
		scene.interact()
		walk(scene,v.PRISONER if escape else v.GRANARY)
		scene.interact()
		if not escape:
			check(scene.step == v.Stage.TRACK and v.captive.visible,"evidence needs hearing")
			walk(scene,v.ELDER)
			scene.interact()
		check(not v.captive.visible and v.witness.visible != escape,"resident consequence")
		walk(scene,v.FORGE)
		scene.interact()
		check(scene.sword.visible,"blade equipped")
		change_scene_to_file("res://Content/NightRoad.tscn")
		await scene_changed
		current_scene.slot_path = path
		current_scene.resume_district()
		await scene_changed
		scene = current_scene
		check(scene.sword.visible and scene.village.witness.visible != escape and scene.evidence == "улика","persistent outcome")
		DirAccess.remove_absolute(ProjectSettings.globalize_path(path))
	print("Village routes: ", "PASS" if errors.is_empty() else str(errors))
	await create_timer(0.35).timeout
	quit(int(not errors.is_empty()))
