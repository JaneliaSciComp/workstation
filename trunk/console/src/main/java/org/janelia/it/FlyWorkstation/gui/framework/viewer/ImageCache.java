package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.Map;

import org.janelia.it.FlyWorkstation.shared.util.LRUCache;

/**
 * An constrained-size image cache with an LRU eviction policy. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ImageCache {

	public static final int DEFAULT_CACHE_SIZE = 500;
	
	private final Map<String, BufferedImage> cache;

	public ImageCache() {
		this(DEFAULT_CACHE_SIZE);
	}
	
	public ImageCache(int size) {
		this.cache = Collections.synchronizedMap(new LRUCache<String, BufferedImage>(size));
	}
	
	public boolean contains(String identifier) {
		return cache.containsKey(identifier);
	}
	
	public BufferedImage get(String identifier) {
		return cache.get(identifier);
	}
	
	public BufferedImage put(String identifier, BufferedImage image) {
		return cache.put(identifier, image);
	}
	
	public int size() {
		return cache.size();
	}
}
