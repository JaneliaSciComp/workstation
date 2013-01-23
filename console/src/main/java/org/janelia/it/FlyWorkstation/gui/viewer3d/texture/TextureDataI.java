package org.janelia.it.FlyWorkstation.gui.viewer3d.texture;

import org.janelia.it.FlyWorkstation.gui.viewer3d.VolumeDataAcceptor;

import java.nio.ByteOrder;
import java.nio.IntBuffer;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 1/23/13
 * Time: 10:23 AM
 *
 * Implement this to make an object holding/conveying all texture data about one input file.
 */
public interface TextureDataI {
    void setMaskData( IntBuffer maskData, int sx, int sy, int sz );

    IntBuffer getMaskData();

    int getSx();

    int getSy();

    int getSz();

    boolean isLoaded();

    void setLoaded(boolean loaded);

    void setMaskData(IntBuffer maskData);

    void setSx(int sx);

    void setSy(int sy);

    void setSz(int sz);

    VolumeDataAcceptor.TextureColorSpace getColorSpace();

    void setColorSpace(VolumeDataAcceptor.TextureColorSpace colorSpace);

    Double[] getVolumeMicrometers();

    void setVolumeMicrometers(Double[] volumeMicrometers);

    Double[] getVoxelMicrometers();

    void setVoxelMicrometers(Double[] voxelMicrometers);

    String getHeader();

    void setHeader(String header);

    ByteOrder getByteOrder();

    void setByteOrder(ByteOrder byteOrder);

    int getPixelByteCount();

    void setPixelByteCount(int pixelByteCount);
}
