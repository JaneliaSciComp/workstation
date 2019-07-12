package org.janelia.workstation.core.events.selection;

import org.janelia.workstation.core.actions.ViewerContext;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ViewerContextChangeEvent {

    private final Object sourceComponent;
    private final ViewerContext viewerContext;

    public ViewerContextChangeEvent(Object sourceComponent, ViewerContext viewerContext) {
        this.sourceComponent = sourceComponent;
        this.viewerContext = viewerContext;
    }

    public Object getSourceComponent() {
        return sourceComponent;
    }

    public ViewerContext getViewerContext() {
        return viewerContext;
    }
}
