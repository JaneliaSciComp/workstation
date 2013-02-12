package org.janelia.it.FlyWorkstation.gui.viewer3d.texture;

import org.janelia.it.FlyWorkstation.gui.viewer3d.VolumeDataAcceptor;

import java.nio.ByteBuffer;
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

    void setTextureData( ByteBuffer textureData );
    ByteBuffer getTextureData();

    int getSx();

    int getSy();

    int getSz();

    boolean isLoaded();

    void setLoaded(boolean loaded);

    boolean isInverted();

    void setInverted( boolean inverted );

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

    String getFilename();

    void setFilename( String filename );

    int getChannelCount();

    void setChannelCount( int channelCount );
}
