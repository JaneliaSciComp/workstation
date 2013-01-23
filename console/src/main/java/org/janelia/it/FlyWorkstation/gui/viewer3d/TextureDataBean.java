package org.janelia.it.FlyWorkstation.gui.viewer3d;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 1/10/13
 * Time: 10:42 AM
 *
 * All info representing a texture volume.  Note that the texture offset for this is not known,
 * because masks like this can be switched on and off, changing their offsets at runtime.
 */

import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataI;

import java.nio.ByteOrder;
import java.nio.IntBuffer;

public class TextureDataBean implements TextureDataI {
    private IntBuffer maskData;
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

    public TextureDataBean(IntBuffer maskData, int sx, int sy, int sz) {
        this();
        setMaskData(maskData, sx, sy, sz);
    }

    public TextureDataBean(int[] maskData, int sx, int sy, int sz) {
        this( IntBuffer.wrap(maskData), sx, sy, sz );
    }

    public TextureDataBean( int[] maskData, Integer[] voxels ) {
        this( IntBuffer.wrap(maskData), voxels[ 0 ], voxels[ 1 ], voxels[ 2 ] );
    }

    public void setMaskData( IntBuffer maskData, int sx, int sy, int sz ) {
        this.maskData = maskData;
        this.sx = sx;
        this.sy = sy;
        this.sz = sz;
    }

    public IntBuffer getMaskData() {
        return maskData;
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

    public void setMaskData(IntBuffer maskData) {
        this.maskData = maskData;
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

