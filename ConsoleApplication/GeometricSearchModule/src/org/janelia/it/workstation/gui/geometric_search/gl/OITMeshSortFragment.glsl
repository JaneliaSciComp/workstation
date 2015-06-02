#version 430

//layout (binding = 0, offset = 0) uniform atomic_uint index_counter;
//layout (binding = 0, rgba32ui) uniform uimageBuffer list_buffer;
//layout (binding = 1, r32ui) uniform uimage2D head_pointer_image;

uniform sampler2D head_pointer_image;

uniform samplerBuffer list_buffer;

#define MAX_FRAGMENTS 15

uvec fragments[MAX_FRAGMENTS];

layout (location=0) out vec4 output_color;

void main(void) {

    int frag_count;

    frag_count = build_local_fragment_list();

    sort_fragment_list(frag_count);

    output_color = calculate_final_color(frag_count);

}


int build_local_fragment_list(void)
{
    uint current;
    int frag_count=0;

    current = texelFetch(head_pointer_image, ivec2(gl_FragCoord.xy), 0);

    while( current != 0xFFFFFFFF && frag_count < MAX_FRAGMENTS) {
        item = texelFetch(list_buffer, current);
        current = item.x;
        uvec4 fragments[frag_count] = item;
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
            float depth_i = uintBitsToFloat(fragments[i].z);
            float depth_j = uintBitsToFloat(fragments[j].z);

            if (depth_i > depth_j) {
                uvec4 temp = fragments[i];
                fragments[i] = fragments[j];
                fragments[j] = temp;
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
       vec4 frag_color = unpackUnorm4x8(fragments[i].y);
       final_color = blend(final_color, frag_color);
    }

    return final_color;
}