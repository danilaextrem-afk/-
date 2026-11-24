shader_type spatial;
render_mode cull_disabled;

uniform sampler2D clouds : source_color, filter_linear, repeat_enable;
varying vec3 world;

void vertex() {
    world = (MODEL_MATRIX * vec4(VERTEX, 1.0)).xyz;
}

void fragment() {
    vec2 p = floor(world.xz * 40.0) * .025;
    float ripple = sin(p.x * 13.0 + p.y * 6.0 + TIME * 1.2) * .009
        + sin(p.y * 23.0 - TIME) * .007;
    vec3 ray = reflect(normalize(world - CAMERA_POSITION_WORLD),normalize(vec3(ripple,1.,-.55*ripple)));
    vec2 uv = vec2(atan(ray.z,ray.x)/(2.0*PI)+.5,.5-asin(clamp(ray.y,-1.,1.))/PI);
    vec3 sky = texture(clouds,uv).rgb;
    // Low horizon occlusion in a shallow ditch, shared with the sky's colour space.
    float bank = .06 + .04*sin(uv.x*187.) + .033*sin(uv.x*317.);
    float open_sky = smoothstep(bank,bank+.042,ray.y);
    ALBEDO = vec3(.053,.061,.061);
    EMISSION = sky * mix(.11,.59,open_sky) * (1.-.62*clamp(ray.y,0.,1.));
    SPECULAR = .08;
    ROUGHNESS = .48;
}
