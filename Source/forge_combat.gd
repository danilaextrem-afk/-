extends "res://Source/walk_scene.gd"

const START := Vector3(0,0,-22)
const SPAWN := Vector3(1.15,0,-26)
const MAX_STAMINA := 100.0
var enemy := Node3D.new()
var arms: Array[MeshInstance3D] = []
var health := 3
var enemy_health := 3
var stamina := MAX_STAMINA
var windup := 0.0
var recovery := 0.0
var swing := 0.0

func _ready() -> void:
	add_child(enemy)
	enemy.scale = Vector3.ONE * 1.12
	var torso := MeshInstance3D.new()
	torso.mesh = preload("res://Art/Forge/ghoul.obj")
	enemy.add_child(torso)
	for side in [-1,1]:
		var arm := MeshInstance3D.new()
		arm.mesh = preload("res://Art/Forge/left_claw.obj") if side < 0 else preload("res://Art/Forge/right_claw.obj")
		enemy.add_child(arm)
		arm.position = Vector3(side*.32,1.55,-.04)
		arms.append(arm)
	reset()

func reset() -> void:
	enemy.position = SPAWN
	health = 3
	enemy_health = 3
	stamina = MAX_STAMINA
	windup = 0.0
	recovery = 0.0
	swing = 0.0

func strike(hero_position: Vector3) -> void:
	if swing > 0 or stamina < 25.0 or enemy_health <= 0: return
	stamina -= 25.0
	swing = .55
	if hero_position.distance_to(enemy.position) <= 2.25:
		enemy_health -= 1

func pose() -> void:
	for arm in arms:
		arm.rotation.x = 1.8 * (1.0 - windup / .8) + .45 if windup > 0 else -.45 * recovery

func advance(delta: float, hero_position: Vector3, blocking: bool) -> void:
	swing = maxf(0.0,swing-delta)
	stamina = minf(MAX_STAMINA,stamina+delta*(5.0 if blocking else 24.0))
	if enemy.position.distance_to(hero_position) > .01:
		enemy.look_at(hero_position)
	if windup > 0:
		windup = maxf(0.0,windup-delta)
		if windup == 0:
			recovery = .95
			if enemy.position.distance_to(hero_position) <= 1.95:
				if blocking and stamina >= 18.0:
					stamina -= 18.0
				else:
					health -= 1
		return
	recovery = maxf(0.0,recovery-delta)
	if enemy.position.distance_to(hero_position) > 1.55:
		enemy.position = enemy.position.move_toward(hero_position,delta*2.6)
	elif recovery == 0:
		windup = .8

func status() -> String:
	var warning := "Упырь замахнулся!" if windup > 0 else "Держи дистанцию или бей после замаха."
	return "Кирилл %d/3   Выносливость %d/100   Упырь %d/3\n%s" % [health,int(stamina),enemy_health,warning]
