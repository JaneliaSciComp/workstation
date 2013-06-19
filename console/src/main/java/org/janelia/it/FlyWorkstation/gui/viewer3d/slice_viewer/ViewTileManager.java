package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.CoordinateAxis;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Rotation3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Camera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Viewport;

/**
 * ViewTileManager is a per-viewer implementation of tile management that
 * used to be in the (per-specimen) TileServer class.
 * @author brunsc
 *
 */
public class ViewTileManager {
	/*
	 * A TileSet is a group of rectangles that complete the SliceViewer image
	 * display.
	 * 
	 * Three TileSets are maintained:
	 * 1) Latest tiles : the tiles representing the current view
	 * 2) LastGood tiles : the most recent tile set that could be successfully 
	 *    displayed.
	 * 3) Emergency tiles : a tile set that is updated with moderate frequency.
	 * 
	 * We would always prefer to display the Latest tiles. But frequently
	 * the image data for those tiles are not yet available. So we choose
	 * among the three tile sets to give the best appearance of a responsive
	 * interface.
	 * 
	 * The tricky part occurs when the user is rapidly changing the view,
	 * faster than we can load the tile images. We load tile images in
	 * multiple threads, but still it is not always possible to keep up. So
	 * one important optimization is to first insert every desired tile image
	 * into the load queue, but then when it is time to actually load an image,
	 * make another check to ensure that the image is still desired. Otherwise
	 * the view can fall farther and farther behind the current state.
	 * 
	 * One approach is to display Latest tiles if they are ready, or the
	 * LastGood tiles otherwise. The problem with this approach is that if
	 * the user is rapidly changing the view, there is never time to fully
	 * update the Latest tiles before they become stale. So the user just
	 * sees a static ancient LastGood tile set. Precisely when the user most
	 * hopes to see things moving fast.  That is where 'emergency' tiles
	 * come in.
	 * 
	 * Sets of emergency tiles are fully loaded as fast as possible, but
	 * no faster. They are not dropped from the load queue, nor are they
	 * updated until the previous set of emergency tiles has loaded and
	 * displayed. During rapid user interaction, the use of emergency
	 * tiles allows the scene to update in the fastest possible way, giving
	 * the comforting impression of responsiveness. 
	 */
	// Latest tiles list stores the current desired tile set, even if
	// not all of the tiles are ready.
	private TileSet latestTiles;
	// Emergency tiles list stores a recently displayed view, so that
	// SOMETHING gets displayed while the current view is being loaded.
	private TileSet emergencyTiles;
	// LastGoodTiles always hold a displayable tile set, even when emergency
	// tiles are loading.
	private TileSet lastGoodTiles;
	private Set<TileIndex> neededTextures = new HashSet<TileIndex>();

	// signal for tile loaded
	private double zoomOffset = 0.5; // tradeoff between optimal resolution (0.0) and speed.
	private TileSet previousTiles;

	private TileConsumer tileConsumer;
	private TextureCache textureCache;
	private SharedVolumeImage volumeImage;
	
	public Signal1<TileSet> tileSetChangedSignal = new Signal1<TileSet>();

	public ViewTileManager(TileConsumer tileConsumer) 
	{
		this.tileConsumer = tileConsumer;
	}

	public void clear() {
		emergencyTiles = null;
		if (latestTiles != null)
			latestTiles.clear();
		if (lastGoodTiles != null)
			lastGoodTiles.clear();
	}
	
	public TileSet createLatestTiles()
	{
		return createLatestTiles(tileConsumer);
	}
	
	protected TileSet createLatestTiles(TileConsumer tileConsumer)
	{
		return createLatestTiles(tileConsumer.getCamera(), 
				tileConsumer.getViewport(),
				tileConsumer.getSliceAxis(),
				tileConsumer.getViewerInGround());
	}
	
