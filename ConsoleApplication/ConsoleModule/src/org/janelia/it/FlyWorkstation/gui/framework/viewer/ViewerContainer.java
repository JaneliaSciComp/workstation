package org.janelia.it.FlyWorkstation.gui.framework.viewer;

/**
 * A container for one or more viewers which supports maintaining an "active" viewer, and dynamic titles for viewers.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface ViewerContainer {

    /**
     * Set the given viewer pane as active.
     *
     * @param viewer
     */
    public void setActiveViewerPane(ViewerPane viewerPane);

    /**
     * Returns the active viewer pane which contains the active viewer.
     *
     * @return
     */
    public ViewerPane getActiveViewerPane();

}
