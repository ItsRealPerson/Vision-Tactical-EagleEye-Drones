#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 OutSize;
uniform float Time;
uniform float Signal;

in vec2 texCoord;
out vec4 fragColor;

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

void main() {
    vec2 uv = texCoord;

    // Progressive analog RF horizontal jitter when signal degrades below 0.80
    float deg = clamp((0.80 - Signal) / 0.80, 0.0, 1.0);
    if (deg > 0.02) {
        float lineNoise = hash12(vec2(floor(uv.y * 160.0), floor(Time * 24.0)));
        if (lineNoise > 1.0 - deg * 0.35) {
            uv.x += (lineNoise - 0.5) * 0.025 * deg;
        }
    }

    // Subtle EO lens chromatic micro-separation at corners
    vec2 centered = uv - 0.5;
    float dist = length(centered);
    float chroma = 0.0012 * (0.4 + dist * 1.5 + deg * 3.0);
    float r = texture(DiffuseSampler, clamp(uv + vec2(chroma, 0.0), 0.001, 0.999)).r;
    float g = texture(DiffuseSampler, clamp(uv, 0.001, 0.999)).g;
    float b = texture(DiffuseSampler, clamp(uv - vec2(chroma, 0.0), 0.001, 0.999)).b;
    vec3 src = vec3(r, g, b);

    // Military EO/UAV low-light boost (gentler & cleaner than green NVG, preserves crisp detail)
    float luma = dot(src, vec3(0.299, 0.587, 0.114));
    float shadowBoost = mix(1.55, 1.02, smoothstep(0.03, 0.42, luma));
    vec3 lifted = clamp(src * shadowBoost + vec3(0.015, 0.022, 0.030) * (1.0 - smoothstep(0.0, 0.35, luma)), 0.0, 1.0);

    // Cool slate-blue / tactical EO monochrome desaturation (distinct from phosphor green NVG)
    float liftedLuma = dot(lifted, vec3(0.299, 0.587, 0.114));
    vec3 eoMono = vec3(liftedLuma * 0.82, liftedLuma * 0.95, liftedLuma * 1.10);
    vec3 col = mix(lifted, eoMono, 0.68);

    // Crisp tactical S-curve contrast
    col = clamp((col - 0.5) * 1.14 + 0.5, 0.0, 1.0);

    // Fine CMOS digital sensor grain
    float grain = (hash12(gl_FragCoord.xy + vec2(fract(Time * 13.7) * 512.0, fract(Time * 7.3) * 512.0)) - 0.5);
    col += grain * (0.032 + deg * 0.11);

    // Subtle horizontal EO camera scanlines
    float scan = sin(gl_FragCoord.y * 1.5707963);
    col *= 1.0 - 0.045 * (0.5 + 0.5 * scan);

    // Optical gimbal vignette
    float vig = smoothstep(0.92, 0.32, dist);
    col *= mix(0.58, 1.0, vig);

    fragColor = vec4(clamp(col, 0.0, 1.0), 1.0);
}
