#version 430

struct OITArrNodeType {
    vec4 color;
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

    //int check1=0;
    //int check2=0;
    //int check3=0;

    //if (fl.x==0 && fl.y==0) {
    //    nodes[0].color = vec4(1.0, 1.0, 1.0, 1.0);
    //    nodes[0].depth = 1.0;
    //} else {

    if (oldPosition<MAX_DEPTH) {

        int nodeOffset = (fl.y * hpi_width + fl.x)*hpi_depth;

        //color = vec4(0.0, 0.0, 1.0, 1.0);
        //float vz2 = 0.5;

        uint newIndex = uint(nodeOffset) + oldPosition;

        //if (nodeOffset > 100000000) {
        //    check1=1;
        //}

        //if (oldPosition > 50) {
        //    check2=1;
        //}

        //if (newIndex > 100000000) {
        //    check3=1;
        //}

        nodes[newIndex].color = color;
        //nodes[newIndex].depth = 1.0 - vz;
    }

    //}

    //float rC=0.0;
    //float gC=0.0;
    //float bC=0.1;

    //if (check1>0) {
    //    rC=1.0;
    //}

    //if (check2>0) {
    //    gC=1.0;
    //}

    //if (check3>0) {
    //    bC=1.0;
    //}

    //blankOut = vec4(rC, gC, bC, 1.0);

    blankOut = vec4(0.0, 0.0, 0.0, 0.0);
    

}