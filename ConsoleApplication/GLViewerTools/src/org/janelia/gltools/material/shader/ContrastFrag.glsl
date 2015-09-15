#version 330

/**
 * Fragment shader for applying constrast adjustment to intensities from a 
 * previous render pass.
 * Intensities are expected in the red channel of the input texture.
 */

uniform sampler2D upstreamImage;
uniform vec3 opacityFunction = vec3(
        0.0, // min
        1.0, // max
        1.0); // gamma

in vec2 screenCoord; // from RenderPassVrtx shader

out vec4 fragColor;

const vec4 hue = vec4(
    120, // cyan/blue?
    // 158, // GFP green, 509 nm
    49, // TDTomato red
    326, // magenta
    242 // blue
    );

const vec4 saturation = vec4(1, 1, 1, 1) * 0.95;

vec3 hslToRgb(vec3 hsl) 
{
    float H = hsl.x;
    float S = hsl.y;
    float L = hsl.z;

    float C = (1.0 - abs(2*L - 1.0)) * S; // chroma
    float HPrime = H / 60.0;
    float X = C * (1.0 - abs(mod(HPrime, 2.0) - 1.0));

    vec3 rgb;
    if (HPrime < 1) rgb = vec3(C, X, 0);
    else if (HPrime < 2) rgb = vec3(X, C, 0);
    else if (HPrime < 3) rgb = vec3(0, C, X);
    else if (HPrime < 4) rgb = vec3(0, X, C);
    else if (HPrime < 4) rgb = vec3(X, 0, C);
    else rgb = vec3(C, 0, X);

    float lightnessMatch = L - 0.5 * C;
    rgb += vec3(1,1,1) * lightnessMatch;

    return rgb;
}

void main() {
    vec4 c = texture(upstreamImage, screenCoord);
    float intensity = c.r;
    float opacityIn = c.a;

    if (opacityIn <= 0) discard;

    // Discard required on Mac laptop
    if (intensity <= opacityFunction.x) discard;

    intensity -= opacityFunction.x;
    intensity *= 1.0/(opacityFunction.y - opacityFunction.x);
    intensity = pow(intensity, opacityFunction.z);

    // float opacity = intensity * opacityIn;
    float opacity = opacityIn;

    // HSL approach
    if (false) {
        vec3 hsl = vec3(hue.x, saturation.x, intensity);
        vec3 rgb = hslToRgb(hsl);
        fragColor = vec4(rgb, opacity);
        return;
    }

    // Hard code a color ramp
    // TODO - make ramp user adjustable
    const bool applyColorMap = true;
    vec3 color = vec3(1,1,1); // white
    if (applyColorMap) {
        vec3 color1 = vec3(0, 0.2, 0.05); // green
        vec3 color2 = vec3(0, 0.9, 0); // green/cyan
        vec3 color3 = vec3(1,1,0.80); // white-ish
        if (intensity < 0.5)
            color = mix(color1, color2, 2*intensity);
        else
            color = mix(color2, color3, 2*intensity-1);
    }

    // TODO sRGB should be last thing ever
    // color = pow(color, vec3(0.5, 0.5, 0.5));

    fragColor = vec4(color, opacity);
}
