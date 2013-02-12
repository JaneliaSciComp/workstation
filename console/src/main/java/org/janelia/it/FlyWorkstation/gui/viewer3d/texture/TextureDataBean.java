package org.janelia.it.FlyWorkstation.gui.viewer3d.texture;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 1/10/13
 * Time: 10:42 AM
 *
 * All info representing a texture volume.  Note that the texture offset for this is not known,
 * because masks like this can be switched on and off, changing their offsets at runtime.
 */

import org.janelia.it.FlyWorkstation.gui.viewer3d.VolumeDataAcceptor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

public class TextureDataBean implements TextureDataI {
    private String filename;
    private String remoteFilename;
    private ByteBuffer textureData;
    private Integer sx;
    private Integer sy;
    private Integer sz;

    private VolumeDataAcceptor.TextureColorSpace colorSpace;
    private Double[] volumeMicrometers;
    private Double[] voxelMicrometers;

    // These settings may be avoided for most file types.
    private String header = "Not Available";
    private ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;
    private int pixelByteCount = 1;
    private int channelCount = 0;

    private boolean loaded;
    private boolean inverted = true; // Tested stored images were inverted.

    private static ByteBuffer getByteBuffer(int[] textureData) {
        ByteBuffer textureBuffer = ByteBuffer.allocate( textureData.length * (Integer.SIZE / 8));
        textureBuffer.order( ByteOrder.LITTLE_ENDIAN );
        for ( int i = 0; i < textureData.length; i++ ) {
            textureBuffer.putInt( textureData[ i ] );
        }
        return textureBuffer;
    }

    public TextureDataBean( ByteBuffer textureData, int sx, int sy, int sz ) {
        super();
        setTextureData(textureData);
        setSx( sx );
        setSy( sy );
        setSz( sz );
    }

    public TextureDataBean(int[] textureData, int sx, int sy, int sz) {
        this(getByteBuffer(textureData), sx, sy, sz);
    }

    @Override
    public void setTextureData( ByteBuffer textureData ) {
        this.textureData = textureData;
    }

    public ByteBuffer getTextureData() {
        return textureData;
    }

    public int getSx() {
        return sx;
    }

    public int getSy() {
        return sy;
    }

    public int getSz() {
        return sz;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    public void setSx(int sx) {
        this.sx = sx;
    }

    public void setSy(int sy) {
        this.sy = sy;
    }

    public void setSz(int sz) {
        this.sz = sz;
    }

    public VolumeDataAcceptor.TextureColorSpace getColorSpace() {
        return colorSpace;
    }

    public void setColorSpace(VolumeDataAcceptor.TextureColorSpace colorSpace) {
        this.colorSpace = colorSpace;
    }

    public Double[] getVolumeMicrometers() {
        return volumeMicrometers;
    }

    public void setVolumeMicrometers(Double[] volumeMicrometers) {
        this.volumeMicrometers = volumeMicrometers;
    }

    public Double[] getVoxelMicrometers() {
        return voxelMicrometers;
    }

    public void setVoxelMicrometers(Double[] voxelMicrometers) {
        this.voxelMicrometers = voxelMicrometers;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public ByteOrder getByteOrder() {
        return byteOrder;
    }

    public void setByteOrder(ByteOrder byteOrder) {
        this.byteOrder = byteOrder;
    }

    public int getPixelByteCount() {
        return pixelByteCount;
    }

    public void setPixelByteCount(int pixelByteCount) {
        this.pixelByteCount = pixelByteCount;
    }

    @Override
    public String getFilename() {
        return remoteFilename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public int getChannelCount() {
        return channelCount;
    }

    public void setChannelCount(int channelCount) {
        this.channelCount = channelCount;
    }

    public boolean isInverted() {
        return inverted;
    }

    public void setInverted(boolean inverted) {
        this.inverted = inverted;
    }
}

