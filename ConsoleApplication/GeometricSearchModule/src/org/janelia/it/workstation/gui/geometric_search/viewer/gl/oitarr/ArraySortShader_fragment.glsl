#version 430

layout (early_fragment_tests) in;

struct NodeType {
    uint colorUPack;
    float depth;
};

in vec2 vs_tex_coord;

uniform usampler2D head_pointer_image;

uniform int hpi_width;
uniform int hpi_height;
uniform int hpi_depth;

layout (binding = 0, rgba32ui) uniform coherent uimage1D fragment_buffer;

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

layout (location=0) out vec4 output_color;

#define BUFFER_DEPTH GL_TRANSPARENCY_QUARTERDEPTH_INT
#define MAX_DEPTH GL_TRANSPARENCY_MAXDEPTH_INT

struct NodeType frags[MAX_DEPTH];

ivec2 fl;

int build_local_fragment_list(void)
{
    int frag_count=0;
    fl = ivec2(gl_FragCoord.xy);
    int zSize=hpi_width * hpi_height;

    uint hpiFragCount = texelFetch(head_pointer_image, fl, 0).x;
    while (frag_count < hpiFragCount && frag_count < MAX_DEPTH) {
        if (frag_count < BUFFER_DEPTH) {
            int nodeOffset = (fl.y * hpi_width + fl.x) + zSize * frag_count;
            frags[frag_count] = nodes0[nodeOffset];
        } else if (frag_count < BUFFER_DEPTH*2) {
           int nodeOffset = (fl.y * hpi_width + fl.x) + zSize * (frag_count-BUFFER_DEPTH);
           frags[frag_count] = nodes1[nodeOffset];
        } else if (frag_count < BUFFER_DEPTH*3) {
           int nodeOffset = (fl.y * hpi_width + fl.x) + zSize * (frag_count-BUFFER_DEPTH*2);
           frags[frag_count] = nodes2[nodeOffset];
        } else if (frag_count < BUFFER_DEPTH*4) {
           int nodeOffset = (fl.y * hpi_width + fl.x) + zSize * (frag_count-BUFFER_DEPTH*3);
           frags[frag_count] = nodes3[nodeOffset];
        }
        frag_count++;
    }
    return frag_count;
}

void sort_fragment_list(int frag_count)
{
    int i;
    int j;
    for (i=0;i<frag_count;i++) {
        for (j=i+1; j<frag_count;j++) {
            float depth_i = frags[i].depth;
            float depth_j = frags[j].depth;
            if (depth_i > depth_j) {
                struct NodeType temp = frags[i];
                frags[i] = frags[j];
                frags[j] = temp;
            }
        }
    }
}

vec4 blend(vec4 current_color, vec4 new_color) {
    return mix(current_color, new_color, new_color.a);
}

vec4 calculate_final_color(int frag_count) {
    int i;
    vec4 final_color = vec4(0.0, 0.0, 0.0, 0.0);
    for (i=0; i < frag_count; i++) {
       vec4 color = unpackUnorm4x8(frags[i].colorUPack);

       // MIP
       //float f=final_color.x+final_color.y+final_color.z;
       //float c=color.x+color.y+color.z;
       //if (c>f) {
       //     final_color=color;
       //     f=c;
       //}

       //if (f>650) break;

       // Transparency
       //final_color = blend(final_color, color);
       final_color = mix(final_color, color, color.a);
    }
    return final_color;
}

void main(void) {
    int frag_count=0;
    frag_count = build_local_fragment_list();
    sort_fragment_list(frag_count);
    output_color = calculate_final_color(frag_count);

//    if (frag_count>0) {
//        output_color = vec4(0.0, 0.0, 1.0, 1.0);
//     } else {
//        output_color = vec4(1.0, 0.0, 0.0, 1.0);
//     }

    //if (frag_count>=MAX_DEPTH) {
    //    output_color = vec4(0.0, 0.0, 1.0, 1.0);
    //}
}