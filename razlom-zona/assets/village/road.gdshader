shader_type spatial;
render_mode cull_disabled;
uniform sampler2D ground_map : source_color, filter_nearest, repeat_disable;
uniform sampler2D soil_detail : source_color, filter_nearest_mipmap, repeat_enable;
uniform sampler2D sky_map : source_color, filter_linear, repeat_enable;
uniform float shoulder_start = 2.65;
uniform float shoulder_end = 4.45;
uniform bool village_pools = false;
uniform float lane_z_offset = 0.0;
varying vec3 world_position;
varying vec2 road_position;
float cell_hash(vec2 cell) {
    return fract(sin(dot(cell, vec2(127.1, 311.7))) * 43758.5453);
}
float puddle(vec2 at, vec2 center, vec2 extent, float rough_edge) {
    float distance_to_edge = length((at-center)/extent);
    return 1.0-smoothstep(.72,.99,distance_to_edge+rough_edge);
}
void vertex() {
    world_position = (MODEL_MATRIX * vec4(VERTEX, 1.0)).xyz;
    road_position = VERTEX.xz;
}
void fragment() {
    vec4 soil = texture(ground_map, UV);
    vec2 p = floor(world_position.xz * 48.0) / 48.0;
    float grain = cell_hash(floor(p * 48.0));
    vec2 lane = road_position + vec2(0.0,lane_z_offset);
    float drift = sin(lane.y*.34)*.18 + sin(lane.y*.81)*.06;
    float track_distance = abs(abs(lane.x-drift)-1.05);
    float rut = 1.0-smoothstep(.15,.36,track_distance);
    float shoulder = smoothstep(shoulder_start,shoulder_end,abs(lane.x));
    vec3 detail = texture(soil_detail,p*.85).rgb;
    vec3 dirt = mix(soil.rgb,detail,.28)*(0.94+.13*grain);
    dirt = mix(dirt,dirt*vec3(.84,.94,.78),shoulder*.42);
    float irregularity = .06*sin(lane.x*31.0+lane.y*9.0)+.04*sin(lane.y*19.0+lane.x*11.0);
    float pool = 0.0;
    if (village_pools) {
        pool = max(pool,puddle(lane,vec2(-1.12,-2.0),vec2(.36,1.7),irregularity));
        pool = max(pool,puddle(lane,vec2(1.18,-5.3),vec2(.40,2.0),irregularity));
        pool = max(pool,puddle(lane,vec2(-1.01,-9.1),vec2(.34,1.3),irregularity));
        pool = max(pool,puddle(lane,vec2(-.22,-13.0),vec2(.48,1.4),irregularity));
        pool = max(pool,puddle(lane,vec2(.9,-20.0),vec2(.40,1.8),irregularity));
        pool = max(pool,puddle(lane,vec2(-1.06,-26.0),vec2(.34,2.0),irregularity));
        pool = max(pool,puddle(lane,vec2(1.14,-35.0),vec2(.31,1.25),irregularity));
        pool = max(pool,puddle(lane,vec2(-1.24,-40.0),vec2(.35,1.5),irregularity));
    } else {
        pool = smoothstep(.74,.97,soil.a)*rut*.48;
    }
    float wet = max(pool,clamp(soil.a*.39+rut*.18,0.0,.67))*(1.0-shoulder*.82);
    vec3 eye = normalize(world_position-CAMERA_POSITION_WORLD);
    float ripple_x = sin(p.x*24.0+p.y*13.0+TIME*1.05)*.018 + sin(p.y*36.0-TIME*.79)*.009;
    float ripple_z = sin(p.y*25.0-p.x*8.0+TIME*.83)*.018;
    vec3 reflected = reflect(eye,normalize(vec3(ripple_x,1.0,ripple_z)));
    vec2 sky_uv = vec2(atan(reflected.z,reflected.x)/(2.0*PI)+.5,.5-asin(clamp(reflected.y,-1.0,1.0))/PI);
    vec3 sky = texture(sky_map,sky_uv).rgb;
    float fresnel = pow(1.0-clamp(-eye.y,0.0,1.0),2.5);
    // Shallow water retains the mud colour and approaches sky at a grazing angle.
    float water = pool*(1.0-shoulder);
    float reflection = water*(.20+.45*fresnel);
    vec3 submerged = dirt*(1.0-.26*wet-.10*rut);
    ALBEDO = mix(submerged,sky*.66,reflection*.64);
    EMISSION = sky*reflection*.23;
    ROUGHNESS = mix(.94,.29,water*.83);
    SPECULAR = mix(.16,.34,water);
}
