package org.janelia.it.workstation.gui.slice_viewer;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HistoryCache
implements Map<org.janelia.it.workstation.gui.slice_viewer.TileIndex, org.janelia.it.workstation.gui.slice_viewer.TileTexture>
{
	private static final Logger log = LoggerFactory.getLogger(HistoryCache.class);	
	
	// Store deleted opengl texture ids, for deferred disposal.
	private Set<Integer> obsoleteGlTextures = new HashSet<Integer>();
	private Set<Integer> removedOpenGLTexturesSinceClear = new HashSet<Integer>();
	// Flag to help report when cache has filled.
	private boolean fullReported = false;
	// To allow lookup by index
	private Map<org.janelia.it.workstation.gui.slice_viewer.TileIndex, org.janelia.it.workstation.gui.slice_viewer.TileTexture> map;
	private int maxEntries;

	public HistoryCache(int maxSize) {
		this.maxEntries = maxSize;
		// Create a synchronized, least-recently-used map container, with a size limit.
		// Use synchronizedMap() to generate thread-safe container.
		map = Collections.synchronizedMap(new LinkedHashMap<org.janelia.it.workstation.gui.slice_viewer.TileIndex, org.janelia.it.workstation.gui.slice_viewer.TileTexture>(
				maxSize, 0.75f, true) { // "true" for access-order, based on get()/add() calls
			private static final long serialVersionUID = 1L;
			@Override
			// Fix size of LRU cache
			protected boolean removeEldestEntry(Map.Entry<org.janelia.it.workstation.gui.slice_viewer.TileIndex, org.janelia.it.workstation.gui.slice_viewer.TileTexture> eldest) {
				boolean result = (size() > maxEntries);
				if (result && ! fullReported) {
					log.info("cache full");
					fullReported = true;
				}
				return result;
			}
		});
	}

	@Override
	public synchronized void clear() {
		for (org.janelia.it.workstation.gui.slice_viewer.TileTexture tile : map.values()) {
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
		fullReported = false;
	}

	public Set<Integer> popObsoleteGlTextures() {
		Set<Integer> result = obsoleteGlTextures;
		obsoleteGlTextures = new HashSet<Integer>();
		return result;
	}
	
	@Override
	public org.janelia.it.workstation.gui.slice_viewer.TileTexture remove(Object item) {
		org.janelia.it.workstation.gui.slice_viewer.TileTexture tile = map.remove(item);
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
	public boolean containsKey(Object key) {
		return map.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return map.containsValue(value);
	}

	@Override
	public Set<java.util.Map.Entry<org.janelia.it.workstation.gui.slice_viewer.TileIndex, org.janelia.it.workstation.gui.slice_viewer.TileTexture>> entrySet() {
		return map.entrySet();
	}

	/**
	 * NOTE - get marks the retrieved item as most-recently-accessed.
	 * @param key
	 * @return
	 */
	@Override
	public org.janelia.it.workstation.gui.slice_viewer.TileTexture get(Object key) {
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
	public Set<org.janelia.it.workstation.gui.slice_viewer.TileIndex> keySet() {
		return map.keySet();
	}

	/**
	 * NOTE - put marks the value as most-recently-accessed.
	 * @param key
	 * @param value
	 * @return
	 */
	@Override
	public org.janelia.it.workstation.gui.slice_viewer.TileTexture put(org.janelia.it.workstation.gui.slice_viewer.TileIndex key, org.janelia.it.workstation.gui.slice_viewer.TileTexture value) {
		return map.put(key, value);
	}

	@Override
	public void putAll(Map<? extends org.janelia.it.workstation.gui.slice_viewer.TileIndex, ? extends org.janelia.it.workstation.gui.slice_viewer.TileTexture> m) {
		map.putAll(m);
	}

	@Override
	public int size() {
		return map.size();
	}

	@Override
	public Collection<org.janelia.it.workstation.gui.slice_viewer.TileTexture> values() {
		return map.values();
	}

    public void storeObsoleteTextureIds(int[] textureIds) {
        for (int i : textureIds)
            obsoleteGlTextures.add(i);
    }

}
