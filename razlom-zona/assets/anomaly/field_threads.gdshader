shader_type spatial;
render_mode unshaded, cull_disabled, blend_mix, depth_draw_never;
uniform vec4 field_color : source_color = vec4(0.35,0.70,0.66,1.0);
uniform float seed_phase = 0.0;
uniform float field_radius = 4.6;
varying vec3 local_pos;
void vertex() {
    local_pos = VERTEX / field_radius;
    float motion = sin(VERTEX.y*3.1+TIME*1.1+seed_phase)*sin(VERTEX.x*2.7+VERTEX.z*2.1-TIME*.7);
    VERTEX += NORMAL * motion * .065;
}
void fragment() {
    vec3 p = normalize(local_pos);
    float theta = atan(p.z,p.x);
    float lat = p.y;
    // Irregular interrupted threads, not the surface of a solid glowing bubble.
    float drift = .14*sin(theta*5.0+lat*7.0-TIME*.8+seed_phase);
    float track = sin(theta*3.0+lat*11.0+drift*5.0+TIME*.38+seed_phase);
    float thread = 1.0-smoothstep(.025,.16,abs(track));
    float segment = smoothstep(.10,.62,sin(theta*7.0-lat*6.0+TIME*.65+seed_phase));
    float upper_fade = 1.0-smoothstep(.30,.88,lat);
    float lower_fade = smoothstep(-.62,-.32,lat);
    float grazing = pow(1.0-abs(dot(normalize(NORMAL),normalize(VIEW))),2.5);
    float flicker = .80+.20*sin(TIME*2.4+theta*9.0+lat*14.0);
    float haze = pow(max(0.0,sin(theta*4.0-lat*5.0+TIME*.42)),8.0)*.028;
    float strands = thread*segment*upper_fade*lower_fade*flicker;
    ALBEDO = field_color.rgb * (.58+.36*grazing);
    EMISSION = field_color.rgb * strands * .36;
    ALPHA = clamp(strands*(.17+.19*grazing)+haze*upper_fade*lower_fade,0.0,.34);
}
