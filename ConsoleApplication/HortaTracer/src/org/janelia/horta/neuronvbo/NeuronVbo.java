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
package org.janelia.horta.neuronvbo;

import com.jogamp.common.nio.Buffers;
import java.awt.Color;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.media.opengl.GL3;
import org.janelia.console.viewerapi.model.NeuronEdge;
import org.janelia.console.viewerapi.model.NeuronModel;
import org.janelia.console.viewerapi.model.NeuronVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds one OpenGL vertex buffer object for rendering groups of neuron models.
 * Multiple NeuronVbos may be held in a NeuronVboPool
 * @author brunsc
 */
public class NeuronVbo 
{
    private final static int FLOATS_PER_VERTEX = 8;
    // Be sure to synchronize these constants with the actual shader vertex attribute (in) layout
    private final static int XYZR_ATTRIB = 1;
    private final static int RGBV_ATTRIB = 2;

    private Set<NeuronModel> neurons = new HashSet<>();
    private int vboVertices = 0;
    private int vboEdgeIndices = 0;
    private int edgeCount = 0;
    private int vertexCount = 0;
    private boolean buffersAreDirty = false;
    
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    int getNeuronCount() {
        return neurons.size();
    }
    
    void init(GL3 gl)
    {
        if (vboEdgeIndices > 0)
            return; // already initialized
        IntBuffer vbos = IntBuffer.allocate(2);
        vbos.rewind();
        gl.glGenBuffers(2, vbos);
        vboVertices = vbos.get(0);
        vboEdgeIndices = vbos.get(1);
        refreshBuffers(gl);
    }
    
    // Make sure the cone shader is loaded before calling this method
    synchronized void displayEdges(GL3 gl) 
    {
        init(gl);
        
        if (edgeCount < 1) 
            return;
        
        if (buffersAreDirty)
            refreshBuffers(gl);
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, vboVertices);
        gl.glEnableVertexAttribArray(XYZR_ATTRIB);
        gl.glVertexAttribPointer(
                XYZR_ATTRIB, // location in shader
                4, // component count
                GL3.GL_FLOAT, // component type
                false, // normalize ints?
                32, // stride in bytes, 8 * sizeof(float)
                0 // offset, in bytes
                );
        gl.glEnableVertexAttribArray(RGBV_ATTRIB);
        gl.glVertexAttribPointer(
                RGBV_ATTRIB, // location in shader
                4, // component count
                GL3.GL_FLOAT, // component type
                false, // normalize ints?
                32, // stride in bytes, 2 * 4 * sizeof(float)
                16 // offset, in bytes = 4 * sizeof(float)
                );
        gl.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, vboEdgeIndices);        
        gl.glDrawElements(GL3.GL_LINES, 2 * edgeCount, GL3.GL_UNSIGNED_INT, 0);
    }
    
    synchronized void dispose(GL3 gl) 
    {
        if (vertexCount > 0)
            buffersAreDirty = true;
        if (vboVertices == 0)
            return; // never allocated
        int [] vbos = {vboVertices, vboEdgeIndices};
        gl.glDeleteBuffers(2, vbos, 0);
        vboVertices = 0;
        vboEdgeIndices = 0;
    }
    
    private synchronized void refreshBuffers(GL3 gl) 
    {
        // count the primitives
        List<Float> vertexAttributes = new ArrayList<>();
        List<Integer> edgeIndexes = new ArrayList<>();
        vertexCount = 0;
        edgeCount = 0;
        float rgb[] = {0,0,0};
        for (NeuronModel neuron : neurons) {
            if (! neuron.isVisible())
                continue;
            float visibility = neuron.isVisible() ? 1 : 0;
            Color color = neuron.getColor();
            color.getColorComponents(rgb);
            Map<NeuronVertex, Integer> vertexIndices = new HashMap<>();
            for (NeuronVertex vertex : neuron.getVertexes()) {
                int index = vertexCount;
                vertexIndices.put(vertex, index);
                // X, Y, Z, radius, r, g, b, visibility
                float[] xyz = vertex.getLocation();
                vertexAttributes.add(xyz[0]); // X
                vertexAttributes.add(xyz[1]); // Y
                vertexAttributes.add(xyz[2]); // Z
                vertexAttributes.add(vertex.getRadius()); // radius
                vertexAttributes.add(rgb[0]); // red
                vertexAttributes.add(rgb[1]); // green
                vertexAttributes.add(rgb[2]); // blue
                vertexAttributes.add(visibility); // visibility
                vertexCount += 1;
            }
            for (NeuronEdge edge : neuron.getEdges()) {
                for (NeuronVertex v : edge) {
                    int index = vertexIndices.get(v);
                    edgeIndexes.add(index);
                    edgeCount += 1;
                }
            }
        }
        edgeCount = edgeCount / 2;
        
        // allocate storage
        FloatBuffer vertexBuffer = Buffers.newDirectFloatBuffer(vertexCount * FLOATS_PER_VERTEX);        
        vertexBuffer.rewind();
        for (float f : vertexAttributes) {
            vertexBuffer.put(f);
        }
        vertexBuffer.flip();
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, vboVertices);
        gl.glBufferData(GL3.GL_ARRAY_BUFFER, 
                vertexBuffer.capacity() * Buffers.SIZEOF_FLOAT,
                vertexBuffer, 
                GL3.GL_STATIC_DRAW);
        
        IntBuffer edgeBuffer = Buffers.newDirectIntBuffer(edgeCount * 2);
        edgeBuffer.rewind();
        for (int i : edgeIndexes) {
            edgeBuffer.put(i);
        }
        edgeBuffer.flip();
        gl.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, vboEdgeIndices);        
        gl.glBufferData(
                GL3.GL_ELEMENT_ARRAY_BUFFER,
                edgeBuffer.capacity() * Buffers.SIZEOF_INT,
                edgeBuffer,
                GL3.GL_STATIC_DRAW);

        final boolean debugVboContents = true;
        if (debugVboContents) {
            log.info("Vertex buffer contents:");
            for (int i = 0; i < vertexBuffer.capacity(); ++i) {
                log.info(" vertex {} : {}", i, vertexBuffer.get(i));            
            }
            log.info("Edge buffer contents:");
            for (int i = 0; i < edgeBuffer.capacity(); ++i) {
                log.info(" edge {} : {}", i, edgeBuffer.get(i));            
            }
        }
        
        buffersAreDirty = false;
    }

    void add(NeuronModel neuron) {
        if (neurons.add(neuron)) {
            buffersAreDirty = true;
        }
    }

}
