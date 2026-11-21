extends "res://Source/walk_scene.gd"

enum Phase { IDLE, HUNT, WINDUP, RECOVER, LOST, WON }
var phase := Phase.IDLE
var health := 3
var hits_left := 3
var stamina := 100.0
var phase_time := 0.0
var attack_delay := 0.0
var guard_left := 0.0
var torso := Node3D.new()

func _ready() -> void:
	add_child(torso)
	box(torso,Vector3(0,1.1,0),Vector3(.62,1.0,.39),"534e44").rotation.x = -.28
	box(torso,Vector3(0,1.76,-.22),Vector3(.46,.45,.41),"938879")
	for side in [-1,1]:
		box(torso,Vector3(side*.41,.91,-.22),Vector3(.19,.91,.18),"716b60").rotation.x = .45
		box(torso,Vector3(side*.19,.31,.08),Vector3(.24,.64,.26),"4c4b46")
		box(torso,Vector3(side*.12,1.79,-.436),Vector3(.065,.035,.02),"d7ad70")
	hide()

func begin(spawn: Vector3) -> void:
	position = spawn
	phase = Phase.HUNT
	health = 3
	hits_left = 3
	stamina = 100
	phase_time = 0
	attack_delay = 0
	guard_left = 0
	torso.rotation.x = 0
	show()

func fighting() -> bool:
	return phase in [Phase.HUNT,Phase.WINDUP,Phase.RECOVER]

func guard() -> void:
	if fighting() and stamina >= 20 and guard_left <= 0 and attack_delay <= 0:
		guard_left = .72
		stamina -= 20

func strike(player: Vector3) -> void:
	if not fighting() or attack_delay > 0 or guard_left > 0 or stamina < 24:
		return
	attack_delay = .5
	stamina -= 24
	if position.distance_to(player) <= 2.4:
		hits_left -= 1
		if hits_left <= 0:
			phase = Phase.WON
			hide()

func advance(delta: float, player: Vector3) -> void:
	if not fighting(): return
	attack_delay = maxf(0,attack_delay-delta)
	guard_left = maxf(0,guard_left-delta)
	if attack_delay <= 0 and guard_left <= 0:
		stamina = minf(100,stamina+delta*18)
	var gap := position.distance_to(player)
	if gap > .05:
		look_at(Vector3(player.x,position.y,player.z))
	match phase:
		Phase.HUNT:
			if gap > 1.55:
				position = position.move_toward(player,minf(delta*3.3,gap-1.55))
			else:
				phase = Phase.WINDUP
				phase_time = .8
				torso.rotation.x = -.45
		Phase.WINDUP:
			phase_time -= delta
			if phase_time <= 0:
				if gap < 2.25 and guard_left <= 0:
					health -= 1
				phase = Phase.LOST if health <= 0 else Phase.RECOVER
				phase_time = 1
				torso.rotation.x = .25
		Phase.RECOVER:
			phase_time -= delta
			if phase_time <= 0:
				phase = Phase.HUNT
				torso.rotation.x = 0
