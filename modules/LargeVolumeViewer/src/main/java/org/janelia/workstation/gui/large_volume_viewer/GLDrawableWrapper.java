package org.janelia.workstation.gui.large_volume_viewer;

import java.awt.Component;
import java.awt.Graphics;
import javax.media.opengl.GLAutoDrawable;
import javax.swing.JComponent;

/**
 * Intended to provide a common interface for GLJPanel and GLCanvas
 * @author Christopher Bruns
 */
public interface GLDrawableWrapper
{
    Component getInnerAwtComponent();
    JComponent getOuterJComponent();
    GLAutoDrawable getGLAutoDrawable();
    void paintComponent(Graphics g); // expose Component protected method as public
    void repaint();
}
