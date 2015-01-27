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
package org.janelia.scenewindow;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.awt.GLJPanel;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 *
 * @author brunsc
 */
public class GLJComponentFactory 
{
    // Creates a GL drawing surface in an OS appropriate manner
    // Creates a GLCanvas based display on Windows, where we might want
    // hardware stereo 3D to work.
    // Creates a GLJPanel based display on Mac, where GLCanvas behavior
    // is horrible.
    public static GLJComponent createGLJComponent(GLCapabilities capabilities) {
        if (false) {
            return new GLJPanelComponent(capabilities);
        }
        else {
            if (System.getProperty("os.name").startsWith("Mac"))
                return new GLJPanelComponent(capabilities);
            else 
                return new GLCanvasInJPanelComponent(capabilities);
        }
    }

    public static class GLCanvasInJPanelComponent extends JPanel
    implements GLJComponent 
    {
        private final Dimension tinySize = new Dimension(0, 0);
        private GLCanvas glCanvas;

        public GLCanvasInJPanelComponent(GLCapabilities capabilities) {
            super(new BorderLayout());
            glCanvas = new GLCanvas(capabilities);
            add(glCanvas, BorderLayout.CENTER);
            // This workaround avoids horrible GLCanvas resize behavior...
            glCanvas.addGLEventListener(new GLEventListener() {
                @Override
                public void init(GLAutoDrawable glad) {}
                @Override
                public void dispose(GLAutoDrawable glad) {}
                @Override
                public void display(GLAutoDrawable glad) {}
                @Override
                public void reshape(GLAutoDrawable glad, int i, int i1, int i2, int i3) {
                    // Avoid horrible GLCanvas resize behavior in Swing containers
                    // Thank you World Wind developers
                    glCanvas.setMinimumSize(tinySize);
                }
            });
        }

        @Override
        public Component getInnerComponent() {
            return glCanvas;
        }

        @Override
        public JComponent getOuterComponent() {
            return this;
        }

        @Override
        public GLAutoDrawable getGLAutoDrawable() {
            return glCanvas;
        }

        // For some reason, the GLCanvas seems to eat the mouse events before they
        // could reach the enclosing JPanel
        @Override
        public void addMouseMotionListener(MouseMotionListener mml) {
            glCanvas.addMouseMotionListener(mml);
        }
        @Override
        public void addMouseListener(MouseListener ml) {
            glCanvas.addMouseListener(ml);
        }
        @Override
        public void addMouseWheelListener(MouseWheelListener mwl) {
            glCanvas.addMouseWheelListener(mwl);
        }

    }

    
    /**
    * Thin wrapper around GLJPanel, to support common interface between 
    * GLJPanel and GLCanvas based displays.
    * 
    * @author brunsc
    */
   public static class GLJPanelComponent extends GLJPanel
   implements GLJComponent 
   {
       public GLJPanelComponent(GLCapabilities capabilities) {
           super(capabilities);
           // setSkipGLOrientationVerticalFlip(true); // Maybe GL_FRAMEBUFFER_SRGB could work now NOPE
       }

       @Override
       public Component getInnerComponent() {
           return this;
       }

       @Override
       public JComponent getOuterComponent() {
           return this;
       }

       @Override
       public GLAutoDrawable getGLAutoDrawable() {
           return this;
       }

   }

}
