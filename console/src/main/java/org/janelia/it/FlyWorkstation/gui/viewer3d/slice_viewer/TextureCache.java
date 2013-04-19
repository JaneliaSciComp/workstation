package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Wrap texture cache so indices can be interpolated for either quadtrees
 * or octrees.
 */
public class TextureCache 
{
	private static final Logger log = LoggerFactory.getLogger(TextureCache.class);

	private HistoryCache historyCache = new HistoryCache(); // textures that have been displayed, ordered by LRU
	private HistoryCache futureCache = new HistoryCache(); // textures we predict will be displayed
	private HistoryCache persistentCache = new HistoryCache(); // lowest resolution textures for everything
	
	public Signal getCacheClearedSignal() {
		return cacheClearedSignal;
	}

	private Signal cacheClearedSignal = new Signal();

	public synchronized void add(TileTexture texture) {
		TileIndex index = texture.getIndex();
		if (containsKey(index))
			log.warn("Adding texture that is already in cache "+index);
		if (index.getZoom() == index.getMaxZoom()) {
			// log.info("adding persistent texture "+index);
			persistentCache.addFirst(texture);
		}
		else
			futureCache.addFirst(texture);
	}

	synchronized public void clear() {
		// log.info("Clearing texture cache");
		futureCache.clear();
		historyCache.clear();
		persistentCache.clear();
		cacheClearedSignal.emit();
	}

	boolean containsKey(TileIndex index) {
		return persistentCache.containsKey(index)
				|| historyCache.containsKey(index)
				|| futureCache.containsKey(index);
	}
	
	synchronized TileTexture get(TileIndex index) {
		if (persistentCache.containsKey(index))
			return persistentCache.get(index);
		else if (historyCache.containsKey(index))
			return historyCache.get(index);
		else
			return futureCache.get(index);
	}
	
	// Indicate that a particular texture has been viewed, rather than simply
	// pre-fetched.
	public void markHistorical(TileIndex index) {
		// Only future cached textures need to be moved.
		// (textures in the persistent cache should remain there)
		TileTexture texture = futureCache.remove(index);
		if (texture == null)
			texture = historyCache.get(index);
		if (texture == null) {
			if (! persistentCache.containsKey(index))
				log.warn("Failed to find historical texture "+index);
			return;
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
	
	/**
	 * Do not use to modify future cache.
	 * @return
	 */
	public HistoryCache getFutureCache() {
		return futureCache;
	}

	public int[] popObsoleteTextureIds() {
		Set<Integer> ids = futureCache.popObsoleteGlTextures();
		ids.addAll(historyCache.popObsoleteGlTextures());
		ids.addAll(persistentCache.popObsoleteGlTextures());
		int result[] = new int[ids.size()];
		int i = 0;
		for (int val : ids) {
			result[i] = val;
			i += 1;
		}
		return result;
	}

}
