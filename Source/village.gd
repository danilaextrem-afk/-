extends "res://Source/walk_scene.gd"

# Values follow the camp checkpoint (6) in the campaign save.
enum Stage { ELDER = 7, DECIDE, TRACK, SMITH_ESCAPED, SMITH_CLEARED, BLADE_ESCAPED, BLADE_CLEARED, SAFE_ESCAPED, SAFE_CLEARED, SNOW_ESCAPED, SNOW_CLEARED, CLOTH_ESCAPED, CLOTH_CLEARED, HEARING_ESCAPED, HEARING_CLEARED, SPARED_ESCAPED, SPARED_CLEARED, CONDEMNED_ESCAPED, CONDEMNED_CLEARED }
const ELDER := Vector3(0,0,-5)
const PRISONER := Vector3(-4,0,-12)
const GRANARY := Vector3(4,0,-14)
const FORGE := Vector3(1,0,-23)
const SNOW := Vector3(-3,0,-18)
const LOOM := Vector3(3,0,-9)
var weaver := Node3D.new()
var snow_case := Node3D.new()
var captive := Node3D.new()
var shutter := Node3D.new()
var witness := Node3D.new()

func _ready() -> void:
	box(self,Vector3(0,-.25,-13),Vector3(28,.5,44),"73644b")
	for i in range(6):
		var x := -9 if i%2 == 0 else 9
		var z := -5.0-floori(i/2.0)*8
		box(self,Vector3(x,1.5,z),Vector3(5,3,5),"70503a")
		for side in [-1,1]:
			var roof := box(self,Vector3(x+side*1.45,3.55,z),Vector3(3.3,.28,5.6),"443d33")
			roof.rotation.z = -side*.34
		box(self,Vector3(x,1.3,z+2.52),Vector3(1.2,2.6,.1),"3f3029")
	person(self,ELDER,"746344")
	add_child(captive)
	person(captive,PRISONER,"56655e")
	add_child(shutter)
	for i in 5:
		box(shutter,PRISONER+Vector3(-.8+i*.4,1.1,.65),Vector3(.13,2.2,.16),"4f3f32")
	add_child(witness)
	person(witness,FORGE+Vector3.LEFT*2,"56655e")
	person(self,FORGE,"573e37")
	box(self,FORGE+Vector3(1,.7,0),Vector3(1.4,.5,.7),"42434a")
	box(self,GRANARY+Vector3.UP*.2,Vector3(1.8,.4,1.1),"705438")
	for i in 3:
		box(self,GRANARY+Vector3(i*.2-.3,.415,0),Vector3(.05,.02,.65),"312822").rotation.y = -.3

	add_child(snow_case)
	box(snow_case,SNOW,Vector3(2.4,.07,4),"cad0cd")
	for i in 5:
		var print_pos := SNOW + Vector3(.22 if i%2 else -.22,.05,1.4-i*.64)
		box(snow_case,print_pos,Vector3(.18,.04,.32 if i<2 else .21),"705a50")
		if i >= 2:
			for toe in [-1,0,1]:
				box(snow_case,print_pos+Vector3(toe*.08,0,-.17),Vector3(.055,.04,.09),"715d54")
	box(snow_case,SNOW+Vector3(.5,.05,.9),Vector3(.29,.03,.16),"864842")
	box(snow_case,LOOM+Vector3.UP*.7,Vector3(1.5,.12,.75),"80694a")
	for end in [-.6,.6]:
		box(snow_case,LOOM+Vector3(end,.36,0),Vector3(.12,.72,.55),"64513d")
	box(snow_case,LOOM+Vector3.UP*.8,Vector3(.95,.06,.53),"526b7a")
	for stitch in 5:
		box(snow_case,LOOM+Vector3(stitch*.15-.30,.84,0),Vector3(.045,.02,.51),"c6ac70")
	snow_case.add_child(weaver)
	person(weaver,Vector3.ZERO,"526b7a")

func show_stage(stage: int) -> void:
	weaver.visible = stage < Stage.CONDEMNED_ESCAPED
	weaver.position = LOOM+Vector3.LEFT if stage < Stage.HEARING_ESCAPED else ELDER+Vector3.RIGHT*1.4
	snow_case.visible = stage >= Stage.SNOW_ESCAPED
	captive.visible = stage in [Stage.ELDER,Stage.DECIDE,Stage.TRACK]
	shutter.visible = captive.visible
	witness.visible = stage in [Stage.SMITH_CLEARED,Stage.BLADE_CLEARED,Stage.SAFE_CLEARED] or (stage >= Stage.SNOW_ESCAPED and stage % 2 == 1)

