package org.janelia.it.FlyWorkstation.gui.viewer3d.texture;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 1/10/13
 * Time: 10:42 AM
 *
 * All info representing a texture volume. This one is to meet the special needs of masking textures, such
 * as quick fetching of bytes.
 */

import org.janelia.it.FlyWorkstation.gui.viewer3d.VolumeDataAcceptor;
import org.jdesktop.swingx.plaf.BuddyTextFieldUI;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MaskTextureDataBean implements TextureDataI {
    private String filename;
    private byte[] textureData;
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
    private boolean inverted = true; // Most tested masks were inverted.

    public MaskTextureDataBean() {
        super();
    }

    public MaskTextureDataBean(byte[] textureData, int sx, int sy, int sz) {
        super();
        this.textureData = textureData;
        setSx( sx );
        setSy( sy );
        setSz( sz );
    }

    public MaskTextureDataBean(byte[] textureData, Integer[] voxels) {
        this(textureData, voxels[0], voxels[1], voxels[2]);
    }

    public void setTextureData( ByteBuffer textureData ) {
        this.textureData = textureData.array();
    }

    public byte[] getTextureBytes() {
        return textureData;
    }

    public ByteBuffer getTextureData() {
        return ByteBuffer.wrap( textureData );
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

    public String getFilename() {
        return filename;
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

