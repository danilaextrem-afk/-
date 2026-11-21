shader_type spatial;

varying vec2 ground;

float hash21(vec2 p) {
	vec3 q = fract(vec3(p.xyx) * vec3(.1031, .1030, .0973));
	q += dot(q, q.yzx + 33.33);
	return fract((q.x + q.y) * q.z);
}
float noise_at(vec2 p) {
	vec2 cell = floor(p);
	vec2 f = fract(p);
	f = f*f*(3.0-2.0*f);
	return mix(mix(hash21(cell),hash21(cell+vec2(1,0)),f.x),
		mix(hash21(cell+vec2(0,1)),hash21(cell+1.0),f.x),f.y);
}
void vertex() {
	ground = (MODEL_MATRIX * vec4(VERTEX,1.0)).xz;
}
void fragment() {
	float grit = noise_at(ground * 21.0);
	float pools = smoothstep(.47,.72,noise_at(ground*vec2(.83,.36)) + .09*grit);
	float wheel = exp(-4.0*abs(abs(mod(ground.x,6.0)-3.0)-.81));
	ALBEDO = mix(vec3(.082,.088,.102),vec3(.031,.042,.053),pools) * (.72+.43*grit-.18*wheel);
	ROUGHNESS = mix(.87,.18,pools);
	SPECULAR = mix(.32,.87,pools);
	// Warm streaks from the right-hand lamps break up on rough patches.
	float lamp_z = mod(ground.y+16.0+9.0,18.0)-9.0;
	float ripple = noise_at(ground*vec2(2.8,8.0));
	float streak = exp(-abs(ground.x-4.35-.5*(ripple-.5))*1.8-abs(lamp_z)*.22);
	EMISSION = vec3(.45,.27,.12)*streak*pools*(.30+.70*ripple);
}
