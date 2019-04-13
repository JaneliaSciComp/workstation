package org.janelia.geometry3d.camera;

import org.janelia.geometry3d.ConstVector3;

/**
 *
 * @author brunsc
 */
public interface ConstVantage {
    float getSceneUnitsPerViewportHeight();
    ConstVector3 getFocusAsVector3();
    ConstRotation getRotation();
    ConstVector3 getUpInWorld();
}
