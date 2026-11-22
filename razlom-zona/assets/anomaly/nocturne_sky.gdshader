shader_type sky;
// Controlled midnight/dusk grade of the existing painted cloud source.
// Original source image remains in the project; this prevents flat orange
// daylight and huge white sky streaks from overwhelming the silhouettes.
uniform sampler2D panorama : source_color, filter_linear, repeat_enable;
uniform vec3 upper_ink : source_color = vec3(0.017, 0.027, 0.052);
uniform vec3 horizon_ink : source_color = vec3(0.082, 0.142, 0.159);
uniform vec3 cloud_ink : source_color = vec3(0.11, 0.095, 0.164);
uniform float cloud_contrast : hint_range(0.0, 2.0) = 0.56;
void sky() {
    vec2 uv = vec2(atan(EYEDIR.z, EYEDIR.x) / (2.0 * PI) + 0.5,
                    0.5 - asin(clamp(EYEDIR.y, -1.0, 1.0)) / PI);
    vec3 sample_rgb = texture(panorama, uv).rgb;
    float source_luma = dot(sample_rgb, vec3(0.2126, 0.7152, 0.0722));
    float cloud_signal = smoothstep(0.065, 0.68, min(source_luma, 0.68));
    float horizon = pow(1.0 - clamp(abs(EYEDIR.y), 0.0, 1.0), 3.3);
    vec3 base = mix(upper_ink, horizon_ink, horizon);
    float cloud_mask = cloud_signal * cloud_contrast * (0.38 + horizon * 0.62);
    // The original panorama's lightning is a low-luminance haze, not
    // an enormous permanently overexposed line on the sky.
    COLOR = base + cloud_ink * cloud_mask;
}
