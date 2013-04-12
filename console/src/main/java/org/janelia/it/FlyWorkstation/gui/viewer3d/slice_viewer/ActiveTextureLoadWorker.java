package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads a TileTexture image into memory. Should be used in a worker thread.
 * 
 * @author brunsc
 *
 */
public class ActiveTextureLoadWorker implements Runnable 
{
	private static final Logger log = LoggerFactory.getLogger(ActiveTextureLoadWorker.class);
	
	private TileTexture texture;
	private SliceActor sliceActor;

	public ActiveTextureLoadWorker(TileTexture texture, SliceActor sliceActor) 
	{
		if (texture.getStage().ordinal() < TileTexture.Stage.LOAD_QUEUED.ordinal())
			texture.setStage(TileTexture.Stage.LOAD_QUEUED);
		this.texture = texture;
		this.sliceActor = sliceActor;
	}

	@Override
	public void run() 
	{
		// Don't load this texture if it is already loaded
		if (texture.getStage().ordinal() >= TileTexture.Stage.RAM_LOADING.ordinal())
		{
			// log.info("Skipping duplicate load of texture "+texture.getIndex());
			return; // already loaded or loading
		}
		// Don't load this texture if it is no longer needed
		if (! sliceActor.getNeededTextures().contains(texture.getIndex())) {
			if (texture.getStage() == TileTexture.Stage.LOAD_QUEUED)
				// revert to not-queued
				texture.setStage(TileTexture.Stage.UNINITIALIZED);
			// log.info("Skipping obsolete load of texture "+texture.getIndex());
			return;
		}
		// Load file
		// log.info("Loading texture "+texture.getIndex());
		if (texture.loadImageToRam())
			texture.getRamLoadedSignal().emit(); // inform consumers (RavelerTileServer?)
		else {
			// log.warn("Failed to load texture " + texture.getIndex());
		}
	}

}
