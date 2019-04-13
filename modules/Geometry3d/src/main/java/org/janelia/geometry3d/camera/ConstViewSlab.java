package org.janelia.geometry3d.camera;

/**
 *
 * @author brunsc
 */
public interface ConstViewSlab 
{
    float getzNearRelative(); // Position of view camera near clipping plane, as a proportion of the distance to the focal point.
    float getzFarRelative(); // Position of view camera far clipping plane, as a proportion of the distance to the focal point.
}
