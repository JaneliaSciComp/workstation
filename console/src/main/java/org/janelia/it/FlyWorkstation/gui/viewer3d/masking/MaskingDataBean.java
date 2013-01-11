package org.janelia.it.FlyWorkstation.gui.viewer3d.masking;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 1/10/13
 * Time: 10:42 AM
 *
 * All info representing a texture for masking.  Note that the texture offset for this is not known,
 * because masks like this can be switched on and off, changing their offsets at runtime. *
 */

import org.janelia.it.FlyWorkstation.gui.viewer3d.VolumeDataAcceptor;

import java.nio.IntBuffer;

public class MaskingDataBean {
    private IntBuffer maskData;
    private Integer sx;
    private Integer sy;
    private Integer sz;

    private VolumeDataAcceptor.TextureColorSpace colorSpace;
    private Double[] volumeMicrometers;
    private Double[] voxelMicrometers;

    private boolean loaded;

    public MaskingDataBean() {
        super();
        this.loaded = false; // Emphasis.
    }

    public MaskingDataBean(IntBuffer maskData, int sx, int sy, int sz) {
        this();
        setMaskData(maskData, sx, sy, sz);
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
}

