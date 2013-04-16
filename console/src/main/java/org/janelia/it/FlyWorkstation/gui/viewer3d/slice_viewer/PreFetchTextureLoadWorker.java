package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads a TileTexture image into memory. Should be used in a worker thread.
 * 
 * @author brunsc
 *
 */
public class PreFetchTextureLoadWorker implements Runnable 
{
	private static final Logger log = LoggerFactory.getLogger(PreFetchTextureLoadWorker.class);

	private TileTexture texture;
	private boolean cancelled = false;

	public synchronized boolean isCancelled() {
		return cancelled;
	}

	public synchronized void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	public PreFetchTextureLoadWorker(TileTexture texture) 
	{
		if (texture.getStage().ordinal() < TileTexture.Stage.LOAD_QUEUED.ordinal())
			texture.setStage(TileTexture.Stage.LOAD_QUEUED);
		this.texture = texture;
	}

	@Override
	public void run() 
	{
		// log.info("loading texture "+texture.getIndex());
		if (isCancelled())
			return;
		
		// Don't load this texture if it is already loaded
		if (texture.getStage().ordinal() == TileTexture.Stage.RAM_LOADING.ordinal()) {
			log.info("Texture already being loaded in another thread");
			return; // already loading
		}
		else if (texture.getStage().ordinal() > TileTexture.Stage.RAM_LOADING.ordinal())
			return; // already loaded

		// Load file
		log.info("Loading texture "+texture.getIndex());
		texture.loadImageToRam();
	}
}
