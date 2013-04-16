package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

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
	
	public PyramidIndexInterpolator getIndexInterpolator() {
		return indexInterpolator;
	}

	// private static final Logger log = LoggerFactory.getLogger(TextureCache.class);
	
	private IndexStyle indexStyle = IndexStyle.QUADTREE;
	
	// Cache actually stores octree or quadtree or whatever index.
	// Interface uses quadtree index.
	// ...with the index interpolator class to mediate between the two.
	// private Map<PyramidTileIndex, TileTexture> cache = new HashMap<PyramidTileIndex, TileTexture>();
	private HistoryCache historyCache = new HistoryCache();
	private Map<PyramidTileIndex, TileTexture> persistentCache = new HashMap<PyramidTileIndex, TileTexture>();
	
	private PyramidIndexInterpolator indexInterpolator = new QuadtreeInterpolator();

	// private Deque<PyramidTileIndex> historyCache = new 
	
	public Signal getCacheClearedSignal() {
		return cacheClearedSignal;
	}

	private Signal cacheClearedSignal = new Signal();

	public TextureCache() {
		// log.info("Creating texture cache");
	}
	
	synchronized public void clear() {
		// log.info("Clearing texture cache");
		historyCache.clear();
		persistentCache.clear();
		cacheClearedSignal.emit();
	}
	
	boolean containsKey(PyramidTileIndex quadtreeIndex) {
		return persistentCache.containsKey(quadtreeIndex)
				|| historyCache.containsKey(getCanonicalIndex(quadtreeIndex));
	}
	
	synchronized TileTexture get(PyramidTileIndex quadtreeIndex) {
		PyramidTileIndex index = getCanonicalIndex(quadtreeIndex);
		if (persistentCache.containsKey(index))
			return persistentCache.get(index);
		else
			return historyCache.get(index);
	}
	
	synchronized TileTexture getOrCreate(
			PyramidTileIndex quadtreeIndex, 
			PyramidTextureLoadAdapter loadAdapter) 
	{
		PyramidTileIndex index = getCanonicalIndex(quadtreeIndex);
		if (containsKey(index))
			return get(index);
		TileTexture texture = new TileTexture(index, loadAdapter);
		// Store lowest resolution tiles into the persistent cache
		if (index.getZoom() == index.getMaxZoom())
			persistentCache.put(index, texture);
		else
			historyCache.addFirst(texture);
		return texture;
	}
	
	/**
	 * For quadtrees, getCanonicalIndex() returns the index passed in.
	 * For octrees, returns a canonicalized version of the input quadtree index.
	 * @param index
	 * @return
	 */
	public PyramidTileIndex getCanonicalIndex(PyramidTileIndex index) {
		if (indexInterpolator == null)
			return null;
		PyramidTileIndex result = indexInterpolator.fromQuadtreeIndex(index);
		result = indexInterpolator.toQuadtreeIndex(result);
		return result;
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

	/*
	synchronized public TileTexture put(PyramidTileIndex quadtreeIndex, TileTexture value)
	{
		PyramidTileIndex ix = getCanonicalIndex(quadtreeIndex);
		// log.info("inserting texture at "+ix);
		return ache.addFirst(value);
	}
	*/
	
	public int size() {return historyCache.size() + persistentCache.size();}
	
	public Collection<TileTexture> values() {
		Set<TileTexture> result = new HashSet<TileTexture>();
		result.addAll(historyCache.values());
		result.addAll(persistentCache.values());
		return result;
	}
	
	static public class OctreeInterpolator
	implements PyramidIndexInterpolator
	{
		@Override
		public PyramidTileIndex fromQuadtreeIndex(PyramidTileIndex quadtreeIndex) {
			int zoomFactor = (int)Math.pow(2, quadtreeIndex.getZoom());
			int z = quadtreeIndex.getZ() / zoomFactor;
			PyramidTileIndex result = new PyramidTileIndex(
					quadtreeIndex.getX(), 
					quadtreeIndex.getY(), 
					z, 
					quadtreeIndex.getZoom(), 
					quadtreeIndex.getMaxZoom());
			// log.info("q: "+quadtreeIndex+"; o: "+result);
			return result;
		}

		@Override
		public PyramidTileIndex toQuadtreeIndex(PyramidTileIndex otherIndex) {
			int zoomFactor = (int)Math.pow(2, otherIndex.getZoom());
			int z = otherIndex.getZ() * zoomFactor;
			PyramidTileIndex result = new PyramidTileIndex(
					otherIndex.getX(), 
					otherIndex.getY(), 
					z, 
					otherIndex.getZoom(), 
					otherIndex.getMaxZoom());
			// log.info("o: "+otherIndex+"; q:"+result);
			return result;
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
