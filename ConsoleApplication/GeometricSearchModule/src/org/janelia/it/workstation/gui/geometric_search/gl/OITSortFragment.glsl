#version 430

layout (early_fragment_tests) in;

in vec2 vs_tex_coord;

struct NodeType {
    vec4 color;
    float depth;
    uint next;
};

uniform usampler2D head_pointer_image;

layout (binding = 0, rgba32ui) uniform coherent uimage1D list_buffer;

layout(binding=0, std430) buffer linkedLists {
    NodeType nodes[];
};

layout (location=0) out vec4 output_color;

#define MAX_FRAGMENTS 500

struct NodeType frags[MAX_FRAGMENTS];

int build_local_fragment_list(void)
{
    int frag_count=0;
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
    vec4 final_color = vec4(1.0, 1.0, 1.0, 0.0);
    for (i=0; i < frag_count; i++) {
       final_color = blend(final_color, frags[i].color);
    }
    return final_color;
}

void main(void) {
    int frag_count=0;
    frag_count = build_local_fragment_list();
    sort_fragment_list(frag_count);
    output_color = calculate_final_color(frag_count);
}