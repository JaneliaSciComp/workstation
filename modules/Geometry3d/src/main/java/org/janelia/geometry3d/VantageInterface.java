package org.janelia.geometry3d;

/**
 *
 * @author Christopher Bruns
 */
public interface VantageInterface extends ObservableInterface
{
    // Zoom
    float getSceneUnitsPerViewportHeight();
    boolean setSceneUnitsPerViewportHeight(float zoom);
    
    // Center
    boolean setFocus(float x, float y, float z);
    float[] getFocus();
    
    // Rotation
    // TODO: How to best express 3D rotation in basic Java types?
}
