package org.janelia.workstation.gui.large_volume_viewer;

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
