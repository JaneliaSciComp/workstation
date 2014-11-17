#version 120

uniform sampler2D tileTexture; // uses default texture
uniform int channel_count = 3; // initialization does not work...
// vector array initialization is broken in Apple's 1.20 implementation
uniform vec3 channel_color[4]; // color for each channel, initially red, green, blue, gray
uniform vec4 channel_gamma; // brightness for each channel
uniform vec4 channel_min;
uniform vec4 channel_scale;
uniform vec4 sign_op = vec4(1, -1, 0, 0);

vec4 getSrgb(vec3 out_color)
{
    // Final sRGB color correction, because JOGL 2.1 won't do it.
    return vec4(pow(out_color.r, 0.46), pow(out_color.g, 0.46), pow(out_color.b, 0.46),1);
}

float getIntensity(vec4 in_color, int c)
{
    float intensity = in_color[c]; // intensity
    intensity -= channel_min[c]; // apply black level
    intensity *= channel_scale[c]; // apply white level
    intensity = clamp(intensity, 0, 1); // avoid extreme numbers in sum
    intensity = pow(intensity, channel_gamma[c]); // apply gamma correction
    return intensity;
}

vec4 chooseColor() 
{
    vec4 in_color = texture2D(tileTexture, gl_TexCoord[0].xy);

    if (channel_count == 2)
    {
        in_color.g = in_color.a;
    }

    vec3 out_color = vec3(0, 0, 0);
    for (int c = 0; c < channel_count; ++c) 
    {
        float i = getIntensity(in_color, c);
        // out_color += i * channel_color[c]; // apply channel color
        out_color = max(out_color, i * channel_color[c]); // apply channel color
    }

    return getSrgb(out_color);
}

vec4 combineColors(vec4 in_color_0, vec4 in_color_1)
{
    float ch0Intensity = getIntensity(in_color_0, 0);
    float ch1Intensity = getIntensity(in_color_1, 1);
    vec4 out_color = getSrgb(
        clamp(
            (ch0Intensity * channel_color[0] * sign_op[0]) + (ch1Intensity * channel_color[1] * sign_op[1]),
            0, 1)
    );
    return out_color;
}

vec4 combineColor() 
{
    vec4 out_color = vec4(0,0,0,0);
    if (channel_count == 2 )
    {
        vec4 in_color = texture2D(tileTexture, gl_TexCoord[0].xy);
        in_color.g = in_color.a;

        out_color = combineColors( in_color, in_color );
    }
    else
    {
        out_color = chooseColor();
    }
    return out_color;
}

void main()
{
    vec4 in_color = texture2D(tileTexture, gl_TexCoord[0].xy);
    gl_FragColor = combineColor();
}
