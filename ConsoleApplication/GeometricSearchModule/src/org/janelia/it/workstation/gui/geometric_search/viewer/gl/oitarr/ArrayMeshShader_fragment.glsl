 #version 430

// vertex to fragment shader io
in vec3 N;
in vec3 I;
in vec4 Cs;

struct NodeType {
    uint colorUPack;
    float depth;
};

layout (early_fragment_tests) in;

in float vz;

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

#define BUFFER_DEPTH GL_TRANSPARENCY_QUARTERDEPTH_INT

void main()
{
    float dz = 1.0 - vz;

    // Actual fragment shading step
     float edgefalloff=1.0;
     float intensity=0.2;
     float ambient=0.05;

     float opac = dot(normalize(-N), normalize(-I));
     opac = abs(opac);
     opac = ambient + intensity*(1.0-pow(opac, edgefalloff));
     vec4 color =  opac * Cs;
     color.a = opac;


    ivec2 fl = ivec2(gl_FragCoord.xy);
    uint oldPosition = imageAtomicAdd(head_pointer_image, fl, 1);
    int iPosition = int(oldPosition);
    int zSize=hpi_width * hpi_height;
    int xyOffset = (fl.y * hpi_width + fl.x);

    if (iPosition > -1 && iPosition < BUFFER_DEPTH) {
        nodes0[xyOffset + zSize*iPosition].colorUPack = packUnorm4x8(color);
        nodes0[xyOffset + zSize*iPosition].depth = dz;
    } else if (iPosition < BUFFER_DEPTH*2) {
        nodes1[xyOffset + zSize*(iPosition-BUFFER_DEPTH)].colorUPack = packUnorm4x8(color);
        nodes1[xyOffset + zSize*(iPosition-BUFFER_DEPTH)].depth = dz;
    } else if (iPosition < BUFFER_DEPTH*3) {
        nodes2[xyOffset + zSize*(iPosition-BUFFER_DEPTH*2)].colorUPack = packUnorm4x8(color);
        nodes2[xyOffset + zSize*(iPosition-BUFFER_DEPTH*2)].depth = dz;
    } else if (iPosition < BUFFER_DEPTH*4) {
        nodes3[xyOffset + zSize*(iPosition-BUFFER_DEPTH*3)].colorUPack = packUnorm4x8(color);
        nodes3[xyOffset + zSize*(iPosition-BUFFER_DEPTH*3)].depth = dz;
    }

    blankOut = vec4(0.0, 0.1, 0.0, 0.0);

    memoryBarrier();

}