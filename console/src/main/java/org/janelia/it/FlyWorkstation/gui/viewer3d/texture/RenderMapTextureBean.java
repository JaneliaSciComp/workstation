package org.janelia.it.FlyWorkstation.gui.viewer3d.texture;

import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.RenderMappingI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.renderable.RenderableBean;
import org.janelia.it.FlyWorkstation.gui.viewer3d.VolumeDataAcceptor;

import javax.media.opengl.GL2;
import java.nio.ByteOrder;
import java.util.Collection;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 1/31/13
 * Time: 11:17 AM
 *
 * Uploadable texture representation, with just enough information to map ids represented as luminances in another
 * texture, to 3-int color values.
 */
public class RenderMapTextureBean implements TextureDataI {

    private static final int BYTES_PER_ENTRY = 4;
    private static final int MAP_SIZE = 65536;
    private static final int MAX_COORD_SETS = 192;   // This number yields end Y value divisible by 4.
    private static final int ENTRIES_PER_COORD_SET = 6;
    private static final int BYTES_PER_COORD_SET = ENTRIES_PER_COORD_SET * BYTES_PER_ENTRY;
    private static final String HEADER = "Map of Colors to Neuron Fragment Numbers and Crop Coord Sets";
    private static final int LINE_WIDTH = 256;

    private RenderMappingI renderMapping;
    private Collection<float[]> cropCoordCollection;
    //private byte[] mapData;
    private boolean inverted = false; // Default probably carries the day.
    private Integer voxelComponentFormat = GL2.GL_UNSIGNED_INT_8_8_8_8_REV;
    private Collection<RenderableBean> renderables;

    private int interpolationMethod = GL2.GL_NEAREST;
    private int voxelComponentOrder = GL2.GL_RGBA;

    /**
     * This implementation makes a big array of 64K * 3, to accommodate any possible neuron fragment number's
     * three colors.  It is wasteful in space, but far smaller than most uploaded textures.  It may be possible
     * to pack this into far smaller area, but with the sacrifice of processing time.
     */
    public void setMapping( RenderMappingI renderMapping ) {
        this.renderMapping = renderMapping;
    }

    /**
     * Setting crop coords for the byte array. These need to be de-normalized to non 0..1 values.
     *
     * @param cropCoordCollection collection of 6-float axial position delimiters. 2 for each 3D axis.
     */
    public void setCropCoords( Collection<float[]> cropCoordCollection ) {
        this.cropCoordCollection = cropCoordCollection;
    }

    @Override
    public void setTextureData(byte[] textureData) {
        // Ignored.
    }

    @Override
    public byte[] getTextureData() {
        Map<Integer,byte[]> renderingMap = renderMapping.getMapping();
        if ( renderingMap == null || renderingMap.size() > MAP_SIZE ) {
            throw new IllegalArgumentException("Invalid inputs for render mapping");
        }

        byte[] rawMap = new byte[ getRawBufferSize() ];
        for ( Integer neuronNumber: renderingMap.keySet() ) {
            byte[] rendition = renderingMap.get( neuronNumber );
            if ( rendition.length != 4 ) {
                throw new IllegalArgumentException( "Invalid size of RGB color map target.  Must be 4." );
            }
            int entryOffset = neuronNumber * BYTES_PER_ENTRY;
            for ( int i = 0; i < BYTES_PER_ENTRY; i++ ) {
                rawMap[ entryOffset + i ] = rendition[ i ];
            }
        }

        // Need de-normalized as-int values in the crop coords.
        if ( cropCoordCollection != null ) {
            if ( cropCoordCollection.size() > MAX_COORD_SETS ) {
                throw new IllegalArgumentException("Invalid inputs for crop coordinate sets");
            }

            int nextCropBoxOffset = BYTES_PER_ENTRY * MAP_SIZE;
            for ( float[] cropCoords: cropCoordCollection ) {
                // These are guaranteed non-fractional.
                for ( int i = 0; i < cropCoords.length; i++ ) {
                    int iValue = (int) cropCoords[ i ];
                    for ( int j = 0; j < Integer.SIZE; j++ ) {
                        rawMap[ nextCropBoxOffset + j + (cropCoords.length * i) ] =
                                (byte)(iValue >>> ( 24 - (8 * j) ) & 0xff);
                    }
                }
                nextCropBoxOffset += BYTES_PER_COORD_SET;
            }
        }

        return rawMap;
    }

