#version 430

uniform vec4 dcolor;

struct NodeType {
    vec4 color;
    float depth;
};

layout (early_fragment_tests) in;

in float vz;
in float intensityF;

uniform int hpi_width;
uniform int hpi_height;
uniform int hpi_depth;

layout (binding = 1, r32ui) uniform uimage2D head_pointer_image;

layout (std430, binding = 0) buffer FragmentArrays0 {
    NodeType nodes0[];
};

layout (std430, binding = 1) buffer FragmentArrays1 {
    NodeType nodes1[];
};

out vec4 blankOut;

#define BUFFER_DEPTH 50

void main()
{
    float dz = 1.0 - vz;

    float dopac = dcolor.w;
    vec4 color = dcolor * intensityF;
    color.w = dopac;


    ivec2 fl = ivec2(gl_FragCoord.xy);

    uint oldPosition = imageAtomicAdd(head_pointer_image, fl, 1);

    int iPosition = int(oldPosition);

    int zSize=hpi_width * hpi_height;

    int xyOffset = (fl.y * hpi_width + fl.x);

    if (iPosition > -1 && iPosition < BUFFER_DEPTH) {
        if (iPosition<5) {
            nodes0[xyOffset + zSize*iPosition].color = color;
        } else {
            nodes0[xyOffset + zSize*iPosition].color = color;
        }
        nodes0[xyOffset + zSize*iPosition].depth = dz;
    } else if (iPosition > BUFFER_DEPTH && iPosition < BUFFER_DEPTH*2) {
        nodes1[xyOffset + zSize*(iPosition-BUFFER_DEPTH)].color = color;
        nodes1[xyOffset + zSize*(iPosition-BUFFER_DEPTH)].depth = dz;
    } 

    blankOut = vec4(0.0, 0.0, 0.0, 0.0);

    memoryBarrier();
    
}