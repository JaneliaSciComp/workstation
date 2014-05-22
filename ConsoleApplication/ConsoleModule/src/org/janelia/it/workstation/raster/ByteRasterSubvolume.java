package org.janelia.it.workstation.raster;

import java.nio.ByteBuffer;


public class ByteRasterSubvolume
implements RasterSubvolume
{
	private ByteBuffer byteBuffer;
	org.janelia.it.workstation.raster.VoxelIndex size;
	org.janelia.it.workstation.raster.VoxelIndex origin;
	
	public ByteBuffer getByteBuffer() {
		return byteBuffer;
	}

	@Override
	public org.janelia.it.workstation.raster.VoxelIndex getVolumeSizeInVoxels() {
		return size;
	}

	@Override
	public org.janelia.it.workstation.raster.VoxelIndex getVolumeOriginInVoxels() {
		return origin;
	}
}
