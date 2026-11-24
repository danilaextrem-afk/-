extends SceneTree

# Run with Godot 4: godot --headless --path razlom-zona --script res://tests/zalesye_bulb_smoke.gd
func _initialize() -> void:
    var street_script := load("res://scripts/crimson_street.gd")
    var street := Node3D.new()
    street.set_script(street_script)
    root.add_child(street)
    call_deferred("_check", street)

func _check(street: Node3D) -> void:
    if street.unstable_lamps.size() != 2 or street.lamp_filaments.size() != 2:
        push_error("Expected two unstable lamps and two matching bulb meshes")
        quit(1)
        return
    var bulbs := street.find_children("*", "MeshInstance3D", true, false)
    var emissive_count := 0
    for bulb in bulbs:
        var mat := bulb.material_override as StandardMaterial3D
        if mat != null and mat.emission_enabled:
            emissive_count += 1
    if emissive_count != 4:
        push_error("Expected exactly four emissive fixtures, got %d" % emissive_count)
        quit(1)
        return
    street._process(0.3)
    var energy := (street.lamp_filaments[0].material_override as StandardMaterial3D).emission_energy_multiplier
    if absf(energy - (0.8 + street.unstable_lamps[0].light_energy * 0.9)) > 0.001:
        push_error("Filament brightness must follow its light energy")
        quit(1)
        return
    print("Zalesye bulb smoke OK")
    quit(0)
