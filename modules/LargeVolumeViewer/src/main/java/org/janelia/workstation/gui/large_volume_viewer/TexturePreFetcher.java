package org.janelia.workstation.gui.large_volume_viewer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TexturePreFetcher {

    private static final Logger log = LoggerFactory.getLogger(TexturePreFetcher.class);

    private static final String TEX_FETCH_THREADNAME_PREFIX = "TexturePreFetch";
    private final ThreadPoolExecutor textureLoadExecutor;
    private final Map<Future<?>, TileIndex> futures = new HashMap<>();
    private TextureCache textureCache; // holds texture
    private AbstractTextureLoadAdapter loadAdapter; // knows how to load textures

    public TexturePreFetcher(int threadPoolSize) {
        textureLoadExecutor = new ThreadPoolExecutor(
                threadPoolSize,
                threadPoolSize,
                0, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                new CustomNamedThreadFactory(TEX_FETCH_THREADNAME_PREFIX)
        );
    }

    /**
     * Like loadTexture, but emits an update signal when complete.
     *
     * Returns "true" if this tile would occupy desired space in the future
     * cache.
     */
    public synchronized boolean loadDisplayedTexture(TileIndex index, TileServer tileServer) {
        if (textureCache == null) {
            log.trace("loadDisplayedTexture - return because textureCache is null");
            return false;
        }
        if (loadAdapter == null) {
            log.trace("loadDisplayedTexture - return because loadAdapter is null");
            return false;
        }
        if (textureCache.getFutureCache().containsKey(index)) {
            log.trace("loadDisplayedTexture - already in future cache");
            textureCache.getFutureCache().get(index); // move cached texture to back of queue
            return true;
        }
        if (textureCache.containsKey(index)) {
            log.trace("loadDisplayedTexture - already in current cache");
            return false; // we already have this one!
        }
        if (textureCache.isLoadQueued(index)) {
            log.trace("loadDisplayedTexture - already queued");
            return false;
        }
        TileTexture texture = new TileTexture(index, loadAdapter);
        TextureLoadWorker textureLoadWorker = new TextureLoadWorker(texture, textureCache, tileServer);
        // TODO - handle MISSING textures vs. ERROR textures
        Future<?> foo = textureLoadExecutor.submit(textureLoadWorker);
        futures.put(foo, texture.getIndex());
        textureCache.setLoadQueued(index, true);
        // Lowest resolution textures are in the persistent cache, and thus
        // do not impact the future cache.
        log.debug("loadDisplayedTexture - queued for download: {}", index);
        return (index.getZoom() != index.getMaxZoom());
    }

    public synchronized void clear() {
        if (textureCache == null) {
            return;
        }
        BlockingQueue<Runnable> blockingQueue = textureLoadExecutor.getQueue();
        if (blockingQueue == null) {
            return;
        }
        //synchronize on it to prevent tasks from being polled
        synchronized (blockingQueue) {
            //clear the Queue
            for (Runnable r : blockingQueue) {
                if (futures.containsKey(r)) {
                    textureCache.setLoadQueued(futures.get(r), false);
                }
            }
            blockingQueue.clear();
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
