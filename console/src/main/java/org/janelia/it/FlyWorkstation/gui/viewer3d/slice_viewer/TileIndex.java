package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.util.List;
import java.util.Vector;

/**
 * An efficiently hashable key that uniquely identifies a particular
 * Tile2d or TileTexture in a RavelerTileServer.
 * 
 * @author brunsc
 *
 */
public class TileIndex 
{
	public static enum IndexStyle {
		QUADTREE,
		OCTREE
	}

	private final int x;
	private final int y;
	private final int z;
	private final int zoom;
	private final int canonicalZ; // Uniquified on octree zoom level
	// Perhaps the following items should be in some sort of shared format type
	private final int maxZoom;
	private final IndexStyle indexStyle;
	private final int deltaZ;
	
	public TileIndex(int x, int y, int z, 
			int zoom, int maxZoom, IndexStyle indexStyle) 
	{
		this.x = x;
		this.y = y;
		this.z = z;
		this.zoom = zoom;
		this.maxZoom = maxZoom;
		this.indexStyle = indexStyle;
		if (indexStyle == IndexStyle.OCTREE)
			deltaZ = (int)Math.pow(2, zoom);
		else
			deltaZ = 1;
		canonicalZ = (z/deltaZ)*deltaZ;
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
		if (canonicalZ != other.canonicalZ)
			return false;
		if (zoom != other.zoom)
			return false;
		return true;
	}

	public int getCanonicalZ() {
		return canonicalZ;
	}
	
	// How many fine Z-slices represent a single step at this zoom level?
	public int getDeltaZ() {
		return deltaZ;
	}
	
	public int getMaxZoom() {
		return maxZoom;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getZ() {
		return z;
	}

	public int getZoom() {
		return zoom;
	}

	public List<TextureScore> getTextureScores() 
	{
		List<TextureScore> result = new Vector<TextureScore>();
		double score = 1.0;
		TileIndex key = this;
		result.add(new TextureScore(key, score));
		return result;
	}
	
	// For hashability, we need hashCode() and equals() methods.
	// Pro tip: Use eclipse to autogenerate hashCode and equals methods!
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + x;
		result = prime * result + y;
		result = prime * result + canonicalZ;
		result = prime * result + zoom;
		return result;
	}

	@Override
	public String toString() {
		return "TileIndex [x="+x+"; y="+y+"; z="+z+"; zoom="+zoom+"]";
	}

	public TileIndex clone() {
		return new TileIndex(
				x, 
				y, 
				z, 
				zoom, 
				maxZoom, 
				indexStyle);
	}

	public TileIndex nextZ() {
		return new TileIndex(
		x, 
		y, 
		z + deltaZ, 
		zoom, 
		maxZoom, 
		indexStyle);
	}
	
	public TileIndex previousZ() {
		return new TileIndex(
		x, 
		y, 
		z - deltaZ, 
		zoom, 
		maxZoom, 
		indexStyle);
	}
	
	/**
	 * Returns the index of the next lower resolution tile that 
	 * contains the tile represented by this index.
	 * 
	 * @return null if current zoom index is already zero
	 */
	public TileIndex zoomOut() {
		if (getZoom() >= maxZoom)
			return null; // Cannot zoom farther out than zero
		int x = getX()/2;
		int y = getY()/2;
		int z = getZ();
		int zoom = getZoom() + 1;
		return new TileIndex(x, y, z, zoom, 
				maxZoom, indexStyle);
	}

	// Retarded Java philosophy eschews built-in Pair type nor multiple return values
	// thus the usual "add another class..." ad infinitum.
	public static class TextureScore {
		private TileIndex textureKey;
		private double score;

		public TextureScore(TileIndex key, double score) {
			this.textureKey = key;
			this.score = score;
		}
		public TileIndex getTextureKey() {
			return textureKey;
		}
		public void setTextureKey(TileIndex textureKey) {
			this.textureKey = textureKey;
		}
		public double getScore() {
			return score;
		}
		public void setScore(double score) {
			this.score = score;
		}
	}

}
