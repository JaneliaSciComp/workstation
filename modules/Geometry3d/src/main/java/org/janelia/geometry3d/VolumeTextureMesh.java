package org.janelia.geometry3d;

/**
 *
 * @author Christopher Bruns
 */
public interface VolumeTextureMesh 
{
    // Matrix to help with efficient shader ray casting
    public abstract Matrix4 getTransformWorldToTexCoord();

    /**
     * 
     * @return finest resolution in scene units
     */
    public float getMinResolution();
}
