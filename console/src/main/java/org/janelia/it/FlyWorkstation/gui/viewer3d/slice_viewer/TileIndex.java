package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.util.List;
import java.util.Vector;
import org.janelia.it.FlyWorkstation.gui.viewer3d.CoordinateAxis;

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

	/*
	private final int x;
	private final int y;
	private final int z;
	*/
	private final int xyz[] = new int[3];
	
	private final int zoom;
	private final int canonicalZ; // Uniquified on octree zoom level
	// Perhaps the following items should be in some sort of shared format type
	private final int maxZoom;
	private final IndexStyle indexStyle;
	private final int deltaZ;
	private final CoordinateAxis sliceAxis;
	
	public TileIndex(int x, int y, int z, 
			int zoom, int maxZoom, 
			IndexStyle indexStyle,
			CoordinateAxis axis) 
	{
		this.sliceAxis = axis;
		this.xyz[0] = x;
		this.xyz[1] = y;
		this.xyz[2] = z;
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
		if (sliceAxis != other.sliceAxis)
			return false;
		for (int i = 0; i < 3; ++i) {
			if (i == sliceAxis.index())
				continue; // We will compare canonicalSliceAxis below
			if (xyz[i] != other.xyz[i])
				return false;
		}
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
		return xyz[0];
	}

	public int getY() {
		return xyz[1];
	}

	public int getZ() {
		return xyz[2];
	}
	
	public int getCoordinate(int index) {
		return xyz[index];
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
	
	public IndexStyle getIndexStyle() {
		return indexStyle;
	}

	// For hashability, we need hashCode() and equals() methods.
	// Pro tip: Use eclipse to autogenerate hashCode and equals methods!
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		for (int i = 0; i < 3; ++i) {
			if (i == sliceAxis.index())
				continue; // We will compare canonicalSliceAxis below
			result = prime * result + xyz[i];
		}
		result = prime * result + canonicalZ;
		result = prime * result + zoom;
		result = prime * result + sliceAxis.index();
		return result;
	}

	@Override
	public String toString() {
		return "TileIndex [x="+getX()+"; y="+getY()+"; z="+getZ()+"; zoom="+zoom+"; sliceAxis="+sliceAxis.getName()+"]";
	}

	public TileIndex clone() {
		return new TileIndex(
				getX(), 
				getY(), 
				getZ(), 
				zoom, 
				maxZoom, 
				indexStyle,
				sliceAxis);
	}

	public TileIndex nextZ() {
		int newXyz[] = {getX(), getY(), getZ()};
		newXyz[sliceAxis.index()] += deltaZ;
		return new TileIndex(
		newXyz[0], 
		newXyz[1], 
		newXyz[2], 
		zoom, 
		maxZoom, 
		indexStyle,
		sliceAxis);
	}
	
	public TileIndex previousZ() {
		int newXyz[] = {getX(), getY(), getZ()};
		newXyz[sliceAxis.index()] -= deltaZ;
		return new TileIndex(
		newXyz[0], 
		newXyz[1], 
		newXyz[2], 
		zoom, 
		maxZoom, 
		indexStyle,
		sliceAxis);
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
		int newXyz[] = {getX(), getY(), getZ()};
		for (int i = 0; i < 3; ++i) {
			if (i == sliceAxis.index())
				continue;
			newXyz[i] = newXyz[i]/2;
		}
		int zoom = getZoom() + 1;
		return new TileIndex(
				newXyz[0], 
				newXyz[1], 
				newXyz[2], 
				zoom, 
				maxZoom, indexStyle, sliceAxis);
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

	public CoordinateAxis getSliceAxis() {
		return sliceAxis;
	}

}
