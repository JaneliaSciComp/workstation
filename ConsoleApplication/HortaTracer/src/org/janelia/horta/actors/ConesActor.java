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
import org.janelia.geometry3d.Vector3;
import org.janelia.geometry3d.Vertex;
import org.janelia.gltools.BasicGL3Actor;
import org.janelia.gltools.MeshActor;
import org.janelia.horta.modelapi.NeuronReconstruction;
import org.janelia.horta.modelapi.NeuronVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Christopher Bruns
 */
public class ConesActor extends BasicGL3Actor 
{
    private final MeshGeometry meshGeometry;
    private final MeshActor meshActor;
    private final ConesMaterial material;
    private final NeuronReconstruction neuron;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    public ConesActor(final NeuronReconstruction neuron) {
        super(null);
        material = new ConesMaterial();
        meshGeometry = new MeshGeometry();
        meshActor = new MeshActor(meshGeometry, material, this);
        this.addChild(meshActor);
        this.neuron = neuron;
        
        neuron.addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg)
            {
                setColor(neuron.getColor());
                setVisible(neuron.isVisible());
                // TODO: more careful updating of nodes
                meshGeometry.clear();
                int vertexIndex = 0;
                for (NeuronVertex neuronVertex : neuron.getVertexes()) {
                    NeuronVertex parent = neuronVertex.getParentVertex();
                    if (parent == null) 
                        continue; // need an edge, to draw a cone
                    
                    // Modify locations and radii, so cone is flush with adjacent spheres
                    Vector3 cs1 = neuronVertex.getLocation(); // center of first sphere
                    Vector3 cs2 = parent.getLocation(); // center of second sphere
                    double rs1 = neuronVertex.getRadius();
                    double rs2 = parent.getRadius();
                    // Swap so r2 is always the largest
                    if (rs2 < rs1) {
                        cs2 = neuronVertex.getLocation(); // center of first sphere
                        cs1 = parent.getLocation(); // center of second sphere
                        rs2 = neuronVertex.getRadius();
                        rs1 = parent.getRadius();
                    }
                    double d = (cs2.minus(cs1)).length(); // distance between sphere centers
                    // half cone angle, to just touch each sphere
                    double sinAlpha = (rs2 - rs1) / d;
                    double cosAlpha = Math.sqrt(1 - sinAlpha*sinAlpha);
                    // Actual cone terminal radii might be smaller than sphere radii
                    double r1 = cosAlpha * rs1;
                    double r2 = cosAlpha * rs2;
                    // Cone termini might not lie at sphere centers
                    Vector3 aHat = (cs1.minus(cs2)).multiplyScalar((float)(1.0/d));
                    Vector3 dC1 = new Vector3(aHat).multiplyScalar((float)(sinAlpha * rs1));
                    Vector3 dC2 = new Vector3(aHat).multiplyScalar((float)(sinAlpha * rs2));
                    // Cone termini
                    Vector3 c1 = cs1.plus(dC1);
                    Vector3 c2 = cs2.plus(dC2);

                    // Insert two vertices and an edge into the Geometry object
                    Vertex vertex1 = meshGeometry.addVertex(c1);
                    vertex1.setAttribute("radius", (float)r1);
                    
                    Vertex vertex2 = meshGeometry.addVertex(c2);
                    vertex2.setAttribute("radius", (float)r2);
                    
                    // logger.info("Node locations " + cs1 + ":" + rs1 + ", " + cs2 + ":" + rs2);
                    // logger.info("Creating edge " + c1 + ":" + r1 + ", " + c2 + ":" + r2);
                    
                    meshGeometry.addEdge(vertexIndex, vertexIndex+1);
                    
                    vertexIndex += 2;
                }
                meshGeometry.notifyObservers(); // especially the Material?
            }
        });
    }
    
    @Override
    public void display(GL3 gl, AbstractCamera camera, Matrix4 parentModelViewMatrix) {
        // Propagate any pending structure changes...
        if (neuron != null)
            neuron.notifyObservers();
        if (! isVisible()) return;
        // if (meshGeometry.size() < 1) return;
        // gl.glDisable(GL3.GL_DEPTH_TEST);
        super.display(gl, camera, parentModelViewMatrix);       
    }

    public void setColor(Color color) {
        material.setColor(color);
    }
}
