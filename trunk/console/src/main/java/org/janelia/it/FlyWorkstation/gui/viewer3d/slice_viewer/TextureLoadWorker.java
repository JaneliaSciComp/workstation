package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads a TileTexture image into memory. Should be used in a worker thread.
 * 
 * @author brunsc
 *
 */
public class TextureLoadWorker implements Runnable 
{
	private static final Logger log = LoggerFactory.getLogger(TextureLoadWorker.class);
	
	private TileTexture texture;
	private TextureCache textureCache;

	public TextureLoadWorker(TileTexture texture, TextureCache textureCache) 
	{
		if (texture.getStage().ordinal() < TileTexture.Stage.LOAD_QUEUED.ordinal())
			texture.setStage(TileTexture.Stage.LOAD_QUEUED);
		this.texture = texture;
		this.textureCache = textureCache;
	}

	@Override
	public void run() 
	{
		// Don't load this texture if it is already loaded
		if (texture.getStage().ordinal() == TileTexture.Stage.RAM_LOADING.ordinal())
		{
			log.info("Skipping duplicate load of texture "+texture.getIndex());
			return; // already loading
		}
		else if (texture.getStage().ordinal() > TileTexture.Stage.RAM_LOADING.ordinal())
		{
			// log.info("Skipping duplicate load of texture "+texture.getIndex());
			return; // already loaded or loading
		}
		// Load file
		// log.info("Loading texture "+texture.getIndex());
		if (texture.loadImageToRam()) {
			textureCache.add(texture);
			texture.getRamLoadedSignal().emit(texture.getIndex()); // inform consumers (RavelerTileServer?)
			// log.info("Loaded texture "+texture.getIndex());
		}
		else {
			log.warn("Failed to load texture " + texture.getIndex());
		}
	}

}
