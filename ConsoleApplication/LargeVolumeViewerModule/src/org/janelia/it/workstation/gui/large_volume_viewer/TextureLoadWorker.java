package org.janelia.it.workstation.gui.large_volume_viewer;

import org.janelia.it.jacs.shared.lvv.TileIndex;
import org.janelia.it.jacs.shared.geom.CoordinateAxis;
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
    private TileServer tileServer;

	public TextureLoadWorker(TileTexture texture, TextureCache textureCache, TileServer tileServer)
	{
		if (texture.getLoadStatus().ordinal() < TileTexture.LoadStatus.LOAD_QUEUED.ordinal())
			texture.setLoadStatus(TileTexture.LoadStatus.LOAD_QUEUED);
		this.texture = texture;
		this.textureCache = textureCache;
        this.tileServer = tileServer;
	}

	public TileTexture getTexture() {
		return texture;
	}

	@Override
	public void run() 
	{
		TileIndex index = texture.getIndex();
		
		if (index.getSliceAxis() == CoordinateAxis.X) {
			// System.out.println("Y");
		}
		
		if (textureCache.containsKey(index)) {
			// log.info("Skipping duplicate load of texture (2) "+index);
		}
		// Don't load this texture if it is already loaded
		else if (texture.getLoadStatus().ordinal() == TileTexture.LoadStatus.RAM_LOADING.ordinal())
		{
			// log.info("Skipping duplicate load of texture "+texture.getIndex());
			// return; // already loading
		}
		else if (texture.getLoadStatus().ordinal() > TileTexture.LoadStatus.RAM_LOADING.ordinal())
		{
			// log.info("Skipping duplicate load of texture "+texture.getIndex());
			// return; // already loaded or loading
		}
		// Load file
		// log.info("Loading texture "+texture.getIndex());
		else {
			boolean loadedSuccessfully=texture.loadImageToRam();

			if (loadedSuccessfully) {
				textureCache.add(texture);
				tileServer.textureLoaded(texture.getIndex());
			}
		}

		textureCache.setLoadQueued(index, false);
	}

}
