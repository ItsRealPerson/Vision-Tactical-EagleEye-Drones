#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D ThermalMask;
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

    vec2 centered = uv - 0.5;
    float dist = length(centered);
    vec3 src = texture(DiffuseSampler, clamp(uv, 0.001, 0.999)).rgb;

    // True 94% military EO-IR monochrome (MQ-9 / EagleEye cold slate grayscale, deep blacks, crisp contrast)
    float luma = dot(src, vec3(0.299, 0.587, 0.114));
    // High-contrast tactical S-curve on luminance
    float contrastLuma = clamp((luma - 0.46) * 1.26 + 0.48, 0.0, 1.0);
    vec3 eoMono = vec3(contrastLuma * 0.90, contrastLuma * 0.96, contrastLuma * 1.05);
    vec3 col = mix(src, eoMono, 0.94);

    // EO-IR White-Hot Thermal Signature Overlay from ThermalMask (highlights warm-blooded living entities)
    vec4 mask = texture(ThermalMask, clamp(uv, 0.001, 0.999));
    if (mask.a > 0.05) {
        float heat = clamp(max(mask.r, max(mask.g, mask.b)), 0.0, 1.0);
        float hot = smoothstep(0.08, 0.85, heat);
        vec3 whiteHot = mix(vec3(0.76, 0.84, 0.92), vec3(0.98, 0.99, 1.0), hot);
        col = mix(col, whiteHot, clamp(mask.a * (0.68 + 0.32 * hot), 0.0, 0.96));
    }

    // Fine CMOS digital sensor grain
    float grain = (hash12(gl_FragCoord.xy + vec2(fract(Time * 13.7) * 512.0, fract(Time * 7.3) * 512.0)) - 0.5);
    col += grain * (0.024 + deg * 0.11);

    // Subtle horizontal EO-IR camera scanlines
    float scan = sin(gl_FragCoord.y * 1.5707963);
    col *= 1.0 - 0.036 * (0.5 + 0.5 * scan);

    // Clean optical gimbal edge vignette
    float vig = smoothstep(0.98, 0.38, dist);
    col *= mix(0.76, 1.0, vig);

    fragColor = vec4(clamp(col, 0.0, 1.0), 1.0);
}
