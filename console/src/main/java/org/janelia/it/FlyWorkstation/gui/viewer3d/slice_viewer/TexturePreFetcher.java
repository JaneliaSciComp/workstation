package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
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
		Set<TileIndex> queuedRequests = textureCache.getQueuedRequests();
		synchronized(queuedRequests) {
			if (queuedRequests.contains(index))
				return false;
			TileTexture texture = new TileTexture(index, loadAdapter);
			texture.getRamLoadedSignal().connect(tileServer.getOnTextureLoadedSlot());
			// TODO - handle MISSING textures vs. ERROR textures
			textureLoadExecutor.submit(new TextureLoadWorker(texture, textureCache));
			queuedRequests.add(index);
			// recentRequests.put(index, index);
		}
		// Lowest resolution textures are in the persistent cache, and thus
		// do not impact the future cache.
		return (index.getZoom() != index.getMaxZoom());
	}
	
	public synchronized void clear() {
		BlockingQueue<Runnable> blockingQueue = textureLoadExecutor.getQueue();
		//synchronize on it to prevent tasks from being polled
		synchronized (blockingQueue) {
			//clear the Queue
			for (Runnable r : blockingQueue) { // removes too few
				if (r instanceof TextureLoadWorker) { // never happens
					TextureLoadWorker tlw = (TextureLoadWorker) r;
					TileIndex ix = tlw.getTexture().getIndex();
					textureCache.getQueuedRequests().remove(ix);
				}				
			}
			blockingQueue.clear();
			textureCache.getQueuedRequests().clear(); // removes too many...		
			//or else copy its contents here with a while loop and remove()
		}
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
