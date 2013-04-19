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
	// private static final Logger log = LoggerFactory.getLogger(TextureCache.class);
	
	// private IndexStyle indexStyle = IndexStyle.QUADTREE;

	private HistoryCache historyCache = new HistoryCache();
	private HistoryCache futureCache = new HistoryCache();
	private Map<PyramidTileIndex, TileTexture> persistentCache = new HashMap<PyramidTileIndex, TileTexture>();
	
	public Signal getCacheClearedSignal() {
		return cacheClearedSignal;
	}

	private Signal cacheClearedSignal = new Signal();

	public TextureCache() {
		// log.info("Creating texture cache");
	}
	
	synchronized public void clear() {
		// log.info("Clearing texture cache");
		futureCache.clear();
		historyCache.clear();
		persistentCache.clear();
		cacheClearedSignal.emit();
	}

	boolean containsKey(PyramidTileIndex index) {
		return persistentCache.containsKey(index)
				|| historyCache.containsKey(index)
				|| futureCache.containsKey(index);
	}
	
	synchronized TileTexture get(PyramidTileIndex index) {
		if (persistentCache.containsKey(index))
			return persistentCache.get(index);
		else if (historyCache.containsKey(index))
			return historyCache.get(index);
		else
			return futureCache.get(index);
	}
	
	synchronized TileTexture getOrCreate(
			PyramidTileIndex index, 
			PyramidTextureLoadAdapter loadAdapter) 
	{
		if (containsKey(index))
			return get(index);
		TileTexture texture = new TileTexture(index, loadAdapter);
		// Store lowest resolution tiles into the persistent cache
		if (index.getZoom() == index.getMaxZoom())
			persistentCache.put(index, texture);
		else
			futureCache.addFirst(texture);
		return texture;
	}
	
	// Indicate that a particular texture has been viewed, rather than simply
	// pre-fetched.
	public void markHistorical(PyramidTileIndex index, PyramidTextureLoadAdapter loadAdapter) {
		if (persistentCache.containsKey(index))
			return; // persistent cache has priority over historical cache
		TileTexture texture = historyCache.get(index);
		if (texture == null) { // texture was not already in history cache
			// Move texture from future cache to history cache
			texture = futureCache.remove(index);
			if (texture == null) // This texture wasn't yet in ANY cache
				texture = new TileTexture(index, loadAdapter);
		}
		historyCache.addFirst(texture); // move texture to the front of the queue
	}
	
	public int size() {return futureCache.size() + historyCache.size() + persistentCache.size();}
	
	public Collection<TileTexture> values() {
		Set<TileTexture> result = new HashSet<TileTexture>();
		result.addAll(historyCache.values());
		result.addAll(futureCache.values());
		result.addAll(persistentCache.values());
		return result;
	}
	
	public HistoryCache getFutureCache() {
		return futureCache;
	}

	public int[] popObsoleteTextureIds() {
		Set<Integer> ids = futureCache.popObsoleteGlTextures();
		ids.addAll(historyCache.popObsoleteGlTextures());
		int result[] = new int[ids.size()];
		int i = 0;
		for (int val : ids) {
			result[i] = val;
			i += 1;
		}
		return result;
	}

}
