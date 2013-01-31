package org.janelia.it.FlyWorkstation.gui.viewer3d.texture;

import org.janelia.it.FlyWorkstation.gui.viewer3d.VolumeDataAcceptor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
public class ColorMapTextureBean implements TextureDataI {

    private static final int BYTES_PER_ENTRY = 4;
    private ByteBuffer mapData;

    /**
     * This implementation makes a big array of 64K * 3, to accommodate any possible neuron fragment number's
     * three colors.  It is wasteful in space, but far smaller than most uploaded textures.  It may be possible
     * to pack this into far smaller area, but with the sacrifice of processing time.
     *
     * @param colorMap neuron number from label file, vs 3-byte color array (RGB).
     */
    public void setMapping( Map<Integer,byte[]> colorMap ) {
        if ( colorMap == null || colorMap.size() > 65535 ) {
            throw new IllegalArgumentException("Invalid inputs for color mapping");
        }

        byte[] rawMap = new byte[ 65536 * BYTES_PER_ENTRY ];
        for ( Integer neuronNumber: colorMap.keySet() ) {
            byte[] rgb = colorMap.get( neuronNumber );
            if ( rgb.length != 3 ) {
                throw new IllegalArgumentException( "Invalid size of RGB color map target.  Must be 3." );
            }
            int entryOffset = neuronNumber * BYTES_PER_ENTRY;
            rawMap[ entryOffset ] = rgb[ 0 ];
            rawMap[ entryOffset + 1 ] = rgb[ 1 ];
            rawMap[ entryOffset + 2 ] = rgb[ 2 ];
            rawMap[ entryOffset + 3 ] = (byte)0xff;
        }

        mapData = ByteBuffer.wrap( rawMap );

    }

    @Override
    public ByteBuffer getTextureData() {
        return mapData;
    }

    //  The size of the texture data will be 2**8 * 2**8 or 2**16: same as 65535
    @Override
    public int getSx() {
        return 256;
    }

    @Override
    public int getSy() {
        return 256;
    }

    @Override
    public int getSz() {
        return 1;
    }

    @Override
    public boolean isLoaded() {
        return false;
    }

    @Override
    public void setLoaded(boolean loaded) {
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
        return VolumeDataAcceptor.TextureColorSpace.COLOR_SPACE_RGB;
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
        return "Map of colors to neuron fragment numbers";
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
        return 1;  // Bytes
    }

    @Override
    public void setPixelByteCount(int pixelByteCount) {
    }

    @Override
    public String getFilename() {
        return null;
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
}