func hint(stage: int) -> String:
	match stage:
		Stage.SAFE_ESCAPED,Stage.SAFE_CLEARED: return "Первый клинок · Упырь повержен. Кузнец зовёт: у околицы нашли кровь на снегу."
		Stage.SNOW_ESCAPED,Stage.SNOW_CLEARED: return "Кровь на снегу · Осмотри проталину слева по дороге от кузницы. Жители винят ткача."
		Stage.CLOTH_ESCAPED,Stage.CLOTH_CLEARED: return "В одном следе отпечатался каблук, в следующем — когти. На насте синяя нитка с золотым утком. Проверь ткань справа у ткацкого стола."
		Stage.HEARING_ESCAPED,Stage.HEARING_CLEARED: return "Край полотна совпал с лоскутом. Ткач признаёт: ночью он обернулся, а утром перевязал раненого. Вернись к старосте решить его судьбу."
		Stage.SPARED_ESCAPED,Stage.SPARED_CLEARED: return "Кровь на снегу · Ткач останется под присмотром Миры. Раненый получит помощь."
		Stage.CONDEMNED_ESCAPED,Stage.CONDEMNED_CLEARED: return "Кровь на снегу · Кирилл передал улики старосте и отошёл. На рассвете ткача казнили."
		Stage.ELDER: return "Мира привела тебя в селение. Староста ждёт на дороге перед избами."
		Stage.DECIDE: return "Староста: «Засов разнесло, зерно пропало. А кроме Богдашки тут никого».\nСлева — клеть с пленником. Справа — выломанный порог амбара."
		Stage.TRACK: return "Три борозды идут изнутри. Щепа лежит снаружи. Отнеси обломок старосте."
		Stage.SMITH_ESCAPED: return "Богдашка скрылся в чаще. Староста захлопнул дверь. Мира ждёт тебя у кузнеца."
		Stage.SMITH_CLEARED: return "Пленника отпустили. Он замолвил за тебя слово у кузнеца, в конце улицы."
		Stage.BLADE_ESCAPED: return "Кузнец дал Мире под поручительство затупленный клинок. Староста помнит побег."
	return "Кузнец одолжил учебный клинок. Богдашка остался в селении и готов поручиться за тебя."

func next_step(stage: int, pos: Vector3) -> int:
	match stage:
		Stage.SAFE_ESCAPED,Stage.SAFE_CLEARED:
			if pos.distance_to(FORGE)<2.6: return stage+2
		Stage.SNOW_ESCAPED,Stage.SNOW_CLEARED:
			if pos.distance_to(SNOW)<2.6: return stage+2
		Stage.CLOTH_ESCAPED,Stage.CLOTH_CLEARED:
			if pos.distance_to(LOOM)<2.6: return stage+2
		Stage.HEARING_ESCAPED,Stage.HEARING_CLEARED:
			if pos.distance_to(ELDER)<2.6: return stage+2
		Stage.ELDER:
			if pos.distance_to(ELDER)<2.6: return Stage.DECIDE
		Stage.DECIDE:
			if pos.distance_to(PRISONER)<2.6: return Stage.SMITH_ESCAPED
			if pos.distance_to(GRANARY)<2.6: return Stage.TRACK
		Stage.TRACK:
			if pos.distance_to(ELDER)<2.6: return Stage.SMITH_CLEARED
		Stage.SMITH_ESCAPED,Stage.SMITH_CLEARED:
			if pos.distance_to(FORGE)<2.6: return stage+2
	return stage

func prompt(stage: int, pos: Vector3) -> String:
	var next := next_step(stage,pos)
	if next == stage: return ""
	match next:
		Stage.SNOW_ESCAPED,Stage.SNOW_CLEARED: return "Расспросить о нападении"
		Stage.CLOTH_ESCAPED,Stage.CLOTH_CLEARED: return "Изучить цепочку следов"
		Stage.HEARING_ESCAPED,Stage.HEARING_CLEARED: return "Сравнить лоскут и выслушать ткача"
		Stage.SPARED_ESCAPED,Stage.SPARED_CLEARED: return "Поручиться за ткача"
		Stage.DECIDE: return "Выслушать старосту"
		Stage.SMITH_ESCAPED: return "Отпереть клеть и выпустить пленника"
		Stage.TRACK: return "Осмотреть сломанный порог"
		Stage.SMITH_CLEARED: return "Предъявить обломок"
	return "Взять учебный клинок"
