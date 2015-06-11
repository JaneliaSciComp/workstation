#version 430

in vec2 vs_tex_coord;

struct NodeType {
    vec4 color;
    float depth;
    uint next;
};

uniform usampler2D head_pointer_image;
layout (binding = 0, rgba32ui) uniform coherent uimage1D list_buffer;

//layout (binding = 0, rgba32ui) uniform coherent uimageBuffer list_buffer;

layout(binding=0, std430) buffer linkedLists {
    NodeType nodes[];
};

layout (location=0) out vec4 output_color;

#define MAX_FRAGMENTS 75

struct NodeType frags[MAX_FRAGMENTS];

int build_local_fragment_list(void)
{
    int frag_count=0;

    //current = imageLoad(head_pointer_image, ivec2(gl_FragCoord.xy)).x;
    //current = texelFetch(head_pointer_image, ivec2(gl_FragCoord.xy), 0).x;
    //current = texelFetch(head_pointer_image, ivec2(vs_tex_coord.xy), 0).x;

    uint current = texelFetch(head_pointer_image, ivec2(gl_FragCoord.xy), 0).x;

    while( current != 0xFFFFFFFF && frag_count < MAX_FRAGMENTS) {
        frags[frag_count] = nodes[current];
        current = nodes[current].next;
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

            //float depth_i = uintBitsToFloat(fragments[i].z);
            //float depth_j = uintBitsToFloat(fragments[j].z);

            if (depth_i > depth_j) {

                //uvec4 temp = fragments[i];
                //fragments[i] = fragments[j];
                //fragments[j] = temp;

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

    vec4 final_color = vec4(0.0);

    for (i=0; i < frag_count; i++) {
       final_color = blend(final_color, frags[i].color);
    }

    return final_color;
}

void main(void) {

   int frag_count;

    frag_count = build_local_fragment_list();

    sort_fragment_list(frag_count);

    output_color = calculate_final_color(frag_count);

}
