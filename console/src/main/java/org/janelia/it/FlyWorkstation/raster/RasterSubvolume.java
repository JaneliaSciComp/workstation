package org.janelia.it.FlyWorkstation.raster;

public interface RasterSubvolume 
extends RasterVolume
{
	VoxelIndex getVolumeOriginInVoxels();
}
