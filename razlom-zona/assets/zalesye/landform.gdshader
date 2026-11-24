shader_type spatial;
// Ground detail remains anchored in world metres; use a real raised mesh.
uniform sampler2D leaf_litter : source_color, filter_linear_mipmap_anisotropic, repeat_enable;
varying vec3 ground_world;
float hash21(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}
void vertex() {
    ground_world = (MODEL_MATRIX * vec4(VERTEX, 1.0)).xyz;
}
void fragment() {
    vec2 p = floor(ground_world.xz * 48.0) / 48.0;
    vec3 soil = texture(leaf_litter, p * 0.39).rgb;
    vec3 detail = texture(leaf_litter, p * 1.1 + vec2(0.43,0.27)).rgb;
    float cover = smoothstep(-0.50,0.55, sin(p.x * 0.51) * sin(p.y * 0.43) + 0.34 * sin(p.x * 1.17 + p.y * 0.28));
    float grain = hash21(floor(p * 48.0));
    vec3 base = mix(soil, detail, 0.29);
    vec3 humus = base * vec3(0.84, 0.77, 0.63);
    vec3 moss = base * vec3(0.78, 0.91, 0.63);
    vec3 final_color = mix(humus,moss,cover * 0.82) * (0.92 + 0.16 * grain);
    ALBEDO = final_color;
    ROUGHNESS = mix(0.91, 0.99, cover);
    SPECULAR = 0.22;
}
