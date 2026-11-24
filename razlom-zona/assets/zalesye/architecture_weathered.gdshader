shader_type spatial;
render_mode cull_disabled;
uniform sampler2D district : source_color, filter_nearest_mipmap, repeat_disable;
uniform vec4 timber_tint : source_color = vec4(1.0);
uniform vec4 plaster_tint : source_color = vec4(1.0);
uniform float building_seed = 0.0;
varying vec3 object_pos;
varying vec3 object_normal;
float hash21(vec2 p) {
    return fract(sin(dot(p,vec2(127.1,311.7))) * 43758.5453);
}
vec3 material_tile(vec2 slot, vec2 coords) {
    vec2 pixel = vec2(1.5) + fract(coords)*125.0;
    return texture(district,(slot*128.0+pixel)/vec2(1024.0,512.0)).rgb;
}
void vertex() {
    object_pos = VERTEX;
    object_normal = NORMAL;
}
void fragment() {
    vec2 slot = floor(UV*vec2(8.0,4.0));
    float tile = slot.x + slot.y*8.0;
    vec3 base = texture(district,UV).rgb;
    vec3 n = abs(normalize(object_normal));
    vec3 p = floor(object_pos*56.0)/56.0;
    vec2 face_uv = n.x > n.z ? p.zy : p.xy;
    if (n.y > max(n.x,n.z)) {
        face_uv = p.xz;
    }
    float brick = 1.0-step(.1,abs(tile-0.0));
    float chalk = 1.0-step(.1,abs(tile-1.0));
    float wood = 1.0-step(.1,abs(tile-2.0));
    float roof = 1.0-step(.1,abs(tile-5.0));
    float stone = 1.0-step(.1,abs(tile-8.0));
    float mapped = brick+chalk+wood+roof+stone;
    // Real metre-scale material repeat, independent of the polygon dimensions.
    if (brick > .5) {
        base = material_tile(slot,face_uv*vec2(1.05,1.16));
    } else if (chalk > .5) {
        base = material_tile(slot,face_uv*.67)*plaster_tint.rgb;
    } else if (wood > .5) {
        base = material_tile(slot,face_uv*vec2(.59,1.05))*timber_tint.rgb;
    } else if (roof > .5) {
        base = material_tile(slot,p.xz*vec2(.64,.96));
    } else if (stone > .5) {
        base = material_tile(slot,face_uv*1.2);
    }
    float age = .5+.5*sin(p.x*2.01+p.z*.83+building_seed)*cos(p.y*1.2+p.z*1.34);
    float grain = hash21(floor(face_uv*56.0));
    float damp = (1.0-smoothstep(.12,.92,p.y))*(.45+.55*age)*(brick+chalk+wood+stone);
    float runoff = chalk*step(.8,hash21(vec2(floor(face_uv.x*7.0),building_seed)))*(1.0-smoothstep(1.0,4.0,p.y));
    vec3 worn = base * (1.0-.25*damp-.19*runoff) * mix(1.0,.83+.16*age+.09*grain,mapped);
    worn = mix(worn,worn*vec3(.73,.87,.73),damp*.53);
    ALBEDO = worn;
    ROUGHNESS = .92;
    SPECULAR = .18;
}
