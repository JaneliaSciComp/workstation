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

import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.media.opengl.GL3;
import org.janelia.console.viewerapi.model.ChannelColorModel;
import org.janelia.console.viewerapi.model.ImageColorModel;
import org.janelia.geometry3d.AbstractCamera;
// import org.janelia.geometry3d.ChannelBrightnessModel;
import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.PerspectiveCamera;
import org.janelia.geometry3d.Viewport;
import org.janelia.geometry3d.camera.BasicViewSlab;
import org.janelia.geometry3d.camera.ConstViewSlab;
import org.janelia.gltools.BasicGL3Actor;
import org.janelia.gltools.material.DepthSlabClipper;
import org.janelia.gltools.texture.Texture2d;
import org.janelia.horta.ktx.KtxData;
import org.openide.util.Exceptions;
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
    private static TetVolumeActor singletonInstance;

    // Singleton access
    static public TetVolumeActor getInstance() {
        if (singletonInstance == null)
            singletonInstance = new TetVolumeActor();
        return singletonInstance;
    }
    
    // private final MeshActor meshActor;
    // protected final TetVolumeMaterial material;
    private final TetVolumeMaterial.TetVolumeShader shader;
    private ImageColorModel brightnessModel;
    private final Texture2d colorMapTexture = new Texture2d();
    private float zNearRelative = 0.10f;
    private float zFarRelative = 100.0f; // relative z clip planes
    private final float[] zNearFar = new float[] {0.1f, 100.0f}; // absolute clip for shader
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    public TetVolumeActor() {
        super(null);
        shader = new TetVolumeMaterial.TetVolumeShader();
        BufferedImage colorMapImage = null;
        try {
            colorMapImage = ImageIO.read(
                    getClass().getResourceAsStream(
                            "/org/janelia/horta/images/"
                            + "HotColorMap.png"));
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        colorMapTexture.loadFromBufferedImage(colorMapImage);
        colorMapTexture.setGenerateMipmaps(false);
        colorMapTexture.setMinFilter(GL3.GL_LINEAR);
        colorMapTexture.setMagFilter(GL3.GL_LINEAR);
    }

    public void addKtxBlock(KtxData ktxData) 
    {    
        this.addChild(new TetVolumeMeshActor(ktxData, shader, this));
    }
    
    @Override
    public void init(GL3 gl) 
    {
        super.init(gl);
        colorMapTexture.init(gl);
        shader.init(gl);
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
            // Bind color map to texture unit 1, because 3D volume textures will use unit zero.
            colorMapTexture.bind(gl, 1);
            shader.load(gl);
            // Z-clip planes
            float focusDistance = ((PerspectiveCamera)camera).getCameraFocusDistance();
            zNearFar[0] = zNearRelative * focusDistance;
            zNearFar[1] = zFarRelative * focusDistance;
            final int zNearFarUniformIndex = 2; // explicitly set in shader
            gl.glUniform2fv(zNearFarUniformIndex, 1, zNearFar, 0);
            // Brightness correction
            if (brightnessModel.getChannelCount() == 3) {
                // Use a multichannel model
                ChannelColorModel c0 = brightnessModel.getChannel(0);
                ChannelColorModel c1 = brightnessModel.getChannel(1);
                ChannelColorModel c2 = brightnessModel.getChannel(2);
                float max0 = c0.getDataMax();
                // min
                gl.glUniform3fv(3, 1, new float[] {
                        c0.getBlackLevel()/max0, 
                        c1.getBlackLevel()/max0,
                        c2.getBlackLevel()/max0
                    }, 0);
                // max
                gl.glUniform3fv(4, 1, new float[] {
                        c0.getWhiteLevel()/max0, 
                        c1.getWhiteLevel()/max0,
                        c2.getWhiteLevel()/max0
                    }, 0);
                // gamma
                gl.glUniform3fv(5, 1, new float[] {
                        (float)c0.getGamma(), 
                        (float)c1.getGamma(),
                        (float)c2.getGamma()
                    }, 0);
                // visibility
                gl.glUniform3fv(6, 1, new float[] {
                        c0.isVisible() ? 1.0f : 0.0f,
                        c1.isVisible() ? 1.0f : 0.0f,
                        c2.isVisible() ? 1.0f : 0.0f
                    }, 0);
            }
            else {
                throw new UnsupportedOperationException("Unexpected number of color channels");
            }
            
            super.display(gl, camera, parentModelViewMatrix);
        }
        finally {
            camera.popInternalViewSlab();
            shader.unload(gl);
        }
    }
    
    @Override
    public void dispose(GL3 gl) {
        colorMapTexture.dispose(gl);
        shader.dispose(gl);
    }

    public ImageColorModel getBrightnessModel() {
        return brightnessModel;
    }

    public void setBrightnessModel(ImageColorModel brightnessModel) {
        this.brightnessModel = brightnessModel;
    }

    @Override
    public void setOpaqueDepthTexture(Texture2d opaqueDepthTexture) {
        // TODO: not yet used
    }

    @Override
    public void setRelativeSlabThickness(float zNear, float zFar) {
        zNearRelative = zNear;
        zFarRelative = zFar;
    }

}
