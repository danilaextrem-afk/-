shader_type spatial;
render_mode cull_disabled;
uniform sampler2D pigment : source_color, filter_nearest_mipmap;
uniform float sway = 0.045;

void vertex() {
    vec3 at = (MODEL_MATRIX*vec4(VERTEX,1.)).xyz;
    float crown = smoothstep(.3,7.5,VERTEX.y);
    VERTEX.x += crown*sway*(sin(at.x*.45+at.z*.29+TIME*1.07)+.39*sin(at.z*.67-TIME*.88));
}

void fragment() {
    vec4 leaf = texture(pigment,UV);
    ALBEDO = leaf.rgb;
    ALPHA = leaf.a;
    ALPHA_SCISSOR_THRESHOLD = .42;
    ROUGHNESS = 1.;
    SPECULAR = .04;
}
