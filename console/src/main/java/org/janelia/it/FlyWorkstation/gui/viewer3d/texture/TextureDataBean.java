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
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataI;

import java.nio.ByteOrder;

public class TextureDataBean implements TextureDataI {
    private int[] textureData;
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

    private boolean loaded;

    public TextureDataBean() {
        super();
        this.loaded = false; // Emphasis.
    }

    public TextureDataBean(int[] textureData, int sx, int sy, int sz) {
        super();
        this.textureData = textureData;
        this.sx = sx;
        this.sy = sy;
        this.sz = sz;
    }

    public TextureDataBean( int[] textureData, Integer[] voxels ) {
        this( textureData, voxels[ 0 ], voxels[ 1 ], voxels[ 2 ] );
    }

    public void setTextureData(int[] textureData, int sx, int sy, int sz) {
        this.textureData = textureData;
        this.sx = sx;
        this.sy = sy;
        this.sz = sz;
    }

    public int[] getTextureData() {
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

    public void setTextureData(int[] textureData) {
        this.textureData = textureData;
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
}

