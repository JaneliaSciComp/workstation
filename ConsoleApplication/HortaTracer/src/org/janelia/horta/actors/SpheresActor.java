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

package org.janelia.horta.actors;

import java.awt.Color;
import java.util.Observable;
import java.util.Observer;
import javax.media.opengl.GL3;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.MeshGeometry;
import org.janelia.geometry3d.Vertex;
import org.janelia.gltools.BasicGL3Actor;
import org.janelia.gltools.MeshActor;
import org.janelia.console.viewerapi.model.NeuronReconstruction;
import org.janelia.console.viewerapi.model.NeuronVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Christopher Bruns
 */
public class SpheresActor extends BasicGL3Actor 
{
    private final MeshGeometry meshGeometry;
    private final MeshActor meshActor;
    private final SpheresMaterial material;
    private final NeuronReconstruction neuron;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    public SpheresActor(final NeuronReconstruction neuron) {
        super(null);
        material = new SpheresMaterial();
        meshGeometry = new MeshGeometry();
        meshActor = new MeshActor(meshGeometry, material, this);
        this.addChild(meshActor);
        this.neuron = neuron;
        setColor(neuron.getColor());
        
        neuron.getVisibilityChangeObservable().addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg)
            {
                setVisible(neuron.isVisible());
            }
        });
        neuron.getColorChangeObservable().addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg)
            {
                setColor(neuron.getColor());
            }
        });
        neuron.getGeometryChangeObservable().addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg)
            {
                // TODO: more careful updating of nodes
                meshGeometry.clear();
                for (NeuronVertex neuronVertex : neuron.getVertexes()) {
                    Vertex vertex = meshGeometry.addVertex(neuronVertex.getLocation());
                    vertex.setAttribute("radius", (float)neuronVertex.getRadius());
                }
                meshGeometry.notifyObservers(); // especially the Material?
            }
        });
    }
    
    @Override
    public void display(GL3 gl, AbstractCamera camera, Matrix4 parentModelViewMatrix) {
        // Propagate any pending structure changes...
        if (neuron != null) {
            neuron.getVisibilityChangeObservable().notifyObservers();
        }
        if (! isVisible()) return;
        if (neuron != null) {
            neuron.getColorChangeObservable().notifyObservers();
            neuron.getGeometryChangeObservable().notifyObservers();
        }
        // if (meshGeometry.size() < 1) return;
        // gl.glDisable(GL3.GL_DEPTH_TEST);
        super.display(gl, camera, parentModelViewMatrix);       
    }

    public final void setColor(Color color) {
        material.setColor(color);
    }
}
