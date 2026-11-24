extends SceneTree
# Headless regression for the Zalesye street-light modulation.
const Street = preload("res://scripts/crimson_street.gd")

func _initialize() -> void:
    verify.call_deferred()

func verify() -> void:
    var street = Street.new()
    root.add_child(street)
    # Avoid depending on GPU screenshots: verify bounded, changing light output.
    var a: OmniLight3D = street.unstable_lamps[0]
    var b: OmniLight3D = street.unstable_lamps[1]
    street.set_process(false)
    var low := 100.0
    var high := 0.0
    for step in 240:
        street._process(0.05)
        low = minf(low, a.light_energy)
        high = maxf(high, a.light_energy)
        if a.light_energy < 0.85 or a.light_energy > 3.01:
            push_error("Zalesye lamp energy outside safe envelope")
            quit(1)
            return
    if high - low < 0.1 or absf(a.light_energy - b.light_energy) < 0.0001:
        push_error("Zalesye lamp flicker is static or identical")
        quit(1)
        return
    print("ZALESYE_LAMP_SMOKE_OK")
    quit(0)
