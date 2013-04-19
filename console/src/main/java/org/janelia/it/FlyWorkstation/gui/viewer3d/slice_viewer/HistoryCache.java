package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HistoryCache 
{
	private static final Logger log = LoggerFactory.getLogger(HistoryCache.class);	
	
	// Dictionary to look up elements
	private Map<TileIndex, LRUTexture> map = new HashMap<TileIndex, LRUTexture>();
	// Buffer for opengl textures to delete when a context becomes available
	private Set<Integer> obsoleteGlTextures = new HashSet<Integer>();
	private LRUTexture head = null;
	private LRUTexture tail = null;
	private int maxSize = 300;
	private boolean fullReported = false;

	private void addFirst(LRUTexture lru) {
		TileIndex ix = lru.texture.getIndex();
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
		TileIndex ix = tex.getIndex();
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
			while ((size() > maxSize) && (removeLast() != null))
				;
			// log.info("cache size = "+size());
		}
	}

	public synchronized void clear() {
		head = null;
		tail = null;
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
			obsoleteGlTextures.add(id); // remember OpenGl texture IDs for later deletion.
		}
		map.clear();
		fullReported = false;
	}

	public boolean containsKey(TileIndex ix) {
		return map.containsKey(ix);
	}

	public TileTexture get(TileIndex ix) {
		if (! map.containsKey(ix))
			return null;
		return map.get(ix).texture;
	}
	
	public TileTexture getFirst() {
		if (head == null)
			return null;
		return head.texture;
	}
	
	public int getMaxSize() {
		return maxSize;
	}

	public Set<Integer> popObsoleteGlTextures() {
		Set<Integer> result = obsoleteGlTextures;
		obsoleteGlTextures = new HashSet<Integer>();
		return result;
	}

	public void setMaxSize(int maxSize) {
		this.maxSize = maxSize;
	}

	public int size() {
		return map.size();
	}

	public synchronized TileTexture removeLast() {
		if (! fullReported) {
			log.info("Cache is full");
			fullReported = true;
		}
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
		result.releaseMemory();
		return result;
	}
	
	public synchronized TileTexture remove(TileIndex index) {
		if (! map.containsKey(index))
			return null;
		LRUTexture result = map.get(index);
		if (result.next != null)
			result.next.previous = result.previous;
		if (result.previous != null)
			result.previous.next = result.next;
		map.remove(index);
		return result.texture;
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
