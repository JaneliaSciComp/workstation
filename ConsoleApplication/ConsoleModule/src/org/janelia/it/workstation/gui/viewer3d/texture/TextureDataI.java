package org.janelia.it.workstation.gui.viewer3d.texture;

import org.janelia.it.workstation.gui.viewer3d.masking.VolumeDataI;
import org.janelia.it.workstation.gui.viewer3d.renderable.RenderableBean;
import org.janelia.it.workstation.gui.viewer3d.VolumeDataAcceptor;

import java.nio.ByteOrder;
import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 1/23/13
 * Time: 10:23 AM
 *
 * Implement this to make an object holding/conveying all texture data about one set of texture bytes.
 */
public interface TextureDataI {
    public static final int UNSET_VALUE = -1;

    void setTextureData( VolumeDataI textureData );
    VolumeDataI getTextureData();

    int getSx();

    int getSy();

    int getSz();

    boolean isInverted();

    void setInverted( boolean inverted );

    void setSx(int sx);

    void setSy(int sy);

    void setSz(int sz);

    VolumeDataAcceptor.TextureColorSpace getColorSpace();

    void setColorSpace(VolumeDataAcceptor.TextureColorSpace colorSpace);

    /** Fill in this value with a valid inter method from OpenGL, such as Linear or Nearest. */
    int getInterpolationMethod();
    void setInterpolationMethod( int interpolationMethod );

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
    Integer getExplicitVoxelComponentType();

    /**
     * According to documents at
     * @see https://www.opengl.org/sdk/docs/man3/xhtml/glTexImage3D.xml
     * this is the set of possible values.
     * 
     * GL_UNSIGNED_BYTE, GL_BYTE, GL_UNSIGNED_SHORT, GL_SHORT, GL_UNSIGNED_INT,
     * GL_INT, GL_FLOAT, GL_UNSIGNED_BYTE_3_3_2, GL_UNSIGNED_BYTE_2_3_3_REV,
     * GL_UNSIGNED_SHORT_5_6_5, GL_UNSIGNED_SHORT_5_6_5_REV,
     * GL_UNSIGNED_SHORT_4_4_4_4, GL_UNSIGNED_SHORT_4_4_4_4_REV,
     * GL_UNSIGNED_SHORT_5_5_5_1, GL_UNSIGNED_SHORT_1_5_5_5_REV,
     * GL_UNSIGNED_INT_8_8_8_8, GL_UNSIGNED_INT_8_8_8_8_REV,
     * GL_UNSIGNED_INT_10_10_10_2, and GL_UNSIGNED_INT_2_10_10_10_REV.
     *
     * @param format 
     */
    void setExplicitVoxelComponentType(int format);

    /**
     * The "internal format" is used at "glTexImage*D" (3D in our case) calls.  Specifically,
     * the GLEnum "format" parameter.   This may be omitted if the rules for deducing it are in place and working for
     * this texture.  However, this explicit override may be used otherwise.  Here's the blurb from OpenGL docs:
     * internalFormat
     *
     Specifies the number of color components in the texture.
     Must be 1, 2, 3, or 4, or one of the following symbolic constants:
     GL_ALPHA,
     GL_ALPHA4,
     GL_ALPHA8,
     GL_ALPHA12,
     GL_ALPHA16,
     GL_COMPRESSED_ALPHA,
     GL_COMPRESSED_LUMINANCE,
     GL_COMPRESSED_LUMINANCE_ALPHA,
     GL_COMPRESSED_INTENSITY,
     GL_COMPRESSED_RGB,
     GL_COMPRESSED_RGBA,
     GL_LUMINANCE,
     GL_LUMINANCE4,
     GL_LUMINANCE8,
     GL_LUMINANCE12,
     GL_LUMINANCE16,
     GL_LUMINANCE_ALPHA,
     GL_LUMINANCE4_ALPHA4,
     GL_LUMINANCE6_ALPHA2,
     GL_LUMINANCE8_ALPHA8,
     GL_LUMINANCE12_ALPHA4,
     GL_LUMINANCE12_ALPHA12,
     GL_LUMINANCE16_ALPHA16,
     GL_INTENSITY,
     GL_INTENSITY4,
     GL_INTENSITY8,
     GL_INTENSITY12,
     GL_INTENSITY16,
     GL_R3_G3_B2,
     GL_RGB,
     GL_RGB4,
     GL_RGB5,
     GL_RGB8,
     GL_RGB10,
     GL_RGB12,
     GL_RGB16,
     GL_RGBA,
     GL_RGBA2,
     GL_RGBA4,
     GL_RGB5_A1,
     GL_RGBA8,
     GL_RGB10_A2,
     GL_RGBA12,
     GL_RGBA16,
     GL_SLUMINANCE,
     GL_SLUMINANCE8,
     GL_SLUMINANCE_ALPHA,
     GL_SLUMINANCE8_ALPHA8,
     GL_SRGB,
     GL_SRGB8,
     GL_SRGB_ALPHA, or
     GL_SRGB8_ALPHA8.
     *
     * @return one of the above values, as set in the setter.
     */
    Integer getExplicitInternalFormat();

    void setExplicitInternalFormat( Integer format );

    /**
     * Returns one of these values, which specifies how to 
     * interpret the bytes of the voxel value stored in the texture.
     * 
     * GL_RGBA, GL_BGRA, GL_RED, GL_RG, GL_RGB, GL_BGR
     * 
     * @return
     */
    Integer getExplicitVoxelComponentOrder();
    void setExplicitVoxelComponentOrder( Integer order );

    void setRenderables( Collection<RenderableBean> renderables );
    Collection<RenderableBean> getRenderables();

    /**
     * Transforms may be required against the coordinates, to use them properly.
     * @param transformMatrix to multiply by each vertex coordinate.
     */
    void setTransformMatrix( float[] transformMatrix );
    float[] getTransformMatrix();
}
