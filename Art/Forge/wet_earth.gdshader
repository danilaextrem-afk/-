shader_type spatial;
uniform sampler2D earth : source_color, filter_linear_mipmap_anisotropic, repeat_enable;
uniform sampler2D relief : hint_normal, filter_linear_mipmap_anisotropic, repeat_enable;
varying vec2 ground;
void vertex(){ground=(MODEL_MATRIX*vec4(VERTEX,1.0)).xz;}
void fragment(){
	vec2 p=ground*.34;
	vec3 soil=texture(earth,p).rgb;
	vec3 strata=texture(earth,mat2(vec2(.8,.6),vec2(-.6,.8))*p*.39).rgb;
	float rut=exp(-pow((abs(ground.x+.22*sin(ground.y*.57))-.90)*2.2,2.0));
	float hollow=.38*sin(ground.y*1.31+ground.x*.82)+.24*cos(ground.y*2.2-ground.x*1.7)+rut;
	float wet=smoothstep(.66,.90,hollow-.95*strata.r);

	ALBEDO=mix(mix(vec3(dot(soil,vec3(.30,.58,.12))),soil,.42)*(.39+strata*.35),vec3(.041,.054,.060)+soil*.10,wet*.89);
	ROUGHNESS=mix(.94,.15,wet);
	SPECULAR=mix(.22,.82,wet);
	NORMAL_MAP=texture(relief,p).rgb;
	NORMAL_MAP_DEPTH=mix(.85,.12,wet);
}
