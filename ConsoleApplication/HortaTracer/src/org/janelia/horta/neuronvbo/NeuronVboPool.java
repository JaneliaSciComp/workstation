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

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.TreeSet;
import javax.media.opengl.GL3;
import org.janelia.console.viewerapi.model.NeuronModel;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.geometry3d.Matrix4;
import org.janelia.gltools.BasicShaderProgram;
import org.janelia.gltools.ShaderProgram;
import org.janelia.gltools.ShaderStep;
import org.janelia.gltools.texture.Texture2d;
import org.openide.util.Exceptions;

/**
 * For improved rendering performance with large numbers of neurons, NeuronVboPool
 * distributes all the neurons among a finite set of vertex buffer objects. Instead
 * of using a separate vbo for each neuron, like we were doing before.
 * @author brunsc
 */
public class NeuronVboPool 
{
    private final static int POOL_SIZE = 2;
    private final Collection<NeuronVbo> vbos;

    // private Set<NeuronModel> dirtyNeurons; // Track incremental updates
    // private Map<NeuronModel, NeuronVbo> neuronVbos;
    // TODO: increase after initial debugging
    
    // Shaders...
    // Be sure to synchronize these constants with the actual shader source uniform layout
    private final ShaderProgram conesShader = new ConesShader();
    private final ShaderProgram spheresShader = new SpheresShader();
    private final static int VIEW_UNIFORM = 1;
    private final static int PROJECTION_UNIFORM = 2;
    private final static int LIGHTPROBE_UNIFORM = 3;
    private final static int SCREENSIZE_UNIFORM = 4;
    private final static int RADIUS_OFFSET_UNIFORM = 5;
    private final static int RADIUS_SCALE_UNIFORM = 6;
    private final Texture2d lightProbeTexture;
    
    public NeuronVboPool() 
    {
        this.vbos = new TreeSet<>(new VboComparator());
        for (int i = 0; i < POOL_SIZE; ++i) {
            vbos.add(new NeuronVbo());
        }
        
        lightProbeTexture = new Texture2d();
        try {
            lightProbeTexture.loadFromPpm(getClass().getResourceAsStream(
                    "/org/janelia/gltools/material/lightprobe/"
                            + "Office1W165Both.ppm"));
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
    
    void display(GL3 gl, AbstractCamera camera) 
    {
        Matrix4 modelViewMatrix = camera.getViewMatrix();
        Matrix4 projectionMatrix = camera.getProjectionMatrix();
        float[] screenSize = new float[] {
            camera.getViewport().getWidthPixels(),
            camera.getViewport().getHeightPixels()
        };
        
        // First pass: draw all the connections (edges) between adjacent neuron anchor nodes.
        // These edges are drawn as truncated cones, tapering width between
        // the radii of the adjacent nodes.
        lightProbeTexture.bind(gl, 0);
        conesShader.load(gl);
        gl.glUniformMatrix4fv(VIEW_UNIFORM, 1, false, modelViewMatrix.asArray(), 0);
        gl.glUniformMatrix4fv(PROJECTION_UNIFORM, 1, false, projectionMatrix.asArray(), 0);
        gl.glUniform2fv(SCREENSIZE_UNIFORM, 1, screenSize, 0);
        gl.glUniform1i(LIGHTPROBE_UNIFORM, 0);
        for (NeuronVbo vbo : vbos) {
            vbo.displayEdges(gl);
        }
        
        // TODO: Second pass: repeat display loop for spheres/nodes
        spheresShader.load(gl);
        gl.glUniformMatrix4fv(VIEW_UNIFORM, 1, false, modelViewMatrix.asArray(), 0);
        gl.glUniformMatrix4fv(PROJECTION_UNIFORM, 1, false, projectionMatrix.asArray(), 0);
        gl.glUniform2fv(SCREENSIZE_UNIFORM, 1, screenSize, 0);
        gl.glUniform1i(LIGHTPROBE_UNIFORM, 0);
        for (NeuronVbo vbo : vbos) {
            vbo.displayNodes(gl);
        }
    }

    void dispose(GL3 gl) {
        for (NeuronVbo vbo : vbos) {
            vbo.dispose(gl);
        }
        lightProbeTexture.dispose(gl);
        conesShader.dispose(gl);
        spheresShader.dispose(gl);
    }

    void init(GL3 gl) {
        conesShader.init(gl);
        spheresShader.init(gl);
        lightProbeTexture.init(gl);
        for (NeuronVbo vbo : vbos) {
            vbo.init(gl);
        }
    }

    void add(NeuronModel neuron) 
    {
        // To keep the vbos balanced, always insert into the emptiest vbo
        NeuronVbo emptiestVbo = vbos.iterator().next();
        // remove, append, then insert, to maintain sorted order
        vbos.remove(emptiestVbo);
        emptiestVbo.add(neuron);
        vbos.add(emptiestVbo);
    }

    // Imposes an ordering on the VBOs in the pool, such that the first vbo is
    // always the one with the most room for more elements.
    // To maintain this ordering, it is imperative that the TreeSet<NeuronVbo> be updated
    // after every content change.
    private static class VboComparator implements Comparator<NeuronVbo> 
    {
        @Override
        public int compare(NeuronVbo o1, NeuronVbo o2) {
            return o1.getNeuronCount() - o2.getNeuronCount(); // TODO: test whether this is the correct ordering sense...     
        }
    }
    
    private static class ConesShader extends BasicShaderProgram
    {
        public ConesShader()
        {
            try {
                // Cones and spheres share a vertex shader
                getShaderSteps().add(new ShaderStep(GL3.GL_VERTEX_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/horta/shader/"
                                        + "SpheresColorVrtx430.glsl"))
                );
                getShaderSteps().add(new ShaderStep(GL3.GL_GEOMETRY_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/horta/shader/"
                                        + "imposter_fns330.glsl"))
                );
                getShaderSteps().add(new ShaderStep(GL3.GL_GEOMETRY_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/horta/shader/"
                                        + "ConesColorGeom430.glsl"))
                );
                getShaderSteps().add(new ShaderStep(GL3.GL_FRAGMENT_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/horta/shader/"
                                        + "imposter_fns330.glsl"))
                );
                getShaderSteps().add(new ShaderStep(GL3.GL_FRAGMENT_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/horta/shader/"
                                        + "ConesColorFrag430.glsl"))
                );
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }        
        }
    }

    private static class SpheresShader extends BasicShaderProgram
    {
        public SpheresShader()
        {
            try {
                getShaderSteps().add(new ShaderStep(GL3.GL_VERTEX_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/horta/shader/"
                                        + "SpheresColorVrtx430.glsl"))
                );
                getShaderSteps().add(new ShaderStep(GL3.GL_GEOMETRY_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/horta/shader/"
                                        + "SpheresColorGeom430.glsl"))
                );
                getShaderSteps().add(new ShaderStep(GL3.GL_FRAGMENT_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/horta/shader/"
                                        + "imposter_fns330.glsl"))
                );
                getShaderSteps().add(new ShaderStep(GL3.GL_FRAGMENT_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/horta/shader/"
                                        + "SpheresColorFrag430.glsl"))
                );
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }        
        }
    }
}
