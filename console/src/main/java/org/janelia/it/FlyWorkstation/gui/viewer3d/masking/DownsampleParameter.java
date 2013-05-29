package org.janelia.it.FlyWorkstation.gui.viewer3d.masking;

public class DownsampleParameter {
    private final byte[] fullSizeVolume;
    private final int voxelBytes;
    private final double xScale;
    private final double yScale;
    private final double zScale;
    private final int outSx;
    private final int outSy;
    private final byte[] textureByteArray;
    private int outY = 0;  // Emphasis: set to zero intentionally.
    private final int zOffset;

    public DownsampleParameter(byte[] fullSizeVolume, int voxelBytes, double xScale, double yScale, double zScale, int outSx, int outSy, byte[] textureByteArray, int zOffset) {
        this.fullSizeVolume = fullSizeVolume;
        this.voxelBytes = voxelBytes;
        this.xScale = xScale;
        this.yScale = yScale;
        this.zScale = zScale;
        this.outSx = outSx;
        this.outSy = outSy;
        this.textureByteArray = textureByteArray;
        this.outY = outY;
        this.zOffset = zOffset;
    }

    public byte[] getFullSizeVolume() {
        return fullSizeVolume;
    }

    public int getVoxelBytes() {
        return voxelBytes;
    }

    public double getXScale() {
        return xScale;
    }

    public double getYScale() {
        return yScale;
    }

    public double getZScale() {
        return zScale;
    }

    public int getOutSx() {
        return outSx;
    }

    public int getOutSy() {
        return outSy;
    }

    public byte[] getTextureByteArray() {
        return textureByteArray;
    }

    public int getOutY() {
        return outY;
    }

    public int getZOffset() {
        return zOffset;
    }

    public void setOutY(int outY) {
        this.outY = outY;
    }
}
