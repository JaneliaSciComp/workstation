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

import javax.media.opengl.GL3;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.geometry3d.ChannelBrightnessModel;
import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.MeshGeometry;
import org.janelia.geometry3d.Viewport;
import org.janelia.geometry3d.camera.BasicViewSlab;
import org.janelia.geometry3d.camera.ConstViewSlab;
import org.janelia.gltools.BasicGL3Actor;
import org.janelia.gltools.MeshActor;
import org.janelia.gltools.material.DepthSlabClipper;
import org.janelia.gltools.texture.Texture2d;
import org.janelia.horta.ktx.KtxData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Volume rendering actor for blocks consisting of five tetrahedral components.
 *
 * @author Christopher Bruns
 */
public class TetVolumeActor extends BasicGL3Actor 
implements DepthSlabClipper
{
    private final MeshActor meshActor;
    protected final TetVolumeMaterial material;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    // For scaling efficiency, alternate constructor takes shared resources as argument
    public TetVolumeActor(KtxData ktxData, MeshGeometry meshGeometry, ChannelBrightnessModel brightnessModel) 
    {
        super(null);
        material = new TetVolumeMaterial(ktxData, brightnessModel);
        meshActor = new TetVolumeMeshActor(meshGeometry, material, this);
        this.addChild(meshActor);
    }
    
    public void addOuterTetrahedron(int a, int b, int c, int apex) {
        ((TetVolumeMeshActor)meshActor).addOuterTetrahedron(a, b, c, apex);
    }
    
    public void setCentralTetrahedron(int a, int b, int c, int apex) {
        ((TetVolumeMeshActor)meshActor).setCentralTetrahedron(a, b, c, apex);
    }
    
    @Override
    public void display(GL3 gl, AbstractCamera camera, Matrix4 parentModelViewMatrix) 
    {
        // Adjust actual Z-clip planes to allow imposter geometry to lie
        // outside the "official" Z-clip planes. Correct final clipping will 
        // happen in the fragement shader. This is necessary because the
        // imposter geometry represents multiple voxels at various depths.
        Viewport vp = camera.getViewport();
        // Z-near remains unchanged, because we are using back faces for imposter geometry.
        // But Z-far needs to be pushed back significantly.
        ConstViewSlab slab = new BasicViewSlab(vp.getzNearRelative(), vp.getzFarRelative() + 100.0f);
        try {
            camera.pushInternalViewSlab(slab);    
            super.display(gl, camera, parentModelViewMatrix);
        }
        finally {
            camera.popInternalViewSlab();
        }
    }

    @Override
    public void setOpaqueDepthTexture(Texture2d opaqueDepthTexture) {
        material.setOpaqueDepthTexture(opaqueDepthTexture);
    }

    @Override
    public void setRelativeSlabThickness(float zNear, float zFar) {
        material.setRelativeSlabThickness(zNear, zFar);
    }

}