	// TODO generalize for non-Z axes
	public TileSet createLatestTiles(Camera3d camera, Viewport viewport,
			CoordinateAxis sliceAxis, Rotation3d viewerInGround)
	{
		TileSet result = new TileSet();
		if (volumeImage.getLoadAdapter() == null)
			return result;

		// Need to loop over x and y
		// Need to compute z, and zoom
		// 1) zoom
		double maxRes = Math.min(
				volumeImage.getXResolution(), 
				Math.min(volumeImage.getYResolution(), 
						volumeImage.getZResolution()));
		double voxelsPerPixel = 1.0 / (camera.getPixelsPerSceneUnit() * maxRes);
		int zoom = 20; // default to very coarse zoom
		if (voxelsPerPixel > 0.0) {
			double topZoom = Math.log(voxelsPerPixel) / Math.log(2.0);
			zoom = (int)(topZoom + zoomOffset);
		}
		int zoomMin = 0;
		TileFormat tileFormat = volumeImage.getLoadAdapter().getTileFormat();
		int zoomMax = tileFormat.getZoomLevelCount() - 1;
		zoom = Math.max(zoom, zoomMin);
		zoom = Math.min(zoom, zoomMax);
		// 2) z
		Vec3 focus = camera.getFocus();
		int z = (int)Math.round(focus.getZ() / volumeImage.getZResolution() - 0.5);
		// 3) x and y range
		// In scene units
		// Clip to screen space
		double xFMin = focus.getX() - 0.5*viewport.getWidth()/camera.getPixelsPerSceneUnit();
		double xFMax = focus.getX() + 0.5*viewport.getWidth()/camera.getPixelsPerSceneUnit();
		double yFMin = focus.getY() - 0.5*viewport.getHeight()/camera.getPixelsPerSceneUnit();
		double yFMax = focus.getY() + 0.5*viewport.getHeight()/camera.getPixelsPerSceneUnit();
		// Clip to volume space
		// Subtract one half pixel to avoid loading an extra layer of tiles
		double dx = 0.25 * tileFormat.getVoxelMicrometers()[0];
		double dy = 0.25 * tileFormat.getVoxelMicrometers()[1];
		BoundingBox3d bb = volumeImage.getBoundingBox3d();
		xFMin = Math.max(xFMin, bb.getMin().getX() + dx);
		yFMin = Math.max(yFMin, bb.getMin().getY() + dy);
		xFMax = Math.min(xFMax, bb.getMax().getX() - dx);
		yFMax = Math.min(yFMax, bb.getMax().getY() - dy);
		double zoomFactor = Math.pow(2.0, zoom);
		// get tile pixel size 1024 from loadAdapter
		int tileSize[] = tileFormat.getTileSize();
		double tileWidth = tileSize[0] * zoomFactor * volumeImage.getXResolution();
		double tileHeight = tileSize[1] * zoomFactor * volumeImage.getYResolution();
		// In tile units
		int xMin = (int)Math.floor(xFMin / tileWidth);
		int xMax = (int)Math.floor(xFMax / tileWidth);
		
		// Correct for bottom Y origin of Raveler tile coordinate system
		// (everything else is top Y origin: image, our OpenGL, user facing coordinate system)
		double bottomY = bb.getMax().getY();
		int yMin = (int)Math.floor((bottomY - yFMax) / tileHeight);
		int yMax = (int)Math.floor((bottomY - yFMin) / tileHeight);
		
		TileIndex.IndexStyle indexStyle = tileFormat.getIndexStyle();
		for (int x = xMin; x <= xMax; ++x) {
			for (int y = yMin; y <= yMax; ++y) {
				TileIndex key = new TileIndex(x, y, z, zoom, 
						zoomMax, indexStyle, CoordinateAxis.Z);
				Tile2d tile = new Tile2d(key, tileFormat);
				tile.setYMax(bb.getMax().getY()); // To help flip y
				result.add(tile);
			}
		}
		
		return result;
	}
	
	public TextureCache getTextureCache() {
		return textureCache;
	}

	public void setTextureCache(TextureCache textureCache) {
		this.textureCache = textureCache;
	}

	public TileConsumer getTileConsumer() {
		return tileConsumer;
	}

	public SharedVolumeImage getVolumeImage() {
		return volumeImage;
	}

	public void setVolumeImage(SharedVolumeImage volumeImage) {
		this.volumeImage = volumeImage;
	}

	// Produce a list of renderable tiles to complete this view
	public TileSet updateDisplayTiles() 
	{
		// Update latest tile set
		latestTiles = createLatestTiles();
		latestTiles.assignTextures(textureCache);

		// Push latest textures to front of LRU cache
		for (Tile2d tile : latestTiles) {
			TileTexture texture = tile.getBestTexture();
			if (texture == null)
				continue;
			textureCache.markHistorical(texture);
		}

		// Need to assign textures to emergency tiles too...
		if (emergencyTiles != null)
			emergencyTiles.assignTextures(textureCache);
		
		// Maybe initialize emergency tiles
		if (emergencyTiles == null)
			emergencyTiles = latestTiles;
		if (emergencyTiles.size() < 1)
			emergencyTiles = latestTiles;

		// Which tile set will we display this time?
		TileSet result = latestTiles;
		if (latestTiles.canDisplay()) {
			// log.info("Using Latest tiles");
			emergencyTiles = latestTiles;
			lastGoodTiles = latestTiles;
			result = latestTiles;
		}
		else if (emergencyTiles.canDisplay()) {
			// log.info("Using Emergency tiles");
			lastGoodTiles = emergencyTiles;
			result = emergencyTiles;
			// These emergency tiles will now be displayed.
			// So start a new batch of emergency tiles
			emergencyTiles = latestTiles; 
		}
		else {
			// log.info("Using LastGood tiles");
			// Fall back to a known displayable
			result = lastGoodTiles;
		}
		
		// Keep working on loading both emergency and latest tiles only.
		Set<TileIndex> newNeededTextures = new LinkedHashSet<TileIndex>();
		newNeededTextures.addAll(emergencyTiles.getFastNeededTextures());
		// Decide whether to load fastest textures or best textures
		Tile2d.Stage stage = latestTiles.getMinStage();
		if (stage.ordinal() < Tile2d.Stage.COARSE_TEXTURE_LOADED.ordinal())
			// First load the fast ones
			newNeededTextures.addAll(latestTiles.getFastNeededTextures());
		// Then load the best ones
		newNeededTextures.addAll(latestTiles.getBestNeededTextures());
		// Use set/getNeededTextures() methods for thread safety
		// log.info("Needed textures:");
		/*
		for (TileIndex ix : newNeededTextures) {
			log.info("  "+ix);
		}
		*/
		synchronized(neededTextures) {
			neededTextures.clear();
			neededTextures.addAll(newNeededTextures);
		}
		// queueTextureLoad(getNeededTextures());
		
		// put tile set changed signal here
		if (! latestTiles.equals(previousTiles)) {
			previousTiles = latestTiles;
			tileSetChangedSignal.emit(result);
		}
		
		return result;
	}	

	public TileSet getLatestTiles() {
		return latestTiles;
	}
	
	public Set<TileIndex> getNeededTextures() {
		return neededTextures;
	}
	
}
