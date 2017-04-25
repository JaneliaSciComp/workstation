package org.janelia.it.workstation.gui.large_volume_viewer.annotation;

import javax.swing.JPanel;

import org.janelia.it.workstation.gui.large_volume_viewer.controller.ViewStateListener;

/**
 * base class for UI panels that get placed to the right of the main 2d view
 * in the LVV for various modes of tracing; it's pretty minimal, because
 * the class is really only an interface for dealing with communication
 * with the QuadViewUi on top of a panel; all the other connections are
 * done with subclasses of AnnotationManager
 */
public abstract class AnnotationPanel extends JPanel {

    /**
     * called to set up communication back with the QuadViewUi; the
     * QuadViewController is a ViewStateListener
     */
    public abstract void setViewStateListener(ViewStateListener listener);

}
