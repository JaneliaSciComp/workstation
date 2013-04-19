package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

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

	public TexturePreFetcher(int threadPoolSize) {
		textureLoadExecutor = new ThreadPoolExecutor(
				threadPoolSize,
				threadPoolSize,
				0, TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>());
	}

	/**
	 * Like loadTexture, but emits an update signal when complete.
	 * @param quadtreeIndex
	 */
	public synchronized void loadDisplayedTexture(TileIndex index, TileServer tileServer) 
	{
		if (textureCache == null)
			return;
		if (loadAdapter == null)
			return;
		if (textureCache.containsKey(index))
			return; // we already have this one!
		// TODO - is it already queued?
		TileTexture texture = new TileTexture(index, loadAdapter);
		texture.getRamLoadedSignal().connect(tileServer.getOnTextureLoadedSlot());
		// TODO - handle MISSING textures vs. ERROR textures
		textureLoadExecutor.submit(new TextureLoadWorker(texture, textureCache));
	}
	
	public synchronized void clear() {
		BlockingQueue<Runnable> blockingQueue = textureLoadExecutor.getQueue();
		//synchronize on it to prevent tasks from being polled
		synchronized (blockingQueue) {
			//clear the Queue
			blockingQueue.clear();
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
