package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;

/*
 * Wrap texture cache so indices can be interpolated for either quadtrees
 * or octrees.
 */
public class TextureCache 
{
	public static enum IndexStyle {
		QUADTREE,
		OCTREE
	}
	
	private IndexStyle indexStyle = IndexStyle.QUADTREE;
	
	// Cache actually stores octree or quadtree or whatever index.
	// Interface uses quadtree index.
	// ...with the index interpolator class to mediate between the two.
	private Map<PyramidTileIndex, TileTexture> cache = new Hashtable<PyramidTileIndex, TileTexture>();
	PyramidIndexInterpolator indexInterpolator = new QuadtreeInterpolator();

	public void clear() {cache.clear();}
	
	boolean containsKey(PyramidTileIndex quadtreeIndex) {
		PyramidTileIndex otherIndex = indexInterpolator.fromQuadtreeIndex(quadtreeIndex);
		boolean result = cache.containsKey(otherIndex);
		// if (! result)
		// 	System.out.println("cache miss "+quadtreeIndex+"/"+otherIndex);
		return result;
	}
	
	TileTexture get(PyramidTileIndex quadtreeIndex) {
		return cache.get(indexInterpolator.fromQuadtreeIndex(quadtreeIndex));
	}
	
	public IndexStyle getIndexStyle() {
		return indexStyle;
	}

	public void setIndexStyle(IndexStyle indexStyle) {
		if (indexStyle == this.indexStyle)
			return;
		this.indexStyle = indexStyle;
		switch (this.indexStyle) {
			case OCTREE:
				this.indexInterpolator = new OctreeInterpolator();
				break;
			default:
				this.indexInterpolator = new QuadtreeInterpolator();
				break;				
		}
	}

	public TileTexture put(PyramidTileIndex quadtreeIndex, TileTexture value)
	{
		PyramidTileIndex otherIndex = indexInterpolator.fromQuadtreeIndex(quadtreeIndex);
		// System.out.println("Inserting cache "+quadtreeIndex+"/"+otherIndex);
		return cache.put(otherIndex, value);
	}
	
	public int size() {return cache.size();}
	
	public Collection<TileTexture> values() {return cache.values();}
	
	
	static public class OctreeInterpolator
	implements PyramidIndexInterpolator
	{
		@Override
		public PyramidTileIndex fromQuadtreeIndex(PyramidTileIndex quadtreeIndex) {
			int zoomFactor = (int)Math.pow(2, quadtreeIndex.getMaxZoom() - quadtreeIndex.getZoom());
			int z = quadtreeIndex.getZ() / zoomFactor;
			return new PyramidTileIndex(
					quadtreeIndex.getX(), 
					quadtreeIndex.getY(), 
					z, 
					quadtreeIndex.getZoom(), 
					quadtreeIndex.getMaxZoom());
		}

		@Override
		public PyramidTileIndex toQuadtreeIndex(PyramidTileIndex otherIndex) {
			int zoomFactor = (int)Math.pow(2, otherIndex.getMaxZoom() - otherIndex.getZoom());
			int z = otherIndex.getZ() * zoomFactor;
			return new PyramidTileIndex(otherIndex.getX(), otherIndex.getY(), z, otherIndex.getZoom(), otherIndex.getMaxZoom());
		}
	}
	
	static public class QuadtreeInterpolator
	implements PyramidIndexInterpolator
	{
		@Override
		public PyramidTileIndex fromQuadtreeIndex(PyramidTileIndex quadtreeIndex) {
			return quadtreeIndex;
		}

		@Override
		public PyramidTileIndex toQuadtreeIndex(PyramidTileIndex otherIndex) {
			return otherIndex;
		}
	}
	
}
