shader_type spatial;
render_mode cull_back;
uniform sampler2D atlas : source_color, filter_nearest, repeat_disable;
uniform vec3 section_tint : source_color = vec3(0.8, 0.82, 0.79);
uniform vec3 lamp_tint : source_color = vec3(0.8, 0.94, 0.81);
uniform float lamp_power = 0.8;
uniform float section_id = 0.0;
varying vec3 bay_pos;
float h(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}
void vertex() {
    bay_pos = VERTEX;
}
void fragment() {
    vec3 base = texture(atlas, UV).rgb;
    vec2 tile = floor(UV * vec2(4.0, 2.0));
    float id = tile.x + 4.0 * tile.y;
    vec3 p = floor(bay_pos * 48.0) / 48.0;
    float height = p.y - p.z * 0.25;
    float concrete = 1.0 - step(0.1, abs(id));
    float floor_tile = 1.0 - step(0.1, abs(id - 4.0));
    float rust_tile = 1.0 - step(0.1, abs(id - 2.0));
    float lamp = 1.0 - step(0.1, abs(id - 6.0));
    float wall = step(1.35, abs(p.x)) * concrete;
    float seam = step(0.87, h(vec2(floor(p.z * 9.0), section_id)));
    float streak = wall * seam * (0.35 + 0.65 * (1.0 - smoothstep(0.35, 2.6, height)));
    float damp = wall * (1.0 - smoothstep(0.20, 0.92, height))
        * (0.65 + 0.35 * h(floor(vec2(p.z, height) * 7.0)));
    float dirt = h(floor(vec2(p.x + p.z * 0.11, p.z) * 15.0));
    float wet = floor_tile * (1.0 - smoothstep(0.12, 0.48, abs(p.x)))
        * smoothstep(0.38, 0.83, h(vec2(floor(p.z * 3.0), section_id + 7.0)));
    vec3 color = base * mix(vec3(1.0), section_tint, concrete + floor_tile);
    color *= 1.0 - 0.22 * streak - 0.26 * damp - 0.12 * floor_tile * dirt;
    color = mix(color, color * vec3(0.68, 0.83, 0.75), damp * 0.5);
    color *= 1.0 - 0.16 * wet;
    ALBEDO = mix(color, lamp_tint, lamp * 0.7);
    ROUGHNESS = mix(0.91 - 0.15 * rust_tile, 0.34, wet);
    SPECULAR = mix(0.22, 0.48, wet);
    EMISSION = lamp_tint * lamp * lamp_power;
}
