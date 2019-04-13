package org.janelia.workstation.raster;

public interface RasterSubvolume 
extends RasterVolume
{
	VoxelIndex getVolumeOriginInVoxels();
}
