extends RefCounted

const PATH := "user://district_v1.cfg"

static func store_checkpoint(stage: int, evidence: String, path := PATH, finished := false, tenant := "") -> Error:
	var config := ConfigFile.new()
	config.set_value("campaign", "version", 2)
	config.set_value("district", "stage", stage)
	config.set_value("district", "finished", finished)
	config.set_value("district", "tenant", tenant)
	config.set_value("road", "evidence", evidence)
	var result := config.save(path + ".tmp")
	if result != OK:
		return result
	return DirAccess.rename_absolute(ProjectSettings.globalize_path(path + ".tmp"), ProjectSettings.globalize_path(path))

static func resume(path := PATH) -> Dictionary:
	var config := ConfigFile.new()
	if not FileAccess.file_exists(path) or config.load(path) != OK:
		return {}
	var version = config.get_value("campaign", "version", 0)
	if version not in [1,2]:
		return {}
	var stage = config.get_value("district", "stage", -1)
	var evidence = config.get_value("road", "evidence", "")
	if typeof(stage) != TYPE_INT or stage < 0 or stage > (7 if version == 1 else 11) or typeof(evidence) != TYPE_STRING:
		return {}
	var done = config.get_value("district","finished",false) == true
	if version == 1 and stage == 7 and done:
		stage = 8
		done = false
	var tenant = config.get_value("district","tenant","")
	if stage >= 10 and tenant not in ["rescued","called"]:
		return {}
	return {"stage":stage,"evidence":evidence,"finished":stage == 11 and done,"tenant":tenant}
