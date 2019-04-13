package org.janelia.geometry3d.camera;

/**
 *
 * @author brunsc
 */
public interface ShiftableCamera {
    // TODO: Do we notify listeners when viewpoint shifts?
    void pushFrustumShift(ConstFrustumShift shift);
    ConstFrustumShift popFrustumShift();
}
