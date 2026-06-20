package org.janelia.workstation.gui.large_volume_viewer;

import java.awt.Component;
import java.awt.Graphics;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesChooser;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.awt.GLJPanel;
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
