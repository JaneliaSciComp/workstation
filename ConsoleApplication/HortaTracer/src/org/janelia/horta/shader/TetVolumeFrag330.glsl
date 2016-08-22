#version 330

/**
 * Tetrahdedral volume rendering fragment shader.
 */

/* 
 * Licensed under the Janelia Farm Research Campus Software Copyright 1.1
 * 
 * Copyright (c) 2014, Howard Hughes Medical Institute, All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *     1. Redistributions of source code must retain the above copyright notice, 
 *        this list of conditions and the following disclaimer.
 *     2. Redistributions in binary form must reproduce the above copyright 
 *        notice, this list of conditions and the following disclaimer in the 
 *        documentation and/or other materials provided with the distribution.
 *     3. Neither the name of the Howard Hughes Medical Institute nor the names 
 *        of its contributors may be used to endorse or promote products derived 
 *        from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, ANY 
 * IMPLIED WARRANTIES OF MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A 
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * REASONABLE ROYALTIES; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#extension GL_ARB_shading_language_420pack : enable

layout(binding = 0) uniform sampler2D volumeTexture;

in vec4 barycentricCoord;
in vec3 fragTexCoord;

out vec4 fragColor;

void main() {
    vec4 b = barycentricCoord;
    float f = min(b.x, min(b.y, min(b.z, b.w)));

    if (f < 0) discard; // outside of tetrahedron; should not happen

#ifdef DISPLAY_EDGES_ONLY
    // Display only the edges of the triangle
    float e1 = 0; // b.x + b.y; // first leg of base triangle
    float e2 = 0; // b.x + b.z; // third leg of base triangle
    float e3 = 0; // b.y + b.z; // second leg of base triangle
    float e4 = b.x + b.w;
    float e5 = b.y + b.w;
    float e6 = b.z + b.w;
    float edge_score = max(e1, max(e2, max(e3, max(e4, max(e5, e6)))));
    if (edge_score < 0.95) discard; // hollow out non-edge region
#endif

    // reduce max intensity, to keep color channels from saturating to white
    fragColor = vec4(0.3 * fragTexCoord.rgb, 0.5);
}
