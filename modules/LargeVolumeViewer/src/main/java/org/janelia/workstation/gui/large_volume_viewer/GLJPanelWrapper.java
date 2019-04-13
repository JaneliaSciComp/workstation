package org.janelia.workstation.gui.large_volume_viewer;

import java.awt.Component;
import java.awt.Graphics;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesChooser;
import javax.media.opengl.GLContext;
import javax.media.opengl.awt.GLJPanel;
import javax.swing.JComponent;

/**
 *
 * @author Christopher Bruns
 */
public class GLJPanelWrapper extends GLJPanel implements GLDrawableWrapper
{
    public GLJPanelWrapper(final GLCapabilities capabilities,
                             final GLCapabilitiesChooser chooser,
                             final GLContext sharedContext) 
    {
        super(capabilities, chooser);
        if (sharedContext != null)
            setSharedContext(sharedContext);
    }

    @Override
    public Component getInnerAwtComponent()
    {
        return this;
    }

    @Override
    public JComponent getOuterJComponent()
    {
        return this;
    }

    @Override
    public GLAutoDrawable getGLAutoDrawable()
    {
        return this;
    }

    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
    }

}
