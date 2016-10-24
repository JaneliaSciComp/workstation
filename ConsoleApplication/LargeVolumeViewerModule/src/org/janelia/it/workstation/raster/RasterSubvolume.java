package org.janelia.it.workstation.raster;

public interface RasterSubvolume 
extends RasterVolume
{
	VoxelIndex getVolumeOriginInVoxels();
}
