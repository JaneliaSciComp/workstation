package org.janelia.it.FlyWorkstation.gui.slice_viewer;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.CoordinateAxis;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Rotation3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Camera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Viewport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ViewTileManager is a per-viewer implementation of tile management that
 * used to be in the (per-specimen) TileServer class.
 * @author brunsc
 *
 */
public class ViewTileManager {
	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory.getLogger(ViewTileManager.class);

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
	
	public static enum LoadStatus {
		NO_TEXTURES_LOADED,
		STALE_TEXTURES_LOADED,
		IMPERFECT_TEXTURES_LOADED,
		BEST_TEXTURES_LOADED,
	};
	
	private LoadStatus loadStatus = LoadStatus.NO_TEXTURES_LOADED;
	
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
	private Set<TileIndex> displayableTextures = new HashSet<TileIndex>();

	// private double zoomOffset = 0.5; // tradeoff between optimal resolution (0.0) and speed.
	private TileSet previousTiles;

	private TileConsumer tileConsumer;
	private TextureCache textureCache;
	private SharedVolumeImage volumeImage;

	public Signal1<LoadStatus> loadStatusChanged = new Signal1<LoadStatus>();
	
	public Slot1<TileIndex> onTextureLoadedSlot = new Slot1<TileIndex>() {
		@Override
		public void execute(TileIndex index) {
			if (! displayableTextures.contains(index))
				return;
			// log.info("Needed texture loaded! "+index);
			tileConsumer.getRepaintSlot().execute();
		}
	};
	
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
	
	// June 20, 2013 Generalized for non-Z axes
	public TileSet createLatestTiles(Camera3d camera, Viewport viewport,
			CoordinateAxis sliceAxis, Rotation3d viewerInGround)
	{
		TileSet result = new TileSet();
		if (volumeImage.getLoadAdapter() == null)
			return result;
		if (! tileConsumer.isShowing()) // Hidden viewer shows no tiles.
			return result; 

		if (sliceAxis == CoordinateAxis.X) {
			// System.out.println("X");
		}
		
		// Need to loop over x and y
		// Need to compute z, and zoom
		// 1) zoom
		TileFormat tileFormat = volumeImage.getLoadAdapter().getTileFormat();
		int zoom = tileFormat.zoomLevelForCameraZoom(camera.getPixelsPerSceneUnit());
		int zoomMax = tileFormat.getZoomLevelCount() - 1;

		int xyzFromWhd[] = {0,1,2}; 
		// Rearrange from rotation matrix
		// Which axis (x,y,z) corresponds to width, height, and depth?
		for (int whd = 0; whd < 3; ++whd) {
			Vec3 vWhd = new Vec3(0,0,0);
			vWhd.set(whd, 1.0);
			Vec3 vXyz = viewerInGround.times(vWhd);
			double max = 0.0;
			for (int xyz = 0; xyz < 3; ++xyz) {
				double test = Math.abs(vXyz.get(xyz));
				if (test > max) {
					xyzFromWhd[whd] = xyz;
					max = test;
				}
			}
		}
		
		// 2) z or other slice axisIndex (d: depth)
		Vec3 focus = camera.getFocus();
		double fD = focus.get(xyzFromWhd[2]);
		// Correct for bottom Y origin of Raveler tile coordinate system
		// (everything else is top Y origin: image, our OpenGL, user facing coordinate system)
		BoundingBox3d bb = volumeImage.getBoundingBox3d();
		double bottomY = bb.getMax().getY();
		if (xyzFromWhd[2] == 1) {
			fD = bottomY - fD - 0.5; // bounding box extends 0.5 voxels past final slice
		}
		// Bounding box is actually 0.5 voxels bigger than number of slices at each end
		int dMin = (int)(bb.getMin().get(xyzFromWhd[2])/volumeImage.getResolution(xyzFromWhd[2]) + 0.5);
		int dMax = (int)(bb.getMax().get(xyzFromWhd[2])/volumeImage.getResolution(xyzFromWhd[2]) - 0.5);
		int d = (int)Math.round(fD / volumeImage.getResolution(xyzFromWhd[2]) - 0.5);
		d = Math.max(d, dMin);
		d = Math.min(d, dMax);
		/*
		if (sliceAxis == CoordinateAxis.Y)
			log.info("Y slice "+d);
			*/
		
		// 3) x and y tile index range
		
		// In scene units
		// Clip to screen space
		double wFMin = focus.get(xyzFromWhd[0]) - 0.5*viewport.getWidth()/camera.getPixelsPerSceneUnit();
		double wFMax = focus.get(xyzFromWhd[0]) + 0.5*viewport.getWidth()/camera.getPixelsPerSceneUnit();
		double hFMin = focus.get(xyzFromWhd[1]) - 0.5*viewport.getHeight()/camera.getPixelsPerSceneUnit();
		double hFMax = focus.get(xyzFromWhd[1]) + 0.5*viewport.getHeight()/camera.getPixelsPerSceneUnit();
		// Clip to volume space
		// Subtract one half pixel to avoid loading an extra layer of tiles
		double dw = 0.25 * tileFormat.getVoxelMicrometers()[xyzFromWhd[0]];
		double dh = 0.25 * tileFormat.getVoxelMicrometers()[xyzFromWhd[1]];
		wFMin = Math.max(wFMin, bb.getMin().get(xyzFromWhd[0]) + dw);
		hFMin = Math.max(hFMin, bb.getMin().get(xyzFromWhd[1]) + dh);
		wFMax = Math.min(wFMax, bb.getMax().get(xyzFromWhd[0]) - dw);
		hFMax = Math.min(hFMax, bb.getMax().get(xyzFromWhd[1]) - dh);
		double zoomFactor = Math.pow(2.0, zoom);
		// get tile pixel size 1024 from loadAdapter
		int tileSize[] = tileFormat.getTileSize();
		double tileWidth = tileSize[xyzFromWhd[0]] * zoomFactor * volumeImage.getResolution(xyzFromWhd[0]);
		double tileHeight = tileSize[xyzFromWhd[1]] * zoomFactor * volumeImage.getResolution(xyzFromWhd[1]);

		// Correct for bottom Y origin of Raveler tile coordinate system
		// (everything else is top Y origin: image, our OpenGL, user facing coordinate system)
		if (xyzFromWhd[0] == 1) { // Y axis left-right
			double temp = wFMin;
			wFMin = bottomY - wFMax;
			wFMax = bottomY - temp;
		}
		else if (xyzFromWhd[1] == 1) { // Y axis top-bottom
			double temp = hFMin;
			hFMin = bottomY - hFMax;
			hFMax = bottomY - temp;
		}
		else {
			// TODO - invert slice axis? (already inverted above)
		}

		// In tile units
		int wMin = (int)Math.floor(wFMin / tileWidth);
		int wMax = (int)Math.floor(wFMax / tileWidth);

		int hMin = (int)Math.floor(hFMin / tileHeight);
		int hMax = (int)Math.floor(hFMax / tileHeight);

		TileIndex.IndexStyle indexStyle = tileFormat.getIndexStyle();
		for (int w = wMin; w <= wMax; ++w) {
			for (int h = hMin; h <= hMax; ++h) {
				int whd[] = {w, h, d};
				TileIndex key = new TileIndex(
						whd[xyzFromWhd[0]], 
						whd[xyzFromWhd[1]], 
						whd[xyzFromWhd[2]], 
						zoom, 
						zoomMax, indexStyle, sliceAxis);
				Tile2d tile = new Tile2d(key, tileFormat);
				tile.setYMax(bb.getMax().getY()); // To help flip y; Always actual Y! (right?)
				result.add(tile);
			}
		}
		
		return result;
	}
	
