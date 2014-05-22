package org.janelia.it.workstation.gui.slice_viewer;

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

	private HistoryCache historyCache = new HistoryCache(1000); // textures that have been displayed, ordered by LRU
	private HistoryCache futureCache = new HistoryCache(3000); // textures we predict will be displayed
	private PersistentCache persistentCache = new PersistentCache(); // lowest resolution textures for everything
	// private Set<TileIndex> queuedRequests = new HashSet<TileIndex>();
	private Map<TileIndex, Long> queuedTextureTime = new HashMap<TileIndex, Long>();

	public org.janelia.it.workstation.signal.Signal getCacheClearedSignal() {
		return cacheClearedSignal;
	}
	private org.janelia.it.workstation.signal.Signal cacheClearedSignal = new org.janelia.it.workstation.signal.Signal();
	
	public org.janelia.it.workstation.signal.Signal queueDrainedSignal = new org.janelia.it.workstation.signal.Signal();

	public org.janelia.it.workstation.signal.Signal1<TileIndex> textureLoadedSignal = new org.janelia.it.workstation.signal.Signal1<TileIndex>();

	public synchronized void add(org.janelia.it.workstation.gui.slice_viewer.TileTexture texture) {
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
		queuedTextureTime.clear();
		cacheClearedSignal.emit();
	}

	boolean containsKey(TileIndex index) {
		return persistentCache.containsKey(index)
				|| historyCache.containsKey(index)
				|| futureCache.containsKey(index);
	}
	
	synchronized org.janelia.it.workstation.gui.slice_viewer.TileTexture get(TileIndex index) {
		if (persistentCache.containsKey(index))
			return persistentCache.get(index);
		else if (historyCache.containsKey(index))
			return historyCache.get(index);
		else
			return futureCache.get(index);
	}

	// Keep track of recently queued textures, to avoid redundant loads
	
	public boolean hasQueuedTextures() {
		if (queuedTextureTime.isEmpty())
			return false;
		return true;
	}
	
	public boolean isLoadQueued(TileIndex index) {
		if (! queuedTextureTime.containsKey(index))
			return false;
		long elapsed = System.nanoTime() - queuedTextureTime.get(index);
		// Don't wait longer than ten seconds
		long maxSeconds = 10;
		if (elapsed > maxSeconds * 1e9) {
			// log.warn("Waited more than "+maxSeconds+" seconds for texture load "+index);
			queuedTextureTime.remove(index);
			return false;
		}
		return true; // queued for less than 5 seconds
	}
	
	public void setLoadQueued(TileIndex index, boolean isQueued) {
		if (isQueued) {
			queuedTextureTime.put(index, System.nanoTime());
		} else {
			if (! queuedTextureTime.containsKey(index))
				return;
			queuedTextureTime.remove(index);
			if (queuedTextureTime.isEmpty())
				queueDrainedSignal.emit();
		}
	}
	
	//
	
	// Indicate that a particular texture has been viewed, rather than simply
	// pre-fetched.
	public synchronized void markHistorical(org.janelia.it.workstation.gui.slice_viewer.TileTexture tile) {
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
	
	public synchronized Collection<org.janelia.it.workstation.gui.slice_viewer.TileTexture> values() {
		Set<org.janelia.it.workstation.gui.slice_viewer.TileTexture> result = new HashSet<org.janelia.it.workstation.gui.slice_viewer.TileTexture>();
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

	public HistoryCache getHistoryCache() {
		return historyCache;
	}

	public int[] popObsoleteTextureIds() {
		Set<Integer> ids = historyCache.popObsoleteGlTextures();
		ids.addAll(persistentCache.popObsoleteGlTextures());
		ids.addAll(futureCache.popObsoleteGlTextures());
		int result[] = new int[ids.size()];
		int i = 0;
		for (int val : ids) {
			result[i] = val;
			i += 1;
		}
		return result;
	}

}
