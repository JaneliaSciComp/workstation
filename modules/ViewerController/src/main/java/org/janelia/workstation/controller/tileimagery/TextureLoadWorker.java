package org.janelia.workstation.controller.tileimagery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads a TileTexture image into memory. Should be used in a worker thread.
 *
 * @author brunsc
 *
 */
public class TextureLoadWorker implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(TextureLoadWorker.class);

    private TileTexture texture;
    private TextureCache textureCache;
    private TileServer tileServer;

    TextureLoadWorker(TileTexture texture, TextureCache textureCache, TileServer tileServer) {
        if (texture.getLoadStatus().ordinal() < TileTexture.LoadStatus.LOAD_QUEUED.ordinal()) {
            texture.setLoadStatus(TileTexture.LoadStatus.LOAD_QUEUED);
        }
        this.texture = texture;
        this.textureCache = textureCache;
        this.tileServer = tileServer;
    }

    public TileTexture getTexture() {
        return texture;
    }

    @Override
    public void run() {
        TileIndex index = texture.getIndex();
        LOG.debug("loading "+index);
        if (textureCache.containsKey(index)) {
            // texture already loaded
            LOG.debug("Skipping duplicate load of texture (2) {}", index);
        } else if (texture.getLoadStatus().ordinal() == TileTexture.LoadStatus.RAM_LOADING.ordinal()) {
            // texture currently loading
            LOG.debug("Skipping duplicate load of texture {}", texture.getIndex());
        } else if (texture.getLoadStatus().ordinal() > TileTexture.LoadStatus.RAM_LOADING.ordinal()) {
            // texture currently loaded or loading
            LOG.debug("Skipping duplicate load of texture {}", texture.getIndex());
        } else {
            // load texture
            boolean loadedSuccessfully = texture.loadImageToRam();
            LOG.debug("loadedSuccessfully={} loadStatus={}", loadedSuccessfully, texture.getLoadStatus());
            if (loadedSuccessfully) {
                textureCache.add(texture);
                tileServer.textureLoaded(texture.getIndex());
            }
        }

        textureCache.setLoadQueued(index, false);
    }

}
