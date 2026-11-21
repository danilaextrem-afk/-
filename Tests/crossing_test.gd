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
	for choice in ["called","rescued"]:
		var path := "user://crossing-%s-%s.cfg" % [choice,Time.get_ticks_usec()]
		var old := ConfigFile.new()
		old.set_value("campaign","version",2)
		old.set_value("district","stage",11)
		old.set_value("district","finished",true)
		old.set_value("district","tenant",choice)
		old.set_value("road","evidence","улика")
		old.save(path)
		change_scene_to_file("res://Content/NightRoad.tscn")
		await scene_changed
		current_scene.slot_path = path
		current_scene.resume_district()
		await scene_changed
		var district = current_scene
		district.avatar.position = district.TARGETS[11]
		district.interact()
		await scene_changed
		var scene = current_scene
		walk(scene,scene.HATCH)
		if choice == "called":
			check(not scene.action.visible,"hatch requires map")
			walk(scene,scene.GATE)
			scene.interact()
			walk(scene,scene.FUSE)
		scene.interact()
		check(scene.cellar.visible,"enter cellar")
		walk(scene,scene.BELL)
		scene.interact()
		scene._physics_process(3.9)
		check(scene.step == scene.Step.GLIMPSE and Slot.resume(path).crossing == 2,"temporary vision checkpoint")
		scene._physics_process(.12)
		check(scene.step == scene.Step.FOREST and Slot.resume(path).crossing == 4,"full travel")
		change_scene_to_file("res://Content/NightRoad.tscn")
		await scene_changed
		current_scene.slot_path = path
		current_scene.resume_district()
		await scene_changed
		scene = current_scene
		check(scene.step == scene.Step.FOREST and scene.forest.visible and scene.tenant == choice,"restore 1348")
		walk(scene,Vector3(3,0,-10))
		for i in 65:
			Input.action_press("ui_up")
			scene._physics_process(1.0/60)
		Input.action_release("ui_up")
		check(scene.hero.position.z > -9,"false path loop")
		walk(scene,scene.GUIDE)
		scene.interact()
		walk(scene,scene.FIRE)
		scene.interact()
		check(Slot.resume(path).crossing == scene.Step.CAMP,"guide and camp")
		DirAccess.remove_absolute(ProjectSettings.globalize_path(path))
	print("Crossing routes: ", "PASS" if errors.is_empty() else str(errors))
	current_scene.queue_free()
	await create_timer(0.35).timeout
	quit(int(not errors.is_empty()))
