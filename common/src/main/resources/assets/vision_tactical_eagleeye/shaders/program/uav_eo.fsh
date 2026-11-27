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

    // Subtle EO optical lens micro-separation at edges
    vec2 centered = uv - 0.5;
    float dist = length(centered);
    float chroma = 0.0008 * (0.3 + dist * 1.2 + deg * 3.0);
    float r = texture(DiffuseSampler, clamp(uv + vec2(chroma, 0.0), 0.001, 0.999)).r;
    float g = texture(DiffuseSampler, clamp(uv, 0.001, 0.999)).g;
    float b = texture(DiffuseSampler, clamp(uv - vec2(chroma, 0.0), 0.001, 0.999)).b;
    vec3 src = vec3(r, g, b);

    // Cool slate-blue / crisp high-clarity EO/UAV camera grade (distinct from green NVG, never darkens scene)
    float luma = dot(src, vec3(0.299, 0.587, 0.114));
    vec3 eoMono = vec3(luma * 0.86, luma * 0.97, luma * 1.12);
    vec3 col = mix(src, eoMono, 0.60);

    // Crisp clarity contrast without crushing shadows
    col = clamp((col - 0.5) * 1.08 + 0.515, 0.0, 1.0);

    // Fine CMOS digital sensor grain
    float grain = (hash12(gl_FragCoord.xy + vec2(fract(Time * 13.7) * 512.0, fract(Time * 7.3) * 512.0)) - 0.5);
    col += grain * (0.022 + deg * 0.11);

    // Subtle horizontal EO camera scanlines
    float scan = sin(gl_FragCoord.y * 1.5707963);
    col *= 1.0 - 0.032 * (0.5 + 0.5 * scan);

    // Gentle optical gimbal edge vignette (preserves full clarity across the field of view)
    float vig = smoothstep(0.98, 0.38, dist);
    col *= mix(0.80, 1.0, vig);

    fragColor = vec4(clamp(col, 0.0, 1.0), 1.0);
}