	public TextureCache getTextureCache() {
		return textureCache;
	}

	public LoadStatus getLoadStatus() {
		return loadStatus;
	}

	public void setLoadStatus(LoadStatus loadStatus) {
		if (loadStatus == this.loadStatus)
			return;
		this.loadStatus = loadStatus;
		loadStatusChanged.emit(loadStatus);
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
			if (latestTiles.getLoadStatus() == TileSet.LoadStatus.BEST_TEXTURES_LOADED)
				setLoadStatus(LoadStatus.BEST_TEXTURES_LOADED);
			else
				setLoadStatus(LoadStatus.IMPERFECT_TEXTURES_LOADED);
			// TODO - status
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
			setLoadStatus(LoadStatus.STALE_TEXTURES_LOADED);
		}
		else {
			// log.info("Using LastGood tiles");
			// Fall back to a known displayable
			result = lastGoodTiles;
			if (lastGoodTiles == null)
				setLoadStatus(LoadStatus.NO_TEXTURES_LOADED);
			else if (lastGoodTiles.canDisplay())
				setLoadStatus(LoadStatus.STALE_TEXTURES_LOADED);
			else
				setLoadStatus(LoadStatus.NO_TEXTURES_LOADED);
		}
		
		// Keep working on loading both emergency and latest tiles only.
		Set<TileIndex> newNeededTextures = new LinkedHashSet<TileIndex>();
		newNeededTextures.addAll(emergencyTiles.getFastNeededTextures());
		// Decide whether to load fastest textures or best textures
		Tile2d.LoadStatus stage = latestTiles.getMinStage();
		if (stage.ordinal() < Tile2d.LoadStatus.COARSE_TEXTURE_LOADED.ordinal())
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
		if (! newNeededTextures.equals(neededTextures)) {
			synchronized(neededTextures) {
				neededTextures.clear();
				neededTextures.addAll(newNeededTextures);
			}
		}
		// queueTextureLoad(getNeededTextures());
		
		if ( (! latestTiles.equals(previousTiles)) 
				&& (latestTiles != null)
				&& (latestTiles.size() > 0)
				) {
			previousTiles = latestTiles;
		}
		
		// Remember which textures might be useful
		// Even if it's LOADED, it might not be PAINTED yet.
		displayableTextures.clear();
		for (Tile2d tile : latestTiles) {
			// Best texture so far
			if (tile.getBestTexture() != null)
				displayableTextures.add(tile.getBestTexture().getIndex());
			// Best possible
			displayableTextures.add(tile.getIndex());
		}
		for (Tile2d tile : emergencyTiles) {
			// Best texture so far
			if (tile.getBestTexture() != null)
				displayableTextures.add(tile.getBestTexture().getIndex());
			// Best possible
			displayableTextures.add(tile.getIndex());
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
