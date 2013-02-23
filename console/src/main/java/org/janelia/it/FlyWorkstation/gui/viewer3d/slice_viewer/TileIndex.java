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
		result = prime * result + z;
		result = prime * result + zoom;
		return result;
	}

	@Override
	public String toString() {
		return "TileIndex [x="+x+"; y="+y+"; z="+z+"; zoom="+zoom+"]";
	}

	/**
	 * Returns the index of the next lower resolution tile that 
	 * contains the tile represented by this index.
	 * 
	 * @return null if current zoom index is already zero
	 */
	public TileIndex zoomOut() {
		if (getZoom() <= 0)
			return null; // Cannot zoom farther out than zero
		int x = getX()/2;
		int y = getY()/2;
		int z = getZ();
		int zoom = getZoom() - 1;
		return new TileIndex(x, y, z, zoom);
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
