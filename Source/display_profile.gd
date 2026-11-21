extends Node

# In fullscreen the OS supplies the output surface. Limit the 3D pass
# separately so the HUD remains at the drawable's physical resolution.
static func scene_scale(platform: String, drawable: Vector2i) -> float:
	var short_side := mini(drawable.x, drawable.y)
	if short_side <= 0:
		return 1.0
	match platform:
		"Android": return minf(1.0,1080.0/short_side)
		"Windows": return minf(1.0,2160.0/short_side)
	return 1.0

func _ready() -> void:
	if DisplayServer.get_name() == "headless":
		return
	var platform := OS.get_name()
	if platform not in ["Android","Windows"]:
		return
	var window := get_window()
	window.size_changed.connect(apply_profile)
	apply_profile()

func apply_profile() -> void:
	var window := get_window()
	window.scaling_3d_scale = scene_scale(OS.get_name(),window.size)
