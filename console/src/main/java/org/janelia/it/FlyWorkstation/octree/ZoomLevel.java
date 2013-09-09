package org.janelia.it.FlyWorkstation.octree;

public class ZoomLevel {
	private int log2ZoomOutFactor;
	private int zoomOutFactor;

	// 
	public ZoomLevel(int log2ZoomOutFactor) {
		this.log2ZoomOutFactor = log2ZoomOutFactor;
		this.zoomOutFactor = (int)Math.pow(2, log2ZoomOutFactor);
	}
	
	/**
	 * Integer log to the base 2 of the zoom-out factor
	 * @return
	 */
	int getLog2ZoomOutFactor() {
		return log2ZoomOutFactor;
	}
	
	/**
	 * Integer scale factor relating this zoom level to the highest resolution
	 * zoom level.
	 * @return
	 */
	int getZoomOutFactor() {
		return zoomOutFactor;
	}
}
