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
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GL3;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.MeshGeometry;
import org.janelia.geometry3d.Viewport;
import org.janelia.gltools.BasicShaderProgram;
import org.janelia.gltools.MeshActor;
import org.janelia.gltools.ShaderProgram;
import org.janelia.gltools.ShaderStep;
import org.janelia.gltools.material.BasicMaterial;
import org.janelia.gltools.texture.Texture2d;
import org.openide.util.Exceptions;

/**
 *
 * @author Christopher Bruns
 */
public class CenterCrossHairActor extends MeshActor {
    private int heightPixels = 16;
    private int widthPixels = 16;
    private final Matrix4 crossHairTransform = new Matrix4();

    public CenterCrossHairActor() {
        super(new CrossHairGeometry(), 
                new CrossHairMaterial(), 
                null);
    }

    @Override
    public void display(GL3 gl, AbstractCamera camera, Matrix4 parentModelViewMatrix)
    {
        if (! isVisible())
            return;
        Viewport vp = camera.getViewport();
        int vw = vp.getWidthPixels();
        int vh = vp.getHeightPixels();
        setMatrixForBounds(crossHairTransform,
                vw/2 - widthPixels/2, vh/2 - heightPixels/2,
                widthPixels, heightPixels,
                vw, vh);
        gl.glDisable(GL3.GL_DEPTH_TEST);
        ((CrossHairMaterial)getMaterial()).crossHairTexture.bind(gl);
        super.display(gl, camera, crossHairTransform);
        ((CrossHairMaterial)getMaterial()).crossHairTexture.unbind(gl);
    }

    private static void setMatrixForBounds(Matrix4 matrix, 
            int ox, int oy, 
            int w, int h,
            int vw, int vh) 
    {
        matrix.set(
                2*w/(float)vw, 0, 0, 0, // scale X
                0, 2*h/(float)vh, 0, 0, // scale Y
                0, 0, 1, 0,
                (ox - vw/2f)/(0.5f * vw), // offset X
                (oy - vh/2)/(0.5f * vh), // offset Y
                0, 1);
        
    }

    private static class CrossHairGeometry extends MeshGeometry {

        public CrossHairGeometry() {
            addVertex( 0 , 0 , 0.5f);
            addVertex( 1 , 0 , 0.5f);
            addVertex( 1 , 1 , 0.5f);
            addVertex( 0 , 1 , 0.5f);
            addFace(new int[] 
                    {0, 1, 2, 3}
            );
        }
    }

    private static class CrossHairMaterial extends BasicMaterial {
        private Texture2d crossHairTexture = new Texture2d();
        private int crossHairTextureIndex = -1;

        public CrossHairMaterial() {
            shaderProgram = new BasicShaderProgram() {
                ShaderProgram init() {
                    getShaderSteps().add(new ShaderStep(GL2ES2.GL_VERTEX_SHADER,
                            "#version 330 \n"
                          + "uniform mat4 modelViewMatrix = mat4(1); \n"
                          + "uniform mat4 projectionMatrix = mat4(1); \n"
                          + "in vec3 position; \n"
                          + "out vec2 texCoord; \n"
                          + "void main() { \n"
                          + "  gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1); \n"
                          + "  texCoord = vec2(position.x, 1-position.y); \n"
                          + "} \n"
                          + " \n"
                    ));
                    getShaderSteps().add(new ShaderStep(GL2ES2.GL_FRAGMENT_SHADER,
                            "#version 330 \n"
                          + "uniform sampler2D crossHairTexture; \n"
                          + "uniform vec4 barColor = vec4(0, 0, 0, 0.5); \n"
                          + "in vec2 texCoord; \n"
                          + "out vec4 fragColor; \n"
                          + "void main() { \n"
                          + "  vec4 c = texture(crossHairTexture, texCoord); \n"
                          + "  // Additional dimming to alpha \n"
                          + "  fragColor = vec4(c.rgb, 0.25*c.a); \n"
                          + "} \n"
                          + " \n"
                    ));
                    return this;
                }
            }.init();

            BufferedImage crossHairImage = null;
            try {
                crossHairImage = ImageIO.read(
                        getClass().getResourceAsStream(
                                "/org/janelia/horta/images/"
                                + "center_crosshair_darker.png"));
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
            crossHairTexture.loadFromBufferedImage(crossHairImage);
            crossHairTexture.setGenerateMipmaps(false);
            crossHairTexture.setMinFilter(GL3.GL_NEAREST);
            crossHairTexture.setMagFilter(GL3.GL_NEAREST);
        }
        
        @Override
        public boolean usesNormals() {
            return false;
        }

        @Override
        public void load(GL3 gl, AbstractCamera camera) {
            if (crossHairTextureIndex == -1) 
                init(gl);
            super.load(gl, camera);
            int crossHairTextureUnit = 0;
            // crossHairTexture.bind(gl, crossHairTextureUnit);
            gl.glUniform1i(crossHairTextureIndex, crossHairTextureUnit);
            gl.glEnable(GL3.GL_BLEND);
            gl.glBlendFunc (GL3.GL_SRC_ALPHA, GL3.GL_ONE_MINUS_SRC_ALPHA);
        }
        
        @Override
        protected void displayWithMatrices(
                    GL3 gl, 
                    MeshActor mesh, 
                    AbstractCamera camera,
                    Matrix4 modelViewMatrix) 
        {
            if (modelViewMatrix != null) {
                gl.glUniformMatrix4fv(modelViewIndex, 1, false, modelViewMatrix.asArray(), 0);     
            }
            displayNoMatrices(gl, mesh, camera, modelViewMatrix);
        }

        @Override
        public void dispose(GL3 gl) {
            crossHairTexture.dispose(gl);
            super.dispose(gl);
        }
        
        @Override
        public void init(GL3 gl) {
            super.init(gl);
            crossHairTextureIndex = gl.glGetUniformLocation(
                    shaderProgram.getProgramHandle(), 
                    "crossHairTexture");
            if (crossHairTexture != null)
                crossHairTexture.init(gl);
        }       
    }
    
}
