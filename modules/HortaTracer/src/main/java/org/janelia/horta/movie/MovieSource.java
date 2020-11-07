package org.janelia.horta.movie;

import org.janelia.horta.camera.Interpolator;
import org.janelia.geometry3d.ObservableInterface;

/**
 *
 * @author brunsc
 */
public interface MovieSource extends MovieRenderer
{
    ViewerState getViewerState();
    void setViewerState(ViewerState state); // For interactive display playback
    ObservableInterface getViewerStateUpdatedObservable();
    ViewerStateJsonDeserializer getStateDeserializer();
    String getViewerStateType();
    Interpolator<ViewerState> getDefaultInterpolator();
}
