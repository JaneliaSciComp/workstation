package org.janelia.geometry3d;

/**
 *
 * @author Christopher Bruns
 */
public interface VolumeTextureMesh 
{
    // Matrix to help with efficient shader ray casting
    Matrix4 getTransformWorldToTexCoord();

    /**
     * 
     * @return finest resolution in scene units
     */
    float getMinResolution();
}
