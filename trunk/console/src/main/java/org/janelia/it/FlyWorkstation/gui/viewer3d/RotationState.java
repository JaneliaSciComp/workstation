package org.janelia.it.FlyWorkstation.gui.viewer3d;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 2/15/13
 * Time: 9:53 AM
 *
 * Limiting interface to avoid up/down references between objects.  Implement this to provide a rotation value for
 * the display.
 */
public interface RotationState {
    Rotation getRotation();
}
