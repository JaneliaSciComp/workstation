// Fragment shader for all filtering applied by volume brick.
#version 120
uniform sampler3D signalTexture;
uniform sampler3D interleavedTexture;

// Borrowed these from the ColorFrag shader.  Thanks, Christopher Bruns.
uniform vec3 channel_color[4]; // color for each channel, initially red, green, blue, gray
uniform vec4 channel_gamma; // brightness for each channel
uniform vec4 channel_min;
uniform vec4 channel_scale;
uniform int channel_count;
uniform int interleave_flag;
uniform vec4 sign_op = vec4(1, -1, 0, 0);

float getIntensity(vec4 in_color, int c)
{
    float intensity = in_color[c]; // intensity
    intensity -= channel_min[c]; // apply black level
    intensity *= channel_scale[c]; // apply white level
    intensity = clamp(intensity, 0, 1); // avoid extreme numbers in sum
    intensity = pow(intensity, channel_gamma[c]); // apply gamma correction
    return intensity;
}

vec4 getSrgb(vec3 out_color)
{
    // Final sRGB color correction, because JOGL 2.1 won't do it.
    return vec4(pow(out_color.r, 0.46), pow(out_color.g, 0.46), pow(out_color.b, 0.46),1);
}

vec4 chooseColor() 
{
    vec4 in_color = texture3D(signalTexture, gl_TexCoord[0].xyz);

    if (channel_count == 2)
    {
        if (interleave_flag == 0)
        {
            in_color.g = in_color.a;
        }
        else
        {
            // Verified: this code is used when 2 channels/separate tifs avail.
            in_color.g = texture3D(interleavedTexture, gl_TexCoord[0].xyz).r;
        }
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

vec4 subtractColor() 
{
    vec4 out_color = vec4(0,0,0,0);
    if (channel_count == 2  &&  interleave_flag == 1)
    {
        vec4 in_color_0 = texture3D(signalTexture, gl_TexCoord[0].xyz);
        vec4 in_color_1 = texture3D(interleavedTexture, gl_TexCoord[0].xyz);
        float ch0Intensity = getIntensity(in_color_0, 0);
        float ch1Intensity = getIntensity(in_color_1, 1);
        out_color = getSrgb(
            clamp(
                (ch0Intensity * channel_color[0] * sign_op[0]) + (ch1Intensity * channel_color[1] * sign_op[1]),
                0, 1)
        );
    }
    else
    {
        out_color = chooseColor();
    }
    return out_color;
}

void main()
{
    gl_FragColor = subtractColor();
}

