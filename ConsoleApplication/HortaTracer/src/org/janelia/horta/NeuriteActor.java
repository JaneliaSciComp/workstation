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

package org.janelia.horta;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Observable;
import java.util.Observer;
import javax.imageio.ImageIO;
import javax.media.opengl.GL3;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.MeshGeometry;
import org.janelia.geometry3d.CompositeObject3d;
import org.janelia.geometry3d.Vertex;
import org.janelia.gltools.BasicGL3Actor;
import org.janelia.gltools.MeshActor;
import org.janelia.gltools.texture.Texture2d;
import org.janelia.gltools.material.ImageParticleMaterial;
import org.janelia.gltools.material.Material;
import org.openide.util.Exceptions;

/**
 *
 * @author Christopher Bruns
 */
public class NeuriteActor extends BasicGL3Actor {

    private final NeuriteModel neuriteModel;
    private final MeshGeometry meshGeometry;
    private final MeshActor meshActor;
    private ImageParticleMaterial material;
    
    public NeuriteActor(CompositeObject3d parent, final NeuriteModel neuriteModel) 
    {
        super(parent);
        BufferedImage ringImage = null;
        try {
            ringImage = ImageIO.read(
                    getClass().getResourceAsStream(
                            // "/org/janelia/gltools/material/lightprobe/"
                            //         + "ComponentSphere.png"));
                            "/org/janelia/horta/"
                                    + "frame_circle.png"));
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        Texture2d ringTexture = new Texture2d();
        ringTexture.loadFromBufferedImage(ringImage);
        ringTexture.setGenerateMipmaps(true);
        ringTexture.setMinFilter(GL3.GL_LINEAR_MIPMAP_LINEAR);
        material = new ImageParticleMaterial(ringTexture);
        // TODO - update meshGeometry after anchor moves...
        meshGeometry = new MeshGeometry();
        meshActor = new MeshActor(meshGeometry, material, this);
        this.addChild(meshActor);

        this.neuriteModel = neuriteModel;
        neuriteModel.addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                // System.out.println("Neurite actor update");
                boolean geometryChanged = false;
                if (! meshGeometry.isEmpty()) {
                    meshGeometry.clear();
                    geometryChanged = true;
                }
                
                if (! neuriteModel.isEmpty()) {
                    for (NeuriteAnchor anchor : neuriteModel) {
                        Vertex vertex = meshGeometry.addVertex(anchor.getLocationUm());
                        vertex.setAttribute("radius", anchor.getRadiusUm());
                        geometryChanged = true;
                        // System.out.println("Neurite actor anchor location "+anchor.getLocationUm()+"; radius = "+anchor.getRadiusUm()); // works
                    }
                }
                if (geometryChanged)
                    meshGeometry.notifyObservers();
            }
        });
    }
    
    @Override
    public void display(GL3 gl, AbstractCamera camera, Matrix4 parentModelViewMatrix) {
        // if (meshGeometry.size() < 1) return;
        // System.out.println("Displaying anchors, geometry size = "+meshGeometry.size()); // works
        gl.glDisable(GL3.GL_DEPTH_TEST);
        super.display(gl, camera, parentModelViewMatrix);
        // System.out.println("Anchor ModelView matrix = "+parentModelViewMatrix);
        // System.out.println("Finished display anchors");        
    }

    void setColor(Color color) {
        material.setDiffuseColor(color);
        material.setSpecularColor(Color.BLACK);
    }

    NeuriteModel getModel() {
        return neuriteModel; //To change body of generated methods, choose Tools | Templates.
    }
    
}
