package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import java.awt.image.BufferedImage;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * An constrained-size image cache with an LRU eviction policy. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ImageCache {

	public static final int MAX_NUM_IMAGES = 500; // 500 images total
//	public static final int MAX_PIXELS_CACHED = 1048576 * 1024; // 1 GB of 8-bit images
	
	private final Cache<String, BufferedImage> cache;

	public ImageCache() {
		this(MAX_NUM_IMAGES);
	}
	
	public ImageCache(int size) {
//		Weigher weigher = new Weigher<String, BufferedImage>() {
//			public int weigh(String filename, BufferedImage image) {
//	            return image.getData().getWidth()*image.getData().getHeight();
//	          }
//		};
		this.cache = CacheBuilder.newBuilder()
			.concurrencyLevel(16)
			.maximumSize(size)
			// weight is better, but running the Weigher may impact performance and should be profiled before use
//			.weigher(weigher)
//			.maximumWeight(size)
			.softValues()
			.build();
	}
	
	public boolean contains(String identifier) {
		return cache.getIfPresent(identifier)!=null;
	}
	
	public BufferedImage get(String identifier) {
		return cache.getIfPresent(identifier);
	}
	
	public BufferedImage put(String identifier, BufferedImage image) {
		cache.put(identifier, image);
		return image;
	}
}
