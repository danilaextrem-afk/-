extends AudioStreamPlayer
## Original synthesized boot Foley. No external audio or network dependencies.
## Count actual travelled distance rather than input events to avoid footsteps
## while blocked by a wall, idle, falling or restoring a save.
const RATE := 22050
const SURFACES := ["mud", "forest", "concrete"]
var bank: Dictionary = {}
var distance_accum := 0.0
var step_count := 0
var surface_counts: Dictionary = {"mud": 0, "forest": 0, "concrete": 0}
var current_surface := ""
var audio_active := DisplayServer.get_name() != "headless"

func _ready() -> void:
    name = "BootFoley"
    volume_db = -10.0
    max_polyphony = 2
    for surface in SURFACES:
        var samples: Array[AudioStreamWAV] = []
        for variation in range(3):
            samples.append(_synthesize(surface, variation))
        bank[surface] = samples

func surface_at(point: Vector3) -> String:
    if point.y < -0.22 and absf(point.x - 24.0) < 2.15 and point.z < -20.0:
        return "concrete"
    if point.x >= -39.0 and point.x <= 11.0 and absf(point.z - 2.0) < 3.25:
        return "mud"
    if absf(point.x - 18.0) < 3.0 and point.z < 12.0 and point.z > -36.0:
        return "mud"
    return "forest"

func advance(travel: float, grounded: bool, running: bool, point: Vector3) -> void:
    if not grounded:
        distance_accum = 0.0
        return
    if travel <= 0.0001:
        return
    current_surface = surface_at(point)
    var spacing := 1.42 if running else 1.05
    distance_accum += minf(travel, 0.25)
    if distance_accum < spacing:
        return
    distance_accum -= spacing
    var sounds: Array = bank[current_surface]
    var variant: int = step_count % sounds.size()
    step_count += 1
    surface_counts[current_surface] += 1
    if audio_active:
        stream = sounds[variant]
        pitch_scale = 0.94 + 0.04 * float(variant)
        volume_db = -7.5 if running else -11.0
        play()

func _synthesize(surface: String, variant: int) -> AudioStreamWAV:
    var random := RandomNumberGenerator.new()
    random.seed = 81203 + SURFACES.find(surface) * 173 + variant * 31
    var length := int(RATE * (0.26 if surface == "mud" else 0.21))
    var pcm := PackedByteArray()
    pcm.resize(length * 2)
    var smooth_noise := 0.0
    var last_noise := 0.0
    for i in range(length):
        var t := float(i) / float(RATE)
        var noise := random.randf_range(-1.0, 1.0)
        smooth_noise = smooth_noise * 0.86 + noise * 0.14
        var grit := noise - last_noise
        last_noise = noise
        var attack := minf(t / 0.004, 1.0)
        var thump := sin(TAU * (74.0 + variant * 8.0) * t) * exp(-t * 32.0)
        var sound := 0.0
        if surface == "mud":
            var suction := maxf(0.0, t - 0.065)
            var release := (1.0 - exp(-suction * 65.0)) * exp(-suction * 24.0)
            sound = thump * 0.27 + smooth_noise * exp(-t * 21.0) * 0.75 + grit * release * 0.13
        elif surface == "forest":
            var crunch := pow(absf(sin(t * 213.0 + variant)), 3.0) * exp(-t * 19.0)
            sound = thump * 0.20 + grit * crunch * 0.16 + smooth_noise * exp(-t * 13.0) * 0.43
        else:
            var heel := exp(-t * 57.0)
            var echo_time := maxf(0.0, t - 0.045)
            var echo := (1.0 - exp(-echo_time * 180.0)) * exp(-echo_time * 39.0)
            sound = thump * 0.36 + grit * heel * 0.15 + smooth_noise * echo * 0.30
        var fade := clampf(float(length - i) / (RATE * 0.025), 0.0, 1.0)
        pcm.encode_s16(i * 2, int(clampf(sound * attack * fade, -0.95, 0.95) * 32767.0))
    var wav := AudioStreamWAV.new()
    wav.format = AudioStreamWAV.FORMAT_16_BITS
    wav.mix_rate = RATE
    wav.stereo = false
    wav.data = pcm
    return wav
