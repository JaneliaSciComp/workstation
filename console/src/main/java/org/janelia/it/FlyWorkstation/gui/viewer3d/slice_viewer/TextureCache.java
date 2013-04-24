package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.util.Collection;
import java.util.HashSet;
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

	private HistoryCache historyCache = new HistoryCache(1500); // textures that have been displayed, ordered by LRU
	private HistoryCache futureCache = new HistoryCache(1500); // textures we predict will be displayed
	private PersistentCache persistentCache = new PersistentCache(); // lowest resolution textures for everything
	
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
			persistentCache.put(texture.getIndex(), texture);
		}
		else {
			futureCache.put(index, texture);
			if (! futureCache.containsKey(index)) {
				log.error("Future cache insert failed.");
			}
		}
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
	public synchronized void markHistorical(TileTexture tile) {
		if (tile == null)
			return;
		// Only future cached textures need to be moved.
		// (textures in the persistent cache should remain there)
		if (persistentCache.containsKey(tile.getIndex()))
			return;
		futureCache.remove(tile);
		historyCache.put(tile.getIndex(), tile); // move texture to the front of the queue
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
		Set<Integer> ids = historyCache.popObsoleteGlTextures();
		// future cache ids were probably moved to history cache, so do not delete them.
		// ids.addAll(futureCache.popObsoleteGlTextures()); 
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
