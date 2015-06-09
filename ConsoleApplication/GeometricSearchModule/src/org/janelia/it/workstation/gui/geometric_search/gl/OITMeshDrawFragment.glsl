#version 430

// vertex to fragment shader io
in vec3 N;
in vec3 I;
in vec4 Cs;

// globals
//uniform float edgefalloff;
//uniform float intensity;
//uniform float ambient;

layout (early_fragment_tests) in;
layout (binding = 0, offset = 0) uniform atomic_uint index_counter;
layout (binding = 0, r32ui) uniform uimage2D head_pointer_image;
layout (binding = 1, rgba32ui) uniform coherent uimageBuffer list_buffer;

out vec4 blankOut;

// entry point
void main()
{
    // Actual fragment shading step
    float edgefalloff=1.0;
    float intensity=0.5;
    float ambient=0.01;

    float opac = dot(normalize(-N), normalize(-I));
    opac = abs(opac);
    opac = ambient + intensity*(1.0-pow(opac, edgefalloff));
    vec4 color =  opac * Cs;
    color.a = opac;

    ivec2 fl = ivec2(gl_FragCoord.xy);
    uint new_index = atomicCounterIncrement(index_counter);
    int iNewIndex = int(new_index);
    uint old_head = imageAtomicExchange(head_pointer_image, fl, new_index);
    uvec4 item;
    item.x = old_head;
    item.y = packUnorm4x8(color);
    item.z = floatBitsToUint(gl_FragCoord.z);
    item.w = 0;
    imageStore(list_buffer, iNewIndex, item);

    blankOut = vec4(0.0, 0.0, 0.0, 0.0);

}