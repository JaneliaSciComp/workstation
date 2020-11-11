package org.janelia.workstation.controller.tileimagery.raster;

public interface RasterSubvolume 
extends RasterVolume
{
	VoxelIndex getVolumeOriginInVoxels();
}
