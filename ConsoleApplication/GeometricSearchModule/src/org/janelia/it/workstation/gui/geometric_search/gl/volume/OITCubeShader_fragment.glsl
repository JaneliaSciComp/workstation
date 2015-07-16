#version 430

struct NodeType {
    vec4 color;
    float depth;
    uint next;
};

uniform vec4 dcolor;

layout (early_fragment_tests) in;

layout (binding=0, offset=0) uniform atomic_uint index_counter;
layout (binding = 1, r32ui) uniform uimage2D head_pointer_image;
layout (binding = 0, std430) buffer linkedLists {
    NodeType nodes[];
};

// 2048 x 2048 x 200
#define MAX_NODES 838860800 

out vec4 blankOut;

// entry point
void main()
{

    vec4 color = dcolor;

    ivec2 fl = ivec2(gl_FragCoord.xy);
    uint new_index = atomicCounterIncrement(index_counter);
    if (new_index < MAX_NODES) {
        int iNewIndex = int(new_index);
        uint old_head = imageAtomicExchange(head_pointer_image, fl, new_index);
        nodes[new_index].color = color;
        nodes[new_index].depth = 1.0 - gl_FragCoord.z;
        nodes[new_index].next = old_head;
    }

    blankOut = vec4(0.0, 0.0, 0.0, 0.0);

}