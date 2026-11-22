extends "res://Source/walk_scene.gd"

const START := Vector3(0,0,-22)
const SPAWN := Vector3(1.15,0,-26)
const MAX_STAMINA := 100.0
var enemy := Node3D.new()
var animator: AnimationPlayer
var model: Node3D
var approaching := false
var recoil := 0.0
var health := 3
var enemy_health := 3
var stamina := MAX_STAMINA
var windup := 0.0
var recovery := 0.0
var swing := 0.0
var contact_pending := false

func _ready() -> void:
	add_child(enemy)
	model = preload("res://Art/Forge/MarshGhoul.glb").instantiate()
	enemy.add_child(model)
	model.scale = Vector3.ONE*.102
	model.rotation.y = PI
	animator = model.find_child("AnimationPlayer",true,false)
	for clip in ["Walk","Idle"]:
		animator.get_animation(clip).loop_mode = Animation.LOOP_LINEAR
	reset()

func reset() -> void:
	approaching = false
	recoil = 0.0
	animator.play("Idle")
	enemy.position = SPAWN
	health = 3
	enemy_health = 3
	stamina = MAX_STAMINA
	windup = 0.0
	recovery = 0.0
	swing = 0.0
	contact_pending = false

func strike() -> void:
	if swing > 0 or stamina < 25.0 or enemy_health <= 0: return
	stamina -= 25.0
	swing = .55
	contact_pending = true

func pose() -> void:
	model.rotation.x = .14*recoil
	if windup > 0 or recovery > 0:
		# Frame 10 of 30 begins contact. Match the visual strike to the damage tick.
		var length := animator.get_animation("Attack").length
		animator.play("Attack")
		var progress := (1.0-windup/.8)/3.0 if windup > 0 else 1.0/3.0+(1.0-recovery/.95)*2.0/3.0
		animator.seek(length*clampf(progress,0.0,1.0),true)
		animator.pause()
	else:
		var clip := "Walk" if approaching else "Idle"
		if animator.current_animation != clip or not animator.is_playing():
			animator.play(clip,.14)

func advance(delta: float, hero_position: Vector3, blocking: bool) -> void:
	approaching = enemy.position.distance_to(hero_position) > 1.55 and windup <= 0
	recoil = move_toward(recoil,0,delta*4.2)
	swing = maxf(0.0,swing-delta)
	if contact_pending and swing <= .30:
		contact_pending = false
		if enemy.position.distance_to(hero_position) <= 2.25:
			enemy_health -= 1
			recoil = 1.0
			if enemy_health == 0: return
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
