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

layout (std430, binding = 0) buffer FragmentArrays {
    NodeType nodes[];
};

out vec4 blankOut;

#define MAX_DEPTH 50

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

    if (iPosition > -1 && iPosition < MAX_DEPTH) {
        nodes[xyOffset + zSize*iPosition].color = color;
        nodes[xyOffset + zSize*iPosition].depth = dz;   
    } else if (0==1) {
        // Find the closest fragment and average
        int closestIndex=0;
        float closestDistance=10000.0;
        struct NodeType closestNode;
        for (int f=0;f<MAX_DEPTH;f++) {
            struct NodeType fn = nodes[xyOffset + zSize*f];
            float distance = abs(dz - fn.depth);
            if (distance < closestDistance) {
                closestDistance = distance;
                closestIndex=f;
                closestNode=fn;
            }
        }
        vec4 newColor = mix(closestNode.color, color, 0.5);
        float newDepth = (closestNode.depth + dz)/2.0;
        closestNode.color = newColor;
        closestNode.depth = newDepth;
        nodes[xyOffset + zSize*closestIndex] = closestNode;
    } 

    blankOut = vec4(0.0, 0.0, 0.0, 0.0);
    
}