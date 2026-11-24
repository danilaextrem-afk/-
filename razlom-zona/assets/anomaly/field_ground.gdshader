shader_type spatial;
render_mode unshaded, cull_disabled, blend_mix, depth_draw_never;
uniform vec4 field_color : source_color;
uniform float seed_phase = 0.0;
void fragment() {
    vec2 p = (UV-.5)*2.0;
    float theta = atan(p.y,p.x);
    float r = length(p);
    float warp = .025*sin(theta*7.0+seed_phase)+.019*sin(theta*13.0-seed_phase);
    float edge = 1.0-smoothstep(.016,.048,abs(r-.80-warp));
    float pulse = .62+.38*sin(TIME*1.6+theta*4.0);
    float broken = smoothstep(-.48,.58,sin(theta*11.0+seed_phase)+.34*sin(theta*23.0));
    float inner = 1.0-smoothstep(.03,.21,abs(r-.69-warp));
    float fade = 1.0-smoothstep(.78,.98,r);
    float ash = pow(max(0.0,sin(p.x*32.0+p.y*27.0+sin(theta*6.0)*2.0)),10.0);
    ALBEDO = field_color.rgb*.73;
    ALPHA = clamp(edge*broken*pulse*.22+inner*ash*fade*.035,0.0,.23);
}
