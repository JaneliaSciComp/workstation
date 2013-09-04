package org.janelia.it.FlyWorkstation.gui.slice_viewer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

public class TexturePreFetcher 
{
	// private static final Logger log = LoggerFactory.getLogger(TexturePreFetcher.class);

	private TextureCache textureCache; // holds texture
	private AbstractTextureLoadAdapter loadAdapter; // knows how to load textures
	private ThreadPoolExecutor textureLoadExecutor;
	// private Map<TileIndex, TileIndex> recentRequests; // LRU list of recent requests
	// private Set<TileIndex> queuedRequests = new HashSet<TileIndex>();
	private Map<Future<?>, TileIndex> futures = new HashMap<Future<?>, TileIndex>();

	public TexturePreFetcher(int threadPoolSize) {
		textureLoadExecutor = new ThreadPoolExecutor(
				threadPoolSize,
				threadPoolSize,
				0, TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>());
		// Remember recently requested textures, to avoid requesting them again right away
		/*
		recentRequests = new LinkedHashMap<TileIndex, TileIndex>(100, 0.75f, true) {
			private static final long serialVersionUID = 1L;
			@Override
			protected boolean removeEldestEntry(Map.Entry<TileIndex, TileIndex> eldest) {
				return size() > 100;
			}
		};
		*/
	}

	/**
	 * Like loadTexture, but emits an update signal when complete.
	 * @param quadtreeIndex
	 * 
	 * Returns "true" if this tile would occupy desired space in the future cache.
	 */
	public synchronized boolean loadDisplayedTexture(TileIndex index, TileServer tileServer) 
	{
		if (textureCache == null)
			return false;
		if (loadAdapter == null)
			return false;
		if (textureCache.getFutureCache().containsKey(index)) {
			textureCache.getFutureCache().get(index); // move cached texture to back of queue
			return true;
		}
		if (textureCache.containsKey(index))
			return false; // we already have this one!
		// TODO - is it already queued?
		// This "recentRequests" hack is not solving the problem.
		// if (recentRequests.containsKey(index))
		// 	return false;
		if (textureCache.isLoadQueued(index))
			return false;
		TileTexture texture = new TileTexture(index, loadAdapter);
		// TODO - handle MISSING textures vs. ERROR textures
		Future<?> foo = textureLoadExecutor.submit(new TextureLoadWorker(texture, textureCache));
		futures.put(foo, texture.getIndex());
		textureCache.setLoadQueued(index, true);
		// recentRequests.put(index, index);
		// Lowest resolution textures are in the persistent cache, and thus
		// do not impact the future cache.
		return (index.getZoom() != index.getMaxZoom());
	}
	
	public synchronized void clear() {
		if (textureCache == null)
			return;
		BlockingQueue<Runnable> blockingQueue = textureLoadExecutor.getQueue();
		if (blockingQueue == null)
			return;
		//synchronize on it to prevent tasks from being polled
		synchronized (blockingQueue) {
			//clear the Queue
			for (Runnable r : blockingQueue) {
				if (futures.containsKey(r))
					textureCache.setLoadQueued(futures.get(r), false);
			}
			blockingQueue.clear();
			/*
			Set<TileIndex> queued = textureCache.getQueuedRequests();
			if (queued == null)
				return;
			queued.clear(); // removes too many...		
			*/
			//or else copy its contents here with a while loop and remove()
		}
		futures.clear();
	}
	
	public AbstractTextureLoadAdapter getLoadAdapter() {
		return loadAdapter;
	}

	public void setLoadAdapter(AbstractTextureLoadAdapter loadAdapter) {
		this.loadAdapter = loadAdapter;
	}

	public TextureCache getTextureCache() {
		return textureCache;
	}

	public void setTextureCache(TextureCache textureCache) {
		this.textureCache = textureCache;
	}
	
}
