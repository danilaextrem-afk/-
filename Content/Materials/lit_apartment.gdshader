shader_type spatial;
render_mode unshaded;
varying vec3 room;
void vertex() {
	room = (MODEL_MATRIX*vec4(0.,0.,0.,1.)).xyz;
}
void fragment() {
	float seed = fract(sin(dot(room,vec3(2.42,.73,1.65)))*437.24);
	float split = .16+.23*seed;
	float curtain = 1.-smoothstep(split-.025,split+.025,UV.x);
	float fold = .65+.25*cos(65.*UV.x);
	float shade = mix(1.,fold*.65,curtain);
	shade *= mix(.7,1.,smoothstep(.0,.58,UV.y));
	ALBEDO = COLOR.rgb*shade;
}
