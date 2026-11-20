extends AudioStreamPlayer3D

# A short resonant impact, repeated three times by the missing tenant.
func _ready() -> void:
	var rate := 12000
	var bytes := PackedByteArray()
	bytes.resize(rate * 2)
	for i in rate:
		var t := float(i)/rate
		var value := 0.0
		for start in [0.0,0.20,0.64]:
			var age: float = t-start
			if age >= 0:
				value += exp(-25*age)*sin(TAU*310*age) + .4*exp(-34*age)*sin(TAU*793*age)
		bytes.encode_s16(i*2,roundi(12000*value))
	var sound := AudioStreamWAV.new()
	sound.mix_rate = rate
	sound.format = AudioStreamWAV.FORMAT_16_BITS
	sound.data = bytes
	stream = sound
	unit_size = 4
	max_distance = 22
