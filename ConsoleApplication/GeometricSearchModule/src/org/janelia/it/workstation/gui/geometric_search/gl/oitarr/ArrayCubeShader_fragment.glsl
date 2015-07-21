#version 430

struct OITArrNodeType {
    vec4 color;
    float depth;
    uint next;
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

#define MAX_DEPTH 100

// entry point
void main()
{

    float dopac = dcolor.w;
    vec4 color = dcolor * intensityF;
    color.w = dopac;
    ivec2 fl = ivec2(gl_FragCoord.xy);

    uint oldPosition = imageAtomicAdd(head_pointer_image, fl, 1);

    //int loopFlag=0;

    //if (fl.x==0 && fl.y==0) {
    //    nodes[0].color = vec4(1.0, 1.0, 1.0, 1.0);
    //    nodes[0].depth = 1.0;
    //} else {

    if (oldPosition<MAX_DEPTH) {

        int nodeOffset = (fl.y * hpi_width + fl.x)*hpi_depth;

        //color = vec4(0.0, 0.0, 1.0, 1.0);
        //float vz2 = 0.5;

        uint newIndex = uint(nodeOffset) + oldPosition;

        //if (newIndex > 100000000) {
        //    loopFlag=1;
        //}

        nodes[newIndex].color = color;
        nodes[newIndex].depth = 1.0 - vz;
    }

    //}

    blankOut = vec4(0.0, 0.0, 0.0, 0.0);

    //if (loopFlag>0) {
    //    blankOut = vec4(0.7, 0.7, 1.0, 0.0);
    //}

}