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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.text.NumberFormat;
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
import org.janelia.gltools.texture.Texture2d;
import org.janelia.gltools.material.BasicMaterial;

/**
 *
 * @author Christopher Bruns
 */
public class ScaleBar extends MeshActor {
    // Adjustable parameters
    private int heightPixels = 4;
    private int maxWidthPixels = Integer.MAX_VALUE;
    private float maxWidthRelative = 0.15f; // relative to viewport width
    private int borderWidthPixels = 2;
    private Color foregroundColor = Color.WHITE;
    private Color backgroundColor = new Color(0.0f, 0.0f, 0.0f, 0.2f);
    // Negative pad means right/bottom
    private int horizontalPadPixels = -20;
    private int verticalPadPixels = 20;
    
    // Specify what sort of initial digits we want to see for the scale bar.
    private final double[] roundNumbers = new double[] 
            // {1, 2, 5}; // Max difference 2.5X
            {1, 1.5f, 2, 3, 4, 5, 7.5}; // Max difference 1.5X
    
    // Cached graphics parameters
    private final Matrix4 barTransform = new Matrix4();
    private final Matrix4 barBorderTransform = new Matrix4();
    private final Matrix4 labelTransform = new Matrix4();
    private float[] barColor = new float[] {
        foregroundColor.getRed()/255f,
        foregroundColor.getGreen()/255f,
        foregroundColor.getBlue()/255f,
        foregroundColor.getAlpha()/255f
    };
    private float[] borderColor = new float[] {
        backgroundColor.getRed()/255f,
        backgroundColor.getGreen()/255f,
        backgroundColor.getBlue()/255f,
        backgroundColor.getAlpha()/255f
    };
    private float[] nothingColor = new float[] {1, 0, 1, 0};
    
    private BufferedImage labelImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    private String labelString = "  ";
    private NumberFormat labelFormat = new DecimalFormat();
    private final Font font = new Font("Arial", Font.BOLD, 18);
    private int labelWidth = 1;
    private int labelHeight = 1;

    public ScaleBar() {
        super(new ScaleBarGeometry(), 
              new ScaleBarMaterial(),
              null);
        labelFormat.setGroupingUsed(false); // Avoid the thousands comma
        updateLabelString("foo");
    }
    
    private void updateLabelString(String newLabelString) {
        labelString = newLabelString;
        // Regenerate label image
        /*
           Because font metrics is based on a graphics context, we need to use
           a small, temporary image so we can ascertain the width and height
           of the final image
         */
        Graphics2D g2d = labelImage.createGraphics();
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();
        labelWidth = fm.stringWidth(labelString);
        labelHeight = fm.getHeight();
        g2d.dispose();
        // Now create the real label image
        labelImage = new BufferedImage(labelWidth, labelHeight, BufferedImage.TYPE_INT_ARGB);
        g2d = labelImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2d.setFont(font);
        fm = g2d.getFontMetrics();
        g2d.setBackground(backgroundColor);
        g2d.clearRect(0, 0, labelWidth, labelHeight);
        g2d.setColor(foregroundColor);
        g2d.drawString(labelString, 0, fm.getAscent());
        g2d.dispose();
        ((ScaleBarMaterial)getMaterial()).loadFromImage(labelImage);
        // System.out.println(labelString);
    }
    
    @Override
    public void display(GL3 gl, AbstractCamera camera, Matrix4 parentModelViewMatrix)
    {
        // Paint label with length scale text, e.g. "1 um"
        
        displayBar(camera, gl);        
    }

    protected void displayBar(AbstractCamera camera, GL3 gl) {
        // Compute the total scale bar rectangle height in pixels
        int h = heightPixels + 2 * borderWidthPixels;
        
        // Compute the maximum scalebar width in pixels
        Viewport vp = camera.getViewport();
        int vw = vp.getWidthPixels();
        int vh = vp.getHeightPixels();
        float maxRelPixels = vw * maxWidthRelative;
        float maxW = Math.min(maxRelPixels, maxWidthPixels);
        
        // Trim width down, to get a round number.
        // Using logarithm to get best number
        double umPerPixel = camera.getVantage().getSceneUnitsPerViewportHeight()
                / vh;
        double maxBarWidth = maxW * umPerPixel;
        double barValue = 0;
        // Choose the largest round number that fits in the available space
        for (double round : roundNumbers) {
            int exponent = (int) Math.floor(Math.log10(maxBarWidth/round));
            double testBarValue = (round * Math.pow(10, exponent));
            if (testBarValue > barValue)
                barValue = testBarValue;
        }
        int w = (int)(barValue / umPerPixel);
        
        // Create scale bar label with value and units
        // System.out.println("Scale bar value = " + barValue
        //         + "; bar width = " + w + " pixels");
        String newLabelString = labelFormat.format(barValue) + " \u00B5" + "m";
        // Only regenerate the label image if the label has changed
        if (! newLabelString.equals(labelString)) {
            updateLabelString(newLabelString);
        }

        // Compute bounds of on-screen box containing scale bar
        int leftPixel = horizontalPadPixels;
        if (horizontalPadPixels < 0) 
            leftPixel = vw + horizontalPadPixels - w;
        int bottomPixel = verticalPadPixels;
        if (verticalPadPixels < 0)
            bottomPixel = vh + verticalPadPixels - h;

        // Encode position on screen into modelView matrix
        setMatrixForBounds(barBorderTransform, leftPixel, 
                bottomPixel + labelHeight, 
                w, h, vw, vh);

        setMatrixForBounds(barTransform, 
                leftPixel + borderWidthPixels, 
                bottomPixel + borderWidthPixels + labelHeight, 
                w - 2*borderWidthPixels, h - 2*borderWidthPixels, 
                vw, vh);
        
        ((ScaleBarMaterial)getMaterial()).barColor = borderColor;
        super.display(gl, camera, barBorderTransform);

        ((ScaleBarMaterial)getMaterial()).barColor = barColor;
        super.display(gl, camera, barTransform);
        
        // Paint label
        setMatrixForBounds(labelTransform, 
                leftPixel + w - labelWidth - 20, 
                bottomPixel, 
                labelWidth, labelHeight, vw, vh);
        // Set alpha to zero, to force use of labelTexture
        ((ScaleBarMaterial)getMaterial()).barColor = nothingColor;
        ((ScaleBarMaterial)getMaterial()).labelTexture.bind(gl);
        super.display(gl, camera, labelTransform);
        ((ScaleBarMaterial)getMaterial()).labelTexture.unbind(gl);
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
    
    static class ScaleBarGeometry extends MeshGeometry {
        ScaleBarGeometry() {
            addVertex( 0 , 0 , 0.5f);
            addVertex( 1 , 0 , 0.5f);
            addVertex( 1 , 1 , 0.5f);
            addVertex( 0 , 1 , 0.5f);
            addFace(new int[] 
                    {0, 1, 2, 3}
            );
        }
    }

    static class ScaleBarMaterial extends BasicMaterial {
        private int labelTextureIndex = -1;
        private Texture2d labelTexture;
        private Texture2d oldTexture;
        private int barColorIndex = -1;
        private float[] barColor = new float[] {0, 1, 0, 1};

        ScaleBarMaterial() {
            labelTexture = new Texture2d();
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
                          + "uniform sampler2D labelTexture; \n"
                          + "uniform vec4 barColor = vec4(0, 0, 0, 0.5); \n"
                          + "in vec2 texCoord; \n"
                          + "out vec4 fragColor; \n"
                          + "void main() { \n"
                          + "  if (barColor.a == 0) \n"
                          + "    fragColor = texture(labelTexture, texCoord); \n"
                          + "  else \n"
                          + "    fragColor = barColor; \n"
                          + "} \n"
                          + " \n"
                    ));
                    return this;
                }
            }.init();
        }
        
        @Override
        public boolean usesNormals() {
            return false;
        }

        @Override
        public void load(GL3 gl, AbstractCamera camera) {
            if (labelTextureIndex == -1) 
                init(gl);
            super.load(gl, camera);
            int labelTextureUnit = 0;
            // labelTexture.bind(gl, labelTextureUnit);
            gl.glUniform1i(labelTextureIndex, labelTextureUnit);
            gl.glUniform4fv(barColorIndex, 1, barColor, 0);
            if (oldTexture != null) {
                oldTexture.dispose(gl);
                oldTexture = null;
            }
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
            labelTexture.dispose(gl);
            super.dispose(gl);
        }
        
        @Override
        public void init(GL3 gl) {
            super.init(gl);
            labelTextureIndex = gl.glGetUniformLocation(
                    shaderProgram.getProgramHandle(), 
                    "labelTexture");
            barColorIndex = gl.glGetUniformLocation(
                    shaderProgram.getProgramHandle(), 
                    "barColor");
            if (labelTexture != null)
                labelTexture.init(gl);
        }
        
        public void loadFromImage(BufferedImage img) {
            oldTexture = labelTexture;
            labelTexture = new Texture2d();
            labelTexture.loadFromBufferedImage(img);
        }        
    }    
}
