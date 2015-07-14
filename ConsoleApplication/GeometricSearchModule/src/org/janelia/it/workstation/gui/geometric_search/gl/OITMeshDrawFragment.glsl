#version 430

// vertex to fragment shader io
//in vec3 N;
//in vec3 I;
in vec4 Cs;

//flat in vec3 diffuseColor;
//flat in vec3 specularColor;

struct NodeType {
    vec4 color;
    float depth;
    uint next;
};

// globals
//uniform float edgefalloff;
//uniform float intensity;
//uniform float ambient;

layout (early_fragment_tests) in;

layout (binding=0, offset=0) uniform atomic_uint index_counter;
layout (binding = 1, r32ui) uniform uimage2D head_pointer_image;
layout (binding = 0, std430) buffer linkedLists {
    NodeType nodes[];
};

// 2048 x 2048 x 20
#define MAX_NODES 83886080 

out vec4 blankOut;

// entry point
void main()
{

    //float scaleS = 10.0;
    //float scaleT = 10.0;

    //float thresholdS = 0.13;
    //float thresholdT = 0.13;

    //vec3 surfaceColor = vec3(0.8, 0.8, 0.7);

    //vec3 finalColor = surfaceColor * diffuseColor + specularColor;
    //vec4 color = vec4(finalColor, 0.01);



        // BEGIN ORIGINAL x-ray code

    // Actual fragment shading step
    //float edgefalloff=1.0;
    //float intensity=0.5;
    //float ambient=0.1;

    vec4 color = Cs;

        //float opac = dot(normalize(-N), normalize(-I)); 
        //opac = abs(opac);
        //opac = ambient + intensity*(1.0-pow(opac, edgefalloff));
        //color =  opac * color;
        //color.a = opac;

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