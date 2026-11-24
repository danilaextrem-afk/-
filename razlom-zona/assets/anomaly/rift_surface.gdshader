shader_type spatial;
render_mode unshaded, cull_disabled, blend_mix, depth_draw_never, shadows_disabled;

// Thin fractured air rather than a luminous rectangular screen.
// The existing physical crystal and pylon meshes remain behind this layer.
uniform vec4 cold_color : source_color = vec4(0.17, 0.66, 0.64, 1.0);
uniform vec4 hot_color : source_color = vec4(0.52, 0.24, 0.60, 1.0);
uniform float intensity : hint_range(0.2, 3.0) = 1.0;

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

void fragment() {
    vec2 px = (floor(UV * vec2(96.0, 96.0)) + 0.5) / 96.0;
    float t = TIME * 0.42;
    float y = px.y;
    float spine = 0.50 + 0.09 * sin(y * 14.0 + t)
        + 0.035 * sin(y * 48.0 - t * 1.6);
    float d = abs(px.x - spine);
    float filament = 1.0 - smoothstep(0.006, 0.021, d);
    float halo = 1.0 - smoothstep(0.015, 0.17, d);
    // Unequal branching strokes follow the main crack, not the UV rectangle.
    float branch_side = sin(y * 19.0 + 1.1);
    float branch_x = spine + branch_side * (0.095 + 0.055 * sin(y * 29.0));
    float branch = (1.0 - smoothstep(0.005, 0.017, abs(px.x - branch_x)))
        * smoothstep(0.30, 0.80, abs(branch_side));
    float veil = (1.0 - smoothstep(0.035, 0.29, d))
        * (0.35 + 0.65 * pow(abs(sin(y * 36.0 + px.x * 13.0 - t)), 2.0));
    float sparks = step(0.975, hash21(floor(px * 64.0) + vec2(0.0, floor(t * 2.0))))
        * (1.0 - smoothstep(0.03, 0.25, d));
    float tips = smoothstep(0.02, 0.16, y) * (1.0 - smoothstep(0.83, 0.98, y));
    float pulse = 0.83 + 0.17 * sin(y * 20.0 - t * 2.0);
    vec3 tint = mix(cold_color.rgb, hot_color.rgb, 0.35 + 0.30 * sin(y * 11.0 + t));
    vec3 white_core = mix(tint, vec3(0.80, 0.95, 0.87), 0.58);
    ALBEDO = mix(tint, white_core, clamp(filament + branch * 0.35, 0.0, 1.0));
    EMISSION = (white_core * filament * 0.72 + tint * (branch * 0.45 + halo * 0.28 + sparks * 0.55)) * intensity * pulse;
    ALPHA = clamp((filament * 0.86 + branch * 0.51 + halo * 0.18
        + veil * 0.12 + sparks * 0.45) * tips, 0.0, 0.90);
}
