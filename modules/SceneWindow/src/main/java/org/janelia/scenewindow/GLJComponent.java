
package org.janelia.scenewindow;

import java.awt.Component;
import java.awt.event.ActionListener;
import javax.media.opengl.GLAutoDrawable;
import javax.swing.JComponent;

/**
 * Common interface for both GLJPanel and GLCanvas based render surfaces
 * @author brunsc
 */
public interface GLJComponent 
{
    JComponent getOuterComponent();
    Component getInnerComponent();
    GLAutoDrawable getGLAutoDrawable();
    void setControlsVisibility(boolean visible);
    void addPlayForwardListener (ActionListener listener);
    void addPlayReverseListener (ActionListener listener);
    void addPauseListener (ActionListener listener);
}
