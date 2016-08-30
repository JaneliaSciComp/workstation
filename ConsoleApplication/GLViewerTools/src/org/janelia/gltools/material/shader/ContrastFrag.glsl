#version 330

/**
 * Fragment shader for applying constrast adjustment to intensities from a 
 * previous render pass.
 * Intensities are expected in the red channel of the input texture.
 */

// For efficiency, limit number of possible color channels
#define COLOR_VEC vec3

uniform sampler2D upstreamImage;
uniform COLOR_VEC opacityFunctionMin = COLOR_VEC(0.0);
uniform COLOR_VEC opacityFunctionMax = COLOR_VEC(1.0);
uniform COLOR_VEC opacityFunctionGamma = COLOR_VEC(1.0);
// uniform vec3 opacityFunction = vec3(
//        0.0, // min
//        1.0, // max
//        1.0); // gamma

in vec2 screenCoord; // from RenderPassVrtx shader

out vec4 fragColor;

const vec4 hue = vec4(
    120, // Pure green
    // 158, // GFP green, 509 nm, bluish
    49, // TDTomato red
    326, // magenta
    242 // blue
    );

const vec4 saturation = vec4(1, 1, 1, 1) * 0.90;

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

const vec4[4] COLOR_RAMP_RED = vec4[4]( // Red, hue=0
    // Red, Green, Blue, Intensity
    vec4(0.0, 0.0, 0.0, 0.00), // black
    vec4(1.0, 0.0, 0.0, 0.55), // red
    vec4(1.0, 0.5, 0.0, 0.80), // pale orange-red
    vec4(1.0, 0.95, 0.9, 1.00)); // white

const vec4[3] COLOR_RAMP_GREEN = vec4[3]( // Green, hue=120
    // Red, Green, Blue, Intensity
    vec4(0.0, 0.0, 0.0, 0.00), // black
    vec4(0.0, 1.0, 0.0, 0.70), // green
    vec4(0.95, 1.0, 0.9, 1.00)); // white

const vec4[4] COLOR_RAMP_BLUE = vec4[4]( // Blue, hue=240
    // Red, Green, Blue, Intensity
    vec4(0.0, 0.0, 0.0, 0.00), // black
    vec4(0.0, 0.0, 1.0, 0.38), // blue
    vec4(0.0, 0.5, 1.0, 0.78), // cyan-blue
    vec4(0.90, 0.95, 1.0, 1.00)); // white

vec3 ramp_color(in float intensity, in vec4[4] ramp) {
    int ix = 1;
    if (intensity > ramp[ix].w)
        ix += 1;
    if (intensity > ramp[ix].w)
        ix += 1;
    vec4 col1 = ramp[ix-1];
    vec4 col2 = ramp[ix];
    float alpha = (intensity - col1.w) / (col2.w - col1.w);
    alpha = clamp(alpha, 0, 1);
    return mix(col1.rgb, col2.rgb, alpha);
}

vec3 ramp_color(in float intensity, in vec4[3] ramp) {
    int ix = 1;
    if (intensity > ramp[ix].w)
        ix += 1;
    vec4 col1 = ramp[ix-1];
    vec4 col2 = ramp[ix];
    float alpha = (intensity - col1.w) / (col2.w - col1.w);
    alpha = clamp(alpha, 0, 1);
    return mix(col1.rgb, col2.rgb, alpha);
}

vec3 red_color(in float intensity) {
    return ramp_color(intensity, COLOR_RAMP_RED);
}

vec3 green_color(in float intensity) {
    return ramp_color(intensity, COLOR_RAMP_GREEN);
}

vec3 blue_color(in float intensity) {
    return ramp_color(intensity, COLOR_RAMP_BLUE);
}

void main() {
    vec4 c = texture(upstreamImage, screenCoord);

#define RGB_PASS_THROUGH
    vec3 color = c.rgb;
    float opacity = c.a;
#ifdef RGB_PASS_THROUGH
#else
    COLOR_VEC intensity = COLOR_VEC(c);
    float opacityIn = c.a;

    if (opacityIn <= 0) discard;

    // Discard required on Mac laptop
    if (all(greaterThanEqual(opacityFunctionMin, intensity)))
        discard;

    intensity -= opacityFunctionMin;
    intensity *= 1.0/(opacityFunctionMax - opacityFunctionMin);
    intensity = pow(intensity, opacityFunctionGamma);

    // float opacity = intensity * opacityIn;
    float opacity = opacityIn;

    // TODO: allow inversion of lightness spectrum

    // Use hard coded color ramps
    vec3 color = red_color(intensity.r);
    color += green_color(intensity.g);
    color += blue_color(intensity.b);

    // TODO sRGB should be last thing ever
    // color = pow(color, vec3(0.5, 0.5, 0.5));
#endif

    fragColor = vec4(color, opacity);
}
