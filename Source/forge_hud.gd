extends Control

var health := 3
var stamina := 100.0
var enemy_health := 3
var guard := false

func update_fight(fight, blocking: bool) -> void:
	health = fight.health
	stamina = fight.stamina
	enemy_health = fight.enemy_health
	guard = blocking
	queue_redraw()

func _draw() -> void:
	var font := ThemeDB.fallback_font
	var ivory := Color("#eee6d6")
	var shadow := Color(0.045,0.052,0.053,.86)
	draw_rect(Rect2(0,0,295,90),shadow)
	draw_rect(Rect2(0,0,3,90),Color("#b89961"))
	draw_string(font,Vector2(18,29),"КИРИЛЛ",HORIZONTAL_ALIGNMENT_LEFT,-1,19,ivory)
	for i in 3:
		draw_rect(Rect2(18+i*87,42,82,9),Color("#a35649") if i < health else Color("#423634"))
	draw_rect(Rect2(18,65,256,4),Color("#51493b"))
	draw_rect(Rect2(18,65,256*stamina/100,4),Color("#d0b17b"))
	var right := get_viewport_rect().size.x-108
	draw_rect(Rect2(right-228,0,228,68),shadow)
	draw_string(font,Vector2(right-210,28),"УПЫРЬ",HORIZONTAL_ALIGNMENT_LEFT,-1,19,ivory)
	for i in 3:
		draw_rect(Rect2(right-210+i*65,44,59,5),Color("#c27c62") if i < enemy_health else Color("#433936"))
	if guard:
		draw_string(font,Vector2(306,64),"Блок",HORIZONTAL_ALIGNMENT_LEFT,-1,20,ivory)
