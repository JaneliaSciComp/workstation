package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

/*
 * Common metadata to back end tile data formats
 */
public class PyramidTileFormat 
{
	private int[] origin = new int[3];
	private int[] volumeSize = new int[3];
	private int[] tileSize = new int[3];
	private double[] voxelMicrometers = new double[3];
	private int zoomLevelCount;
	private int bitDepth;
	private int channelCount;
	private int intensityMax;
	private int intensityMin;
	private boolean srgb;

	public PyramidTileFormat() 
	{
		setDefaultParameters();
	}
	
	public int[] getOrigin() {return origin;}


	public int[] getTileSize() {
		return tileSize;
	}

	public int[] getVolumeSize() {
		return volumeSize;
	}

	public double[] getVoxelMicrometers() {
		return voxelMicrometers;
	}

	public int getZoomLevelCount() {
		return zoomLevelCount;
	}

	public int getBitDepth() {
		return bitDepth;
	}

	public int getChannelCount() {
		return channelCount;
	}

	public int getIntensityMax() {
		return intensityMax;
	}

	public int getIntensityMin() {
		return intensityMin;
	}

	public boolean isSrgb() {
		return srgb;
	}

	public void setOrigin(int[] origin) {
		this.origin = origin;
	}

	public void setVolumeSize(int[] volumeSize) {
		this.volumeSize = volumeSize;
	}

	public void setTileSize(int[] tileSize) {
		this.tileSize = tileSize;
	}

	public void setVoxelMicrometers(double[] voxelMicrometers) {
		this.voxelMicrometers = voxelMicrometers;
	}

	public void setZoomLevelCount(int zoomLevelCount) {
		this.zoomLevelCount = zoomLevelCount;
	}

	public void setBitDepth(int bitDepth) {
		this.bitDepth = bitDepth;
	}

	public void setChannelCount(int channelCount) {
		this.channelCount = channelCount;
	}

	public void setIntensityMax(int intensityMax) {
		this.intensityMax = intensityMax;
	}

	public void setIntensityMin(int intensityMin) {
		this.intensityMin = intensityMin;
	}

	public void setDefaultParameters() {
		for (int i = 0; i < 3; ++i) {
			origin[i] = 0;
			volumeSize[i] = 512; // whatever
			tileSize[i] = 512; // X, Y, but not Z...
			voxelMicrometers[i] = 1.0;
		}
		tileSize[2] = 1; // tiles are 512x512x1
		zoomLevelCount = 1; // like small images
		bitDepth = 8;
		channelCount = 3; // rgb
		intensityMin = 0;
		intensityMax = 255;
		srgb = false;
	}

	public void setSrgb(boolean srgb) {
		this.srgb = srgb;
	}

}
