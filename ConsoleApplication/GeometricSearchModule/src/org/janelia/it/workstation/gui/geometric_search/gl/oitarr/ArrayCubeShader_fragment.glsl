#version 430

struct OITArrNodeType {
    vec4 color;
    float depth;
};

uniform vec4 dcolor;

layout (early_fragment_tests) in;

in float vz;
in float intensityF;

uniform int hpi_width;
uniform int hpi_height;
uniform int hpi_depth;

layout (binding = 1, r32ui) uniform uimage2D head_pointer_image;
layout (binding=0, std430) buffer fragmentArrays {
    OITArrNodeType nodes[];
};

out vec4 blankOut;

// entry point
void main()
{

    float dopac = dcolor.w;
    vec4 color = dcolor * intensityF;
    color.w = dopac;
    ivec2 fl = ivec2(gl_FragCoord.xy);

    int nodeOffset = (gl_FragCoord.y * hpi_width + gl_FragCoord.x)*hpi_depth;

    uint oldPosition = imageAtomicAdd(head_pointer_image, fl, 1);

    OITArrNodeType newNode;

    newNode.color = color;
    newNode.depth = 1.0 - vz;

    nodes[nodeOffset + oldPosition] = newNode;

    blankOut = vec4(0.0, 0.0, 0.0, 0.0);

}