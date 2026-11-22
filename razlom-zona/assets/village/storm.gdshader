shader_type sky;
uniform sampler2D panorama : source_color, filter_linear, repeat_enable;
void sky() {
    vec2 uv = vec2(atan(EYEDIR.z, EYEDIR.x)/(2.0*PI)+0.5, 0.5-asin(clamp(EYEDIR.y,-1.0,1.0))/PI);
    COLOR = texture(panorama, uv).rgb;
}
