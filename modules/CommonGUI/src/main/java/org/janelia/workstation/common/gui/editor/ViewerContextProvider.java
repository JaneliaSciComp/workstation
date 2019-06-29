package org.janelia.workstation.common.gui.editor;

import org.janelia.workstation.core.actions.ViewerContext;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface ViewerContextProvider<T, S> {

    ViewerContext<T, S> getViewerContext();
}
