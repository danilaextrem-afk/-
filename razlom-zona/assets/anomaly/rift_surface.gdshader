shader_type spatial;
render_mode unshaded, cull_disabled, blend_mix, depth_draw_never;

// A single layered pixel-fracture plane: Godot GL Compatibility friendly.
// It is deliberately lightweight; two real lights animate the physical mesh.
uniform vec4 cold_color : source_color = vec4(0.05, 0.91, 0.84, 1.0);
uniform vec4 hot_color : source_color = vec4(0.54, 0.16, 0.93, 1.0);
uniform float intensity : hint_range(0.2, 3.0) = 1.15;

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

void fragment() {
    vec2 pixel = floor(UV * vec2(48.0, 72.0)) / vec2(48.0, 72.0);
    float motion = TIME * 0.35;
    float lightning = sin(pixel.y * 34.0 + sin(pixel.y * 14.0 + motion * 3.3) * 3.0);
    float crack = abs(pixel.x - (0.50 + sin(pixel.y * 11.4 + motion) * 0.09 + lightning * 0.055));
    float core = 1.0 - smoothstep(0.015, 0.20, crack);
    float shards = step(0.84, hash21(floor(pixel * vec2(19.0, 31.0) + vec2(motion, -motion * 0.5))));
    float vertical = smoothstep(0.0, 0.11, UV.y) * (1.0 - smoothstep(0.84, 1.0, UV.y));
    float width = smoothstep(0.0, 0.14, UV.x) * (1.0 - smoothstep(0.86, 1.0, UV.x));
    vec3 rgb = mix(cold_color.rgb, hot_color.rgb, 0.48 + 0.42 * sin(pixel.y * 21.0 + TIME * 1.3));
    float energy = (core * (1.2 + shards * 0.24) + shards * 0.12) * width * vertical;
    ALBEDO = rgb;
    EMISSION = rgb * energy * intensity * 2.1;
    ALPHA = clamp(energy * 0.57, 0.02, 0.76);
}
