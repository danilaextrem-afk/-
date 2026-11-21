shader_type spatial;
varying vec3 wall_position;
varying vec3 face;
varying float concrete;

float hash_at(vec2 p) {
	vec3 q = fract(vec3(p.xyx)*vec3(.1031,.1137,.0973));
	q += dot(q,q.yzx+33.3);
	return fract((q.x+q.y)*q.z);
}
float mottling(vec2 p) {
	vec2 i = floor(p), f = fract(p);
	f = f*f*(3.0-2.0*f);
	return mix(mix(hash_at(i),hash_at(i+vec2(1,0)),f.x),
		mix(hash_at(i+vec2(0,1)),hash_at(i+1.0),f.x),f.y);
}
void vertex() {
	wall_position = (MODEL_MATRIX*vec4(VERTEX,1.0)).xyz;
	face = MODEL_NORMAL_MATRIX*NORMAL;
	concrete = INSTANCE_CUSTOM.r;
}
void fragment() {
	vec3 paint = COLOR.rgb;
	if (concrete > .5) {
		vec2 wall = vec2(abs(face.x)>abs(face.z)?wall_position.z:wall_position.x,wall_position.y);
		vec2 panel = wall / vec2(2.83,2.65);
		vec2 edge = min(fract(panel),1.0-fract(panel))*vec2(2.83,2.65);
		float seam = min(edge.x,edge.y);
		float joint = smoothstep(.012,.028+fwidth(seam),seam);
		float runoff = mottling(wall*vec2(2.6,.24));
		float mineral = mottling(wall*3.1);
		float patch = .85+.19*hash_at(floor(panel));
		paint *= (.71+.21*runoff+.15*mineral)*patch*mix(.58,1.,joint);
		paint *= mix(vec3(.64,.65,.59),vec3(1.),smoothstep(.18,2.7,wall.y+.45*runoff));
	}
	ALBEDO = paint;
	ROUGHNESS = .92;
}
