package org.janelia.workstation.core.actions;

/**
 * Grants an object the ability to receive the ViewerContext.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface ViewerContextReceiver {

    void setViewerContext(ViewerContext viewerContext);

}
