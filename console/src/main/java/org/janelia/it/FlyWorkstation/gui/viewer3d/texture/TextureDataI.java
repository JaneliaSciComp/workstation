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

    void setTextureData( byte[] textureData );
    byte[] getTextureData();

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

    // "Coordinate Coverage" are the percentage of the texture that should be mapped to the 0..1.0 coordinates
    // used against textures in OpenGL.  It may be that 100% of the texture is used. It could also be, however,
    // that some padding onto-the-ends has happened during processing or fetch, such that <100% is the correct value.
    float[] getCoordCoverage();

    void setCoordCoverage( float[] coverage );

    /**
     * For voxel component format, this value is used for the glTexImage*D (3D in our case) calls.  Specifically,
     * the GLEnum type parameter.  This may be omitted if the rules for deducing it are in place and working for
     * this texture.  However, this explicit override may be used otherwise.
     *
     * @return a constant like INT_8_8_8_8 or null if not overridden.
     */
    Integer getExplicitVoxelComponentFormat();

    void setExplicitVoxelComponentFormat( int format );

}
