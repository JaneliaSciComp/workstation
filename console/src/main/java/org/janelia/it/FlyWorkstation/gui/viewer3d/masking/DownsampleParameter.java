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

    public DownsampleParameter(byte[] fullSizeVolume, int voxelBytes, double xScale, double yScale, double zScale, int outSx, int outSy, byte[] textureByteArray) {
        this.fullSizeVolume = fullSizeVolume;
        this.voxelBytes = voxelBytes;
        this.xScale = xScale;
        this.yScale = yScale;
        this.zScale = zScale;
        this.outSx = outSx;
        this.outSy = outSy;
        this.textureByteArray = textureByteArray;
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

}
