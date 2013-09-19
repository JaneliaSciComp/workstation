package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.volume_builder;

import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.masking.VolumeDataI;

public class DownsampleParameter {
    private final VolumeDataI fullSizeVolume;
    private final int voxelBytes;
    private final double xScale;
    private final double yScale;
    private final double zScale;
    private final int outSx;
    private final int outSy;
    private final VolumeDataI downsampledVolume;

    public DownsampleParameter(VolumeDataI fullSizeVolume, int voxelBytes, double xScale, double yScale, double zScale, int outSx, int outSy, VolumeDataI downsampledVolume) {
        this.fullSizeVolume = fullSizeVolume;
        this.voxelBytes = voxelBytes;
        this.xScale = xScale;
        this.yScale = yScale;
        this.zScale = zScale;
        this.outSx = outSx;
        this.outSy = outSy;
        this.downsampledVolume = downsampledVolume;
    }

    public VolumeDataI getFullSizeVolume() {
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

    public VolumeDataI getDownsampledVolume() {
        return downsampledVolume;
    }

}
