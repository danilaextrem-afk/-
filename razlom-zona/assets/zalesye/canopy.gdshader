shader_type spatial;
render_mode cull_disabled;
uniform sampler2D pigment : source_color, filter_nearest_mipmap;
uniform float sway = 0.045;
uniform float crown_height = 7.5;
varying vec3 world_leaf;
varying float tip;
void vertex() {
    vec3 at = (MODEL_MATRIX * vec4(VERTEX,1.0)).xyz;
    float crown = smoothstep(0.15, crown_height, VERTEX.y);
    float breeze = sin(at.x*.45+at.z*.29+TIME*1.07) + .39*sin(at.z*.67-TIME*.88);
    VERTEX.x += crown*sway*breeze;
    VERTEX.z += crown*sway*.55*sin(at.x*.37+TIME*.82);
    world_leaf = at;
    tip = crown;
}
void fragment() {
    vec4 leaf = texture(pigment, UV);
    float patch = .5+.5*sin(world_leaf.x*.29+world_leaf.z*.42)*cos(world_leaf.z*.21);
    vec3 color = leaf.rgb * mix(vec3(.83,.95,.86),vec3(1.13,1.04,.87),patch);
    color *= mix(.79,1.05,tip);
    ALBEDO = color;
    ALPHA = leaf.a;
    ALPHA_SCISSOR_THRESHOLD = .42;
    ROUGHNESS = .93;
    SPECULAR = .08;
}
