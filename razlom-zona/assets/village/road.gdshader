shader_type spatial;
render_mode cull_disabled;
uniform sampler2D ground_map : source_color, filter_nearest, repeat_disable;
uniform sampler2D sky_map : source_color, filter_linear, repeat_enable;
uniform float shoulder_start = 2.65;
uniform float shoulder_end = 4.45;
varying vec3 world_position;
varying vec2 road_position;

float cell_hash(vec2 cell) {
    return fract(sin(dot(cell, vec2(127.1, 311.7))) * 43758.5453);
}

void vertex() {
    world_position = (MODEL_MATRIX * vec4(VERTEX, 1.0)).xyz;
    road_position = VERTEX.xz;
}

void fragment() {
    vec4 soil = texture(ground_map, UV);
    // World-space pixel detail stays fixed while the player walks.
    vec2 p = floor(world_position.xz * 32.0) / 32.0;
    float grain = cell_hash(floor(p * 32.0));
    float patch = cell_hash(floor(p * 3.0));
    // Two irregular wheel ruts and a raised, relatively dry centre strip.
    // The street is rotated 90 degrees in the world; track coordinates
    // must follow the local plane, not world X.
    vec2 lane = floor(road_position * 32.0) / 32.0;
    float drift = sin(lane.y * 0.34) * 0.18 + sin(lane.y * 0.81) * 0.06;
    float track_distance = abs(abs(lane.x - drift) - 1.05);
    float rut = 1.0 - smoothstep(0.17, 0.39, track_distance);
    float shoulder = smoothstep(shoulder_start, shoulder_end, abs(lane.x));
    float wet = clamp(soil.a * (0.72 + 0.28 * rut) + rut * patch * 0.24, 0.0, 1.0);
    wet *= 1.0 - shoulder * 0.58;
    float grit = step(0.90, grain) * (1.0 - wet) * (0.35 + 0.65 * shoulder);
    vec3 dirt = soil.rgb * (0.84 + 0.23 * grain) * (1.0 - rut * 0.17);
    dirt = mix(dirt, dirt * vec3(0.79, 0.91, 0.72), shoulder * 0.38);
    dirt += vec3(0.065, 0.057, 0.043) * grit;

    vec3 eye = normalize(world_position - CAMERA_POSITION_WORLD);
    float ripple = (sin(p.x * 15.0 + sin(p.y * 9.0)) * 0.009
        + sin(p.y * 31.0 + p.x * 2.0) * 0.012) * (0.35 + 0.65 * wet);
    vec3 reflected = reflect(eye, normalize(vec3(ripple, 1.0, ripple * 0.65)));
    vec2 sky_uv = vec2(atan(reflected.z, reflected.x) / (2.0 * PI) + 0.5,
        0.5 - asin(clamp(reflected.y, -1.0, 1.0)) / PI);
    float fresnel = pow(1.0 - clamp(-eye.y, 0.0, 1.0), 3.0);
    vec3 sky = texture(sky_map, sky_uv).rgb;
    float cloud_signal = dot(sky, vec3(0.2126, 0.7152, 0.0722));
    ALBEDO = mix(dirt, vec3(dot(dirt,vec3(.25,.56,.19))),.46) * (1.0 - 0.37 * wet);
    // Keep puddles reflective without making the whole road self-illuminate.
    float pool = smoothstep(0.42, 0.85, wet);
    EMISSION = (vec3(0.032, 0.069, 0.077) * cloud_signal
        + sky * 0.39)
        * pool * (0.18 + 0.72 * fresnel);
    ROUGHNESS = mix(0.96, 0.24, pool);
    SPECULAR = mix(0.12, 0.035, pool);
}
