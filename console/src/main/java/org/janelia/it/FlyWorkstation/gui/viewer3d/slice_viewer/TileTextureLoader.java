package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

/**
 * Loads a TileTexture image into memory. Should be used in a worker thread.
 * 
 * @author brunsc
 *
 */
public class TileTextureLoader implements Runnable 
{
	private TileTexture texture;
	private RavelerTileServer tileServer;

	public TileTextureLoader(TileTexture texture, RavelerTileServer tileServer) 
	{
		if (texture.getStage().ordinal() < TileTexture.Stage.LOAD_QUEUED.ordinal())
			texture.setStage(TileTexture.Stage.LOAD_QUEUED);
		this.texture = texture;
		this.tileServer = tileServer;
	}

	@Override
	public void run() 
	{
		// Don't load this texture if it is already loaded
		if (texture.getStage().ordinal() >= TileTexture.Stage.RAM_LOADING.ordinal())
			return; // already loaded or loading
		// Don't load this texture if it is no longer needed
		if (! tileServer.getNeededTextures().contains(texture.getIndex())) {
			if (texture.getStage() == TileTexture.Stage.LOAD_QUEUED)
				// revert to not-queued
				texture.setStage(TileTexture.Stage.UNINITIALIZED);
			return;
		}
		// Load file
		if (texture.loadImageToRam())
			texture.getRamLoadedSignal().emit(); // inform consumers (RavelerTileServer?)
	}

}
