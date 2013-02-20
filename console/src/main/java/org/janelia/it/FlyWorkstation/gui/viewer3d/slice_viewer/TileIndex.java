package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

public class TileIndex 
{
	private final int x;
	private final int y;
	private final int z;
	private final int zoom;
	
	public TileIndex(int x, int y, int z, int zoom) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.zoom = zoom;
	}

	// For hashability, we need hashCode() and equals() methods.
	// Pro tip: Use eclipse to autogenerate hashCode and equals methods!
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + x;
		result = prime * result + y;
		result = prime * result + z;
		result = prime * result + zoom;
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
		TileIndex other = (TileIndex) obj;
		if (x != other.x)
			return false;
		if (y != other.y)
			return false;
		if (z != other.z)
			return false;
		if (zoom != other.zoom)
			return false;
		return true;
	}
}
