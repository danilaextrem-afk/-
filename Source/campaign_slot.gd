extends RefCounted

const GROVE_START := 26
const GROVE_END := 30
const PATH := "user://district_v1.cfg"

static func store_checkpoint(stage: int, evidence: String, path := PATH, finished := false, tenant := "", crossing := -1, hunt := -1, guard := -1) -> Error:
	var config := ConfigFile.new()
	config.set_value("campaign", "version", 9)
	config.set_value("district", "stage", stage)
	config.set_value("district", "finished", finished)
	config.set_value("district", "tenant", tenant)
	config.set_value("campaign","crossing",crossing)
	config.set_value("campaign","hunt",hunt)
	config.set_value("campaign","guard",guard)
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
	if version not in [1,2,3,4,5,6,7,8,9]:
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
	var crossing = config.get_value("campaign","crossing",-1) if version >= 3 else -1
	var last_step: int = {3:6,4:13,5:15,6:25,7:33,8:33,9:33}.get(version,6)
	if typeof(crossing) != TYPE_INT or crossing < -1 or crossing > last_step or (crossing >= 0 and (stage != 11 or not done)):
		return {}
	var hunt = config.get_value("campaign","hunt",-1) if version >= 8 else -1
	if typeof(hunt) != TYPE_INT or hunt < -1 or hunt > 4 or (hunt >= 0 and crossing < GROVE_END): return {}
	var guard = config.get_value("campaign","guard",-1) if version >= 9 else -1
	if typeof(guard) != TYPE_INT or guard < -1 or guard > 4 or (guard >= 0 and hunt != 4): return {}
	return {"guard":guard,"hunt":hunt,"crossing":crossing,"stage":stage,"evidence":evidence,"finished":stage == 11 and done,"tenant":tenant}
