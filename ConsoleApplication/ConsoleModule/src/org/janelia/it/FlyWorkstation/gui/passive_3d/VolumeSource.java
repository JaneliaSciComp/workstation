package org.janelia.it.FlyWorkstation.gui.passive_3d;

import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataI;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 7/31/13
 * Time: 3:46 PM
 *
 * Implement this to make a texture data for use in 3d volumes.
 */
public interface VolumeSource {
    void getVolume( VolumeAcceptor volumeListener ) throws Exception;

    /** Implement this to receive data found by this data source, when it becomes available. */
    public interface VolumeAcceptor {
        void accept( TextureDataI textureData );
    }

}
