extends SceneTree
const Slot = preload("res://Source/campaign_slot.gd")
var failures := []
func _initialize() -> void:
	call_deferred("run")

func run() -> void:
	var path := "user://tenant-test-%d.cfg" % Time.get_ticks_usec()
	var legacy := ConfigFile.new()
	legacy.set_value("campaign","version",1)
	legacy.set_value("district","stage",7)
	legacy.set_value("district","finished",true)
	legacy.set_value("road","evidence","Дорожная улика")
	legacy.save(path)
	var saved := Slot.resume(path)
	if saved.stage != 8 or saved.finished or saved.evidence != "Дорожная улика":
		failures.append("v1 migration")
	set_meta("district_entry",saved)
	change_scene_to_file("res://Content/District.tscn")
	await scene_changed
	var district = current_scene
	district.slot_path = path
	district.avatar.position = district.TARGETS[8]
	district._physics_process(0.02)
	if "три удара" not in district.hud.text or district.knock.stream == null:
		failures.append("knock cue")
	district.interact()
	district.call_help()
	saved = Slot.resume(path)
	if saved.stage != 10 or saved.tenant != "called":
		failures.append("call checkpoint")
	set_meta("district_entry",saved)
	change_scene_to_file("res://Content/District.tscn")
	await scene_changed
	district = current_scene
	district.slot_path = path
	for stage in [10,11]:
		district.avatar.position = district.TARGETS[stage]
		district.interact()
	if not district.finished or district.survivor.visible or "копию" not in district.hud.text or "угольным" in district.logbook.text:
		failures.append("copy consequence")
	DirAccess.remove_absolute(ProjectSettings.globalize_path(path))
	await create_timer(.35).timeout
	print("Tenant checkpoint: ", "PASS" if failures.is_empty() else str(failures))
	quit(int(not failures.is_empty()))
