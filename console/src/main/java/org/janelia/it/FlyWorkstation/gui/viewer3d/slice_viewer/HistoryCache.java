package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

public class HistoryCache 
{
	// private static final Logger log = LoggerFactory.getLogger(HistoryCache.class);	
	
	// Dictionary to look up elements
	private Map<PyramidTileIndex, LRUTexture> map = new HashMap<PyramidTileIndex, LRUTexture>();
	// Buffer for opengl textures to delete when a context becomes available
	private Set<Integer> obsoleteGlTextures = new HashSet<Integer>();
	LRUTexture head = null;
	LRUTexture tail = null;

	private void addFirst(LRUTexture lru) {
		PyramidTileIndex ix = lru.texture.getIndex();
		if (head == null) // first element of empty cache
			tail = lru;
		else { // head != null
			if (head.texture.getIndex() == ix)
				return; // already head
			head.previous = lru;
		}
		lru.next = head;
		lru.previous = null;
		head = lru;
		map.put(ix, lru);
	}
	
	public synchronized void addFirst(TileTexture tex) {
		PyramidTileIndex ix = tex.getIndex();
		if (head != null) {
			if (head.texture.getIndex() == ix)
				return; // we're already there
		}
		if (map.containsKey(ix)) { // need to move from previous position to head
			LRUTexture oldItem = map.get(ix);
			// Splice out item from previous location
			if (oldItem.next != null)
				oldItem.next.previous = oldItem.previous;
			if (oldItem.previous != null)
				oldItem.previous.next = oldItem.next;
			// Insert at head
			addFirst(oldItem);
			return;
		}
		else { // this is a new texture, which could grow the cache size
			addFirst(new LRUTexture(tex));
			// TODO - make cache size limit user adjustable, or at least smart.
			while ((size() > 5000) && (removeLast() != null))
				;
			// log.info("cache size = "+size());
			// TODO - don't remove persistent items, like lowest resolution volume
		}
	}

	public synchronized void clear() {
		head = null;
		tail = null;
		// TODO - actually delete those OpenGL textures some day
		for (LRUTexture lru : map.values()) {
			if (lru == null)
				continue;
			TileTexture texture0 = lru.texture;
			if (texture0 == null)
				continue;
			PyramidTexture texture1 = texture0.getTexture();
			if (texture1 == null)
				continue;
			int id = texture1.getTextureId();
			if (id < 1)
				continue;
			obsoleteGlTextures.add(id);
		}
		map.clear();
	}

	public boolean containsKey(PyramidTileIndex ix) {
		return map.containsKey(ix);
	}

	public TileTexture get(PyramidTileIndex ix) {
		if (! map.containsKey(ix))
			return null;
		return map.get(ix).texture;
	}
	
	public TileTexture getFirst() {
		if (head == null)
			return null;
		return head.texture;
	}
	
	public int size() {
		return map.size();
	}

	public TileTexture removeLast() {
		if (tail == null)
			return null;
		TileTexture result = tail.texture;
		tail = tail.previous;
		map.remove(result.getIndex());
		if (map.isEmpty())
			head = null;
		if (result != null) {
			PyramidTexture texture1 = result.getTexture();
			if (texture1 != null) {
				int id = texture1.getTextureId();
				if (id > 0)
					obsoleteGlTextures.add(id);
			}
		}
		return result;
	}
	
	public Collection<TileTexture> values() {
		Vector<TileTexture> result = new Vector<TileTexture>();
		for (LRUTexture lru : map.values()) {
			result.add(lru.texture);
		}
		return result;
	}

	
	static class LRUTexture {
		LRUTexture next = null;
		LRUTexture previous = null;
		TileTexture texture;
		
		LRUTexture(TileTexture tex) {
			this.texture = tex;
		}
	}
}
