package org.janelia.it.FlyWorkstation.gui.viewer3d;

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
        COLOR_SPACE_SRGB // R,G,B values are already gamma corrected for display on computer monitors
    };

    void setVolumeData(int sx, int sy, int sz, int[] rgbaValues);
    void setTextureColorSpace(TextureColorSpace colorSpace);
    void setVolumeMicrometers(double sx, double sy, double sz);
    void setVoxelMicrometers(double sx, double sy, double sz);

}
