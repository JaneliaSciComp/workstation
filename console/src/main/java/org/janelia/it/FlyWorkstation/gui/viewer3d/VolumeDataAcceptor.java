package org.janelia.it.FlyWorkstation.gui.viewer3d;

import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataI;

import java.nio.ByteOrder;
import java.nio.IntBuffer;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 1/10/13
 * Time: 4:25 PM
 *
 * Things like volume bricks can have setters under this interface called, to accept data about volumes.
 */
public interface VolumeDataAcceptor {
    public enum TextureColorSpace {
        COLOR_SPACE_LINEAR, // R,G,B values are proportional to photons collected
        COLOR_SPACE_RGB, // R,G,B valuesonly.  No alpha.  Needed for id-to-color mapping.
        COLOR_SPACE_SRGB // R,G,B values are already gamma corrected for display on computer monitors
    };

    void setTextureData( TextureDataI textureData );

}
