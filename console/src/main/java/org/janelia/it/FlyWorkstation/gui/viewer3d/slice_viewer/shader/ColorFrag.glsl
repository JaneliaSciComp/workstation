#version 120

uniform sampler2D tileTexture; // uses default texture
uniform int channel_count = 3; // initialization does not work...
// vector array initialization is broken in Apple's 1.20 implementation
uniform vec3 channel_color[4]; // color for each channel, initially red, green, blue, gray
uniform vec4 channel_gamma; // brightness for each channel
uniform vec4 channel_min;
uniform vec4 channel_scale;

void main()
{
    vec4 in_color = texture2D(tileTexture, gl_TexCoord[0].xy);
    vec3 out_color = vec3(0, 0, 0);
    for (int c = 0; c < channel_count; ++c) {
        float i = in_color[c]; // intensity
        i -= channel_min[c]; // apply black level
        i *= channel_scale[c]; // apply white level
        i = clamp(i, 0, 1); // avoid extreme numbers in sum
        i = pow(i, channel_gamma[c]); // apply gamma correction
        // out_color += i * channel_color[c]; // apply channel color
        out_color = max(out_color, i * channel_color[c]); // apply channel color
    }
    gl_FragColor = vec4(out_color, 1.0);
}
