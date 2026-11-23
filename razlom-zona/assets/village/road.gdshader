shader_type spatial;
render_mode cull_disabled;
uniform sampler2D ground_map : source_color, filter_nearest, repeat_disable;
uniform sampler2D sky_map : source_color, filter_linear, repeat_enable;
varying vec3 world_position;
void vertex() {
    world_position = (MODEL_MATRIX * vec4(VERTEX,1.0)).xyz;
}
void fragment() {
    vec4 soil = texture(ground_map,UV);
    float wet = soil.a;
    vec2 p = floor(world_position.xz*45.0)/45.0;
    vec3 eye = normalize(world_position-CAMERA_POSITION_WORLD);
    float ripple = sin(p.x*15.0+sin(p.y*9.0))*0.009 + sin(p.y*31.0+p.x*2.0)*0.012;
    vec3 reflected = reflect(eye,normalize(vec3(ripple,1.0,ripple*0.65)));
    vec2 sky_uv = vec2(atan(reflected.z,reflected.x)/(2.0*PI)+0.5,0.5-asin(clamp(reflected.y,-1.0,1.0))/PI);
    float fresnel = pow(1.0-clamp(-eye.y,0.0,1.0),3.0);
    ALBEDO = soil.rgb*(1.0-.52*wet);
    float cloud_signal = dot(texture(sky_map,sky_uv).rgb, vec3(0.2126, 0.7152, 0.0722));
    EMISSION = (vec3(.032,.069,.077)*cloud_signal+min(texture(sky_map,sky_uv).rgb,vec3(.72,.28,.30))*.48)*wet*(.12+.57*fresnel);
    ROUGHNESS = mix(.94,.26,wet);
    SPECULAR = mix(.11,.025,wet);
}
