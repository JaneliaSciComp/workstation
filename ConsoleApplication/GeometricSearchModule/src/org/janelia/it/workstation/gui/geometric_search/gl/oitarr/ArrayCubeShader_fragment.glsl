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

layout (std430, binding = 2) buffer FragmentArrays2 {
    NodeType nodes2[];
};

layout (std430, binding = 3) buffer FragmentArrays3 {
    NodeType nodes3[];
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
        nodes0[xyOffset + zSize*iPosition].color = color;
        nodes0[xyOffset + zSize*iPosition].depth = dz;
    } else if (iPosition < BUFFER_DEPTH*2) {
        nodes1[xyOffset + zSize*(iPosition-BUFFER_DEPTH)].color = color;
        nodes1[xyOffset + zSize*(iPosition-BUFFER_DEPTH)].depth = dz;
    } else if (iPosition < BUFFER_DEPTH*3) {
        nodes2[xyOffset + zSize*(iPosition-BUFFER_DEPTH*2)].color = color;
        nodes2[xyOffset + zSize*(iPosition-BUFFER_DEPTH*2)].depth = dz;
    } else if (iPosition < BUFFER_DEPTH*4) {
        nodes3[xyOffset + zSize*(iPosition-BUFFER_DEPTH*3)].color = color;
        nodes3[xyOffset + zSize*(iPosition-BUFFER_DEPTH*3)].depth = dz;
    } else if (0==1) {

        // We need to preserve the closest fragments and discard the farthest away
        int farthestFIndex=0;
        int farthestBIndex=0;
        float farthestDepth=1000.0;
        int maxDepth = 4*BUFFER_DEPTH;

        for (int b=0;b<4;b++) {
            for (int f=0;f<BUFFER_DEPTH;f++) {
                float depth=0.0;
                int offset=xyOffset + zSize*f;
                if (b==0) {
                    depth=nodes0[offset].depth;
                } else if (b==1) {
                    depth=nodes1[offset].depth;
                } else if (b==2) {
                    depth=nodes2[offset].depth;
                } else if (b==3) {
                    depth=nodes3[offset].depth;
                }
                if (depth<farthestDepth) {
                    farthestDepth=depth;
                    farthestFIndex=f;
                    farthestBIndex=b;
                }
            }
        }

        if (dz < farthestDepth) {

        //    color = vec4(1.0, 0.0, 0.0, 1.0);
        //} else {
        //    color = vec4(0.0, 0.0, 1.0, 1.0);
        //}

            int replacementOffset = xyOffset + zSize*farthestFIndex;
            if (farthestBIndex==0) {
                nodes0[replacementOffset].color=color;
                nodes0[replacementOffset].depth=dz;
            } else if (farthestBIndex==1) {
                nodes1[replacementOffset].color=color;
                nodes1[replacementOffset].depth=dz;
            } else if (farthestBIndex==2) {
                nodes2[replacementOffset].color=color;
                nodes2[replacementOffset].depth=dz;
            } else if (farthestBIndex==3) {
                nodes3[replacementOffset].color=color;
                nodes3[replacementOffset].depth=dz;
            }

        }
        
    }

    blankOut = vec4(0.0, 0.0, 0.0, 0.0);

    memoryBarrier();
    
}