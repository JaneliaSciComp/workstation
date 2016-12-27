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

package org.janelia.it.workstation.gui.large_volume_viewer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesChooser;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.awt.GLCanvas;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.ToolTipManager;

/**
 *
 * @author Christopher Bruns
 */
public class GLCanvasWrapper extends JPanel implements GLDrawableWrapper
{
    private final GLCanvas glCanvas;
    private final Dimension tinySize = new Dimension(0, 0);
    
    public GLCanvasWrapper(final GLCapabilities capabilities,
                             final GLCapabilitiesChooser chooser,
                             final GLContext sharedContext) 
    {
        super(new BorderLayout());
        glCanvas = new GLCanvas(capabilities);
        if (sharedContext != null) {
            glCanvas.setSharedContext(sharedContext);
        }
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

    // For some reason, the GLCanvas seems to eat the mouse events before they
    // could reach the enclosing JPanel
    @Override
    public void addMouseMotionListener(MouseMotionListener mml) {
        if (mml instanceof ToolTipManager)
            return; // AWT components cause errors with tool tips
        if (mml.getClass().getEnclosingClass() == ToolTipManager.class)
            return;
        glCanvas.addMouseMotionListener(mml);
    }
    @Override
    public void addMouseListener(MouseListener ml) {
        if (ml instanceof ToolTipManager)
            return; // AWT components cause errors with tool tips
        glCanvas.addMouseListener(ml);
    }
    @Override
    public void addMouseWheelListener(MouseWheelListener mwl) {
        glCanvas.addMouseWheelListener(mwl);
    }

    @Override
    public Component getInnerAwtComponent()
    {
        return glCanvas;
    }

    @Override
    public JComponent getOuterJComponent()
    {
        return this;
    }

    @Override
    public GLAutoDrawable getGLAutoDrawable()
    {
        return glCanvas;
    }
    
    @Override
    public void paint(Graphics g) { // 2
        super.paint(g);
    }

    @Override
    public void paintComponent(Graphics g) // 3
    {
        if (glCanvas != null) {
            glCanvas.display(); // important
        }
        super.paintComponent(g);
    }
    
    @Override
    public void repaint() { // 1
        super.repaint();
    }
    
    @Override
    public void update(Graphics g) { // never called from user code?
        super.update(g);
    }

}