    //  The size of the texture data will be 2**8 * 2**8 or 2**16: same as 65535, and then add enough 256x4 lines
    //  to include the selection coordinate boxes.
    @Override
    public int getSx() {
        return LINE_WIDTH;
    }

    @Override
    public int getSy() {
        return getRawBufferSize() / BYTES_PER_ENTRY / LINE_WIDTH;
    }

    @Override
    public int getSz() {
        return 1;
    }

    @Override
    public void setSx(int sx) {
    }

    @Override
    public void setSy(int sy) {
    }

    @Override
    public void setSz(int sz) {
    }

    @Override
    public VolumeDataAcceptor.TextureColorSpace getColorSpace() {
        return VolumeDataAcceptor.TextureColorSpace.COLOR_SPACE_SRGB;
    }

    @Override
    public void setColorSpace(VolumeDataAcceptor.TextureColorSpace colorSpace) {
    }

    @Override
    public Double[] getVolumeMicrometers() {
        return new Double[] { 0.0, 0.0, 0.0 };
    }

    @Override
    public void setVolumeMicrometers(Double[] volumeMicrometers) {
    }

    @Override
    public Double[] getVoxelMicrometers() {
        return new Double[] { 0.0, 0.0, 0.0 };
    }

    @Override
    public void setVoxelMicrometers(Double[] voxelMicrometers) {
    }

    @Override
    public String getHeader() {
        return HEADER;
    }

    @Override
    public void setHeader(String header) {
    }

    @Override
    public ByteOrder getByteOrder() {
        return ByteOrder.LITTLE_ENDIAN;
    }

    @Override
    public void setByteOrder(ByteOrder byteOrder) {
    }

    @Override
    public int getPixelByteCount() {
        return BYTES_PER_ENTRY;  // Bytes
    }

    @Override
    public void setPixelByteCount(int pixelByteCount) {
    }

    @Override
    public String getFilename() {
        return "Renderable to Appearance Map";
    }

    @Override
    public void setFilename(String filename) {
    }

    @Override
    public int getChannelCount() {
        return 3;
    }

    @Override
    public void setChannelCount(int channelCount) {
    }

    public boolean isInverted() {
        return inverted;
    }

    public void setInverted(boolean inverted) {
        this.inverted = inverted;
    }

    @Override
    public float[] getCoordCoverage() {
        return new float[] { 1.0f, 1.0f, 1.0f };
    }

    @Override
    public void setCoordCoverage(float[] coverage) {
        // Do nothing.  Special texture never has less than 100% coord-to-edge ratio.
    }

    @Override
    public Integer getExplicitVoxelComponentType() {
        return voxelComponentFormat;
    }

    @Override
    public void setExplicitVoxelComponentType(int format) {
        this.voxelComponentFormat = format;
    }

    @Override
    public void setRenderables(Collection<RenderableBean> renderables) {
        this.renderables = renderables;
    }

    @Override
    public Collection<RenderableBean> getRenderables() {
        return renderables;
    }

    @Override
    public int getInterpolationMethod() {
        return interpolationMethod;
    }

    @Override
    public void setInterpolationMethod(int interpolationMethod) {
        this.interpolationMethod = interpolationMethod;
    }

    @Override
    public Integer getExplicitInternalFormat() {
        return TextureDataI.UNSET_VALUE;
    }

    @Override
    public void setExplicitInternalFormat( Integer format ) {
        throw new IllegalStateException( "If this is being called, this class is being used with wrong intent." );
    }

    @Override
    public Integer getExplicitVoxelComponentOrder() {
        return voxelComponentOrder;
    }

    @Override
    public void setExplicitVoxelComponentOrder(Integer voxelComponentOrder) {
        this.voxelComponentOrder = voxelComponentOrder;
    }

    private int getRawBufferSize() {
        System.out.println("Returning raw buffer size of " + (MAP_SIZE * BYTES_PER_ENTRY + roundUp256( MAX_COORD_SETS * ENTRIES_PER_COORD_SET * BYTES_PER_ENTRY )));
        return MAP_SIZE * BYTES_PER_ENTRY + roundUp256( MAX_COORD_SETS * ENTRIES_PER_COORD_SET * BYTES_PER_ENTRY );
    }

    private int roundUp256( int value ) {
        if ( value % 256 == 0 ) {
            return value;
        }
        else {
            System.out.println("Returning round-up of " + (((value / 256) + 1) * 256));
            return ((value / 256) + 1) * 256;
        }
    }
}

