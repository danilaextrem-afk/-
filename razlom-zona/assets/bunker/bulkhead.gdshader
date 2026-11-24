shader_type spatial;
render_mode cull_back;
uniform sampler2D atlas : source_color, filter_nearest, repeat_disable;
uniform float linked : hint_range(0.0, 1.0) = 0.0;
varying vec3 local_pos;
float hash21(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}
void vertex() {
    local_pos = VERTEX;
}
void fragment() {
    vec2 p = floor(local_pos.xy * 64.0) / 64.0;
    float x = p.x;
    float y = p.y;
    float grain = hash21(floor(p * 46.0));
    vec2 uv_tile = fract(p * 0.8);
    vec3 concrete = texture(atlas, (vec2(0.5) + uv_tile * 31.0) / vec2(128.0,64.0)).rgb;
    vec3 metal = texture(atlas, (vec2(32.5,0.5) + uv_tile * 31.0) / vec2(128.0,64.0)).rgb;
    float aperture = max(abs(x) / 1.22, abs(y + 0.18) / 1.28);
    float chamfer = (abs(x) + abs(y + 0.18)) / 2.15;
    float door = 1.0 - step(1.0, max(aperture, chamfer));
    float rim = door * smoothstep(0.82, 0.88, max(aperture, chamfer));
    float split = door * (1.0 - step(0.023, abs(x)));
    float rails = door * (1.0 - step(0.035, abs(abs(y + 0.18) - 0.72))) * (1.0 - rim);
    float panel = step(0.34, abs(x)) * (1.0 - step(1.04, abs(x))) * door * (1.0 - rim);
    float inset = panel * (1.0 - step(0.47, abs(y + 0.15)));
    float corrosion = smoothstep(0.63, 0.94, hash21(floor(p * vec2(8.0, 4.0)))) * (0.3 + 0.7 * (1.0 - smoothstep(-1.2, 0.3, y)));
    vec3 color = mix(concrete * 0.72, metal * vec3(0.72, 0.85, 0.84), door);
    color *= 0.83 + 0.17 * grain;
    color = mix(color, vec3(0.23,0.12,0.075), corrosion * 0.54);
    color = mix(color, vec3(0.10,0.14,0.15), clamp(rim * 0.65 + split + rails * 0.5 + inset * 0.27,0.0,1.0));
    float warning = (1.0 - step(0.095, abs(y + 1.25))) * door;
    float stripes = step(0.5, fract((x + y) * 4.0));
    color = mix(color, mix(vec3(0.095), vec3(0.49,0.36,0.12), stripes), warning * 0.88);
    float signal = (1.0 - step(0.17, abs(x))) * (1.0 - step(0.045, abs(y - 1.02)));
    vec3 signal_color = mix(vec3(0.63,0.25,0.08), vec3(0.18,0.67,0.38), linked);
    ALBEDO = mix(color,signal_color,signal);
    EMISSION = signal_color * signal * 0.9;
    ROUGHNESS = 0.86;
    METALLIC = door * 0.17;
}
