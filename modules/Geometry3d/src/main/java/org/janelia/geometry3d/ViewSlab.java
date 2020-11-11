package org.janelia.geometry3d;

import org.janelia.geometry3d.camera.ConstViewSlab;

/**
 *
 * @author brunsc
 */
public interface ViewSlab extends ConstViewSlab {

    ComposableObservable getChangeObservable();

    void setzFarRelative(float zFarRelative);

    void setzNearRelative(float zNearRelative);
    
}
