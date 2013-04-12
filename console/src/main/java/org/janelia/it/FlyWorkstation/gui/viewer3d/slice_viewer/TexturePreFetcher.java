package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

public class TexturePreFetcher 
{
	// private static final Logger log = LoggerFactory.getLogger(TexturePreFetcher.class);

	private TextureCache textureCache; // holds texture
	private PyramidTextureLoadAdapter loadAdapter; // knows how to load textures
	private int threadPoolSize = 4;
	private ExecutorService textureLoadExecutor = Executors.newFixedThreadPool(threadPoolSize);

	public TexturePreFetcher() {}

	public void loadTexture(PyramidTileIndex quadtreeIndex) 
	{
		if (textureCache == null)
			return;
		if (loadAdapter == null)
			return;
		TileTexture texture = textureCache.getOrCreate(quadtreeIndex, loadAdapter);
		if (texture.getStage().ordinal() >= TileTexture.Stage.LOAD_QUEUED.ordinal())
			return; // texture load already started
		textureLoadExecutor.submit(new PreFetchTextureLoadWorker(texture));
	}
	
	public void clear() {
		if (textureLoadExecutor != null)
			textureLoadExecutor.shutdownNow();
		textureLoadExecutor = Executors.newFixedThreadPool(threadPoolSize);
	}
	
	public PyramidTextureLoadAdapter getLoadAdapter() {
		return loadAdapter;
	}

	public void setLoadAdapter(PyramidTextureLoadAdapter loadAdapter) {
		this.loadAdapter = loadAdapter;
	}

	public int getThreadPoolSize() {
		return threadPoolSize;
	}

	public void setThreadPoolSize(int threadPoolSize) {
		this.threadPoolSize = threadPoolSize;
	}

	public TextureCache getTextureCache() {
		return textureCache;
	}

	public void setTextureCache(TextureCache textureCache) {
		this.textureCache = textureCache;
	}
	
}
