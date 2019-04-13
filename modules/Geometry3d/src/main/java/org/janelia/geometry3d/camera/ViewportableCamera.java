package org.janelia.geometry3d.camera;

/**
 *
 * @author brunsc
 */
public interface ViewportableCamera {
    ConstViewport getViewport();
    void setViewport(ConstViewport viewport);
}
