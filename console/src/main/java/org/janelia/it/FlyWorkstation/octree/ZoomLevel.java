package org.janelia.it.FlyWorkstation.octree;

public class ZoomLevel {
	private int log2ZoomOutFactor;
	private int zoomOutFactor;

	// 
	public ZoomLevel(int log2ZoomOutFactor) {
		this.log2ZoomOutFactor = log2ZoomOutFactor;
		this.zoomOutFactor = (int)Math.pow(2, log2ZoomOutFactor);
	}
	
	@Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + log2ZoomOutFactor;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ZoomLevel other = (ZoomLevel) obj;
        if (log2ZoomOutFactor != other.log2ZoomOutFactor)
            return false;
        return true;
    }

    /**
	 * Integer log to the base 2 of the zoom-out factor
	 * @return
	 */
	public int getLog2ZoomOutFactor() {
		return log2ZoomOutFactor;
	}
	
	/**
	 * Integer scale factor relating this zoom level to the highest resolution
	 * zoom level.
	 * @return
	 */
	public int getZoomOutFactor() {
		return zoomOutFactor;
	}

}
