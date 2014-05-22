package org.janelia.it.workstation.gui.slice_viewer;

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
	
	private org.janelia.it.workstation.gui.slice_viewer.TileTexture texture;
	private TextureCache textureCache;

	public TextureLoadWorker(org.janelia.it.workstation.gui.slice_viewer.TileTexture texture, TextureCache textureCache)
	{
		if (texture.getLoadStatus().ordinal() < org.janelia.it.workstation.gui.slice_viewer.TileTexture.LoadStatus.LOAD_QUEUED.ordinal())
			texture.setLoadStatus(org.janelia.it.workstation.gui.slice_viewer.TileTexture.LoadStatus.LOAD_QUEUED);
		this.texture = texture;
		this.textureCache = textureCache;
	}

	public org.janelia.it.workstation.gui.slice_viewer.TileTexture getTexture() {
		return texture;
	}

	@Override
	public void run() 
	{
		TileIndex index = texture.getIndex();
		
		// log.info("Loading texture "+index+"...");
		
		if (index.getSliceAxis() == CoordinateAxis.X) {
			// System.out.println("Y");
		}
		
		if (textureCache.containsKey(index)) {
			// log.info("Skipping duplicate load of texture (2) "+index);
		}
		// Don't load this texture if it is already loaded
		else if (texture.getLoadStatus().ordinal() == org.janelia.it.workstation.gui.slice_viewer.TileTexture.LoadStatus.RAM_LOADING.ordinal())
		{
			// log.info("Skipping duplicate load of texture "+texture.getIndex());
			// return; // already loading
		}
		else if (texture.getLoadStatus().ordinal() > org.janelia.it.workstation.gui.slice_viewer.TileTexture.LoadStatus.RAM_LOADING.ordinal())
		{
			// log.info("Skipping duplicate load of texture "+texture.getIndex());
			// return; // already loaded or loading
		}
		// Load file
		// log.info("Loading texture "+texture.getIndex());
		else if (texture.loadImageToRam()) {
			textureCache.add(texture);
			textureCache.textureLoadedSignal.emit(texture.getIndex()); // inform consumers (RavelerTileServer?)
			// log.info("Loaded texture "+texture.getIndex());
		}
		else {
			log.warn("Failed to load texture " + texture.getIndex());
		}
		textureCache.setLoadQueued(index, false);
	}

}
