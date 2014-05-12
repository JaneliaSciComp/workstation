package org.janelia.it.FlyWorkstation.raster;

import java.nio.ByteBuffer;


public class ByteRasterSubvolume
implements RasterSubvolume
{
	private ByteBuffer byteBuffer;
	VoxelIndex size;
	VoxelIndex origin;
	
	public ByteBuffer getByteBuffer() {
		return byteBuffer;
	}

	@Override
	public VoxelIndex getVolumeSizeInVoxels() {
		return size;
	}

	@Override
	public VoxelIndex getVolumeOriginInVoxels() {
		return origin;
	}
}
