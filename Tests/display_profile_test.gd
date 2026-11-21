extends SceneTree
const Profile = preload("res://Source/display_profile.gd")
func _initialize() -> void:
	var checks := [
		is_equal_approx(Profile.scene_scale("Windows",Vector2i(3840,2160)),1.0),
		is_equal_approx(Profile.scene_scale("Windows",Vector2i(7680,4320)),0.5),
		is_equal_approx(Profile.scene_scale("Android",Vector2i(2340,1080)),1.0),
		is_equal_approx(Profile.scene_scale("Android",Vector2i(3200,1440)),0.75),
		is_equal_approx(Profile.scene_scale("Android",Vector2i(1280,720)),1.0),
		is_equal_approx(Profile.scene_scale("Linux",Vector2i(3840,2160)),1.0)
	]
	print("Platform scaling: ",checks)
	quit(int(false in checks))
