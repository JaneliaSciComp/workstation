package org.janelia.it.workstation.gui.large_volume_viewer;

import java.util.Date;
import org.janelia.it.workstation.geom.CoordinateAxis;
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
        Date start = new Date();
		TileIndex index = texture.getIndex();
		
		// log.info("Loading texture "+index+"...");
		
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
		else if (texture.loadImageToRam()) {
			textureCache.add(texture);
            tileServer.textureLoaded(texture.getIndex());
			// log.info("Loaded texture "+texture.getIndex());
            Date end = new Date();
            log.info("Elapsed time in loadImageToRam {}ms.", end.getTime() - start.getTime());
		}
		else {
			log.warn("Failed to load texture " + texture.getIndex());
		}
		textureCache.setLoadQueued(index, false);
	}

}
