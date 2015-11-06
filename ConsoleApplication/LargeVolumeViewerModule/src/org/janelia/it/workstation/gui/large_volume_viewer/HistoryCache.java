package org.janelia.it.workstation.gui.large_volume_viewer;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HistoryCache
implements Map<TileIndex, TileTexture>
{
	private static final Logger log = LoggerFactory.getLogger(HistoryCache.class);	
	
	// Store deleted opengl texture ids, for deferred disposal.
	private Set<Integer> obsoleteGlTextures = new HashSet<Integer>();
	private Set<Integer> removedOpenGLTexturesSinceClear = new HashSet<Integer>();
	// Flag to help report when cache has filled.
	// To allow lookup by index
	private Map<TileIndex, TileTexture> map;
	private int maxEntries;

	public HistoryCache(int maxSize) {
		this.maxEntries = maxSize;
		// Create a synchronized, least-recently-used map container, with a size limit.
		// Use synchronizedMap() to generate thread-safe container.
		//map = Collections.synchronizedMap(new LinkedHashMap<TileIndex, TileTexture>(
				map = new LinkedHashMap<TileIndex, TileTexture>(

						maxSize, 0.75f, true) { // "true" for access-order, based on get()/add() calls
			private static final long serialVersionUID = 1L;
			@Override
			// Fix size of LRU cache
			protected boolean removeEldestEntry(Map.Entry<TileIndex, TileTexture> eldest) {
				return false; // because we are handling removal ourselves
			}

		};
	}

	@Override
	public synchronized void clear() {
		for (TileTexture tile : map.values()) {
			if (tile == null)
				continue;
			PyramidTexture texture1 = tile.getTexture();
			if (texture1 == null)
				continue;
			int id = texture1.getTextureId();
			if (id < 1)
				continue;
			obsoleteGlTextures.add(id); // remember OpenGl texture IDs for later deletion.			
		}
		obsoleteGlTextures.addAll(removedOpenGLTexturesSinceClear);
		map.clear();
	}

	public Set<Integer> popObsoleteGlTextures() {
		if (obsoleteGlTextures.size() > 0)
			log.info("Popping obsolete textures.  Size {}.", obsoleteGlTextures.size());
		Set<Integer> result = obsoleteGlTextures;
		obsoleteGlTextures = new HashSet<Integer>();
		return result;
	}
	
	@Override
	public synchronized TileTexture remove(Object item) {
        if (! (item instanceof TileIndex)) {
            throw new IllegalArgumentException("Wrong type of key used in remove operation.");
        }
		TileTexture tile = map.remove(item);
		if (tile == null)
			return tile;
		// Stored OpenGL texture ID, if any.
		PyramidTexture texture1 = tile.getTexture();
		if (texture1 == null)
			return tile;
		int id = texture1.getTextureId();
		if (id > 0)
		    removedOpenGLTexturesSinceClear.add(id);
		return tile;
	}

	@Override
	public synchronized boolean containsKey(Object key) {
		return map.containsKey(key);
	}

	@Override
	public synchronized boolean containsValue(Object value) {
		return map.containsValue(value);
	}

	@Override
	public Set<Map.Entry<TileIndex, TileTexture>> entrySet() {
		return map.entrySet();
	}

	/**
	 * NOTE - get marks the retrieved item as most-recently-accessed.
	 * @param key
	 * @return
	 */
	@Override
	public TileTexture get(Object key) {
		return map.get(key);
	}

	public int getMaxEntries() {
		return maxEntries;
	}

	public void setMaxEntries(int maxEntries) {
		this.maxEntries = maxEntries;
	}

	public int getMaxSize() {
		return maxEntries;
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public Set<TileIndex> keySet() {
		return map.keySet();
	}

	/**
	 * NOTE - put marks the value as most-recently-accessed.
	 * @param key
	 * @param value
	 * @return
	 */
	@Override
	public synchronized TileTexture put(TileIndex key, TileTexture value) {

		int mapSize=map.size();
		//log.info("put() mapSize="+mapSize+" maxEntries="+maxEntries);
		boolean result = (map.size() > (maxEntries - 1));
		int i=0;

		if (result) {
			try {

				// We will iterate over all members, and remove the earlier 50%.
				// This will avoid constant calls to glDeleteTextureIds once we
				// hit the ceiling.
				int cutoff = mapSize / 2;
				List<TileIndex> keyList=new ArrayList<>();
				Iterator<TileIndex> it = map.keySet().iterator();
				while (it.hasNext()) {
					keyList.add(it.next());
				}
				//log.info("keyList has "+keyList.size()+" entries, cutoff="+cutoff);
				for (;(i<cutoff && i<keyList.size());i++) {
					TileIndex tileIndex=keyList.get(i);
					TileTexture tileTexture = map.get(tileIndex);
					if (tileTexture.getTexture() != null) {
						final int textureId = tileTexture.getTexture().getTextureId();
						obsoleteGlTextures.add(textureId);
					}
					map.remove(tileIndex);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			//log.info("Removed " + i + " TileTextures and tagged " + obsoleteGlTextures.size() + " textureIds as obsolete");
		}

		return map.put(key, value);
	}

	@Override
	public synchronized void putAll(Map<? extends TileIndex, ? extends TileTexture> m) {
		map.putAll(m);
	}

	@Override
	public int size() {
		return map.size();
	}

	@Override
	public Collection<TileTexture> values() {
		return map.values();
	}

    public void storeObsoleteTextureIds(int[] textureIds) {
        for (int i : textureIds)
            obsoleteGlTextures.add(i);
    }

}
