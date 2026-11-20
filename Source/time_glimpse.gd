extends RefCounted

var remaining := 0.0
var cooldown_left := 0.0

func begin(duration: float, cooldown: float) -> bool:
	if remaining > 0 or cooldown_left > 0 or duration < 3 or duration > 5 or cooldown < 0:
		return false
	remaining = duration
	cooldown_left = duration + cooldown
	return true

func advance(seconds: float) -> void:
	if seconds > 0:
		remaining = maxf(0, remaining - seconds)
		cooldown_left = maxf(0, cooldown_left - seconds)
