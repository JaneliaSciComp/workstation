package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Camera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Viewport;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.VolumeImage3d;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

public class TileServer 
implements VolumeImage3d
{
	// private static final Logger log = LoggerFactory.getLogger(TileServer.class);
	
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
	private Set<PyramidTileIndex> neededTextures;

	// One thread pool to load minimal representation of volume
	private TexturePreFetcher minResPreFetcher = new TexturePreFetcher(4);
	// One thread pool to load current and prefetch textures
	private TexturePreFetcher futurePreFetcher = new TexturePreFetcher(4);

	private BoundingBox3d boundingBox3d = new BoundingBox3d();
	//
	private Camera3d camera;
	private Viewport viewport;
	// signal for tile loaded
	private double zoomOffset = 0.5; // tradeoff between optimal resolution (0.0) and speed.
	private PyramidTextureLoadAdapter loadAdapter;
	private TileSet previousTiles;
	
	private Signal viewTextureChangedSignal = new Signal();
	private Signal1<TileSet> tileSetChangedSignal = new Signal1<TileSet>();
	private Signal volumeInitializedSignal = new Signal();
	
	// Initiate loading of low resolution textures
	private Slot startMinResPreFetchSlot = new Slot() {
		@Override
		public void execute() {
			if (loadAdapter == null)
				return;
			// queue load of all low resolution textures
			minResPreFetcher.clear();
			PyramidTileFormat tileFormat = loadAdapter.getTileFormat();
			int maxZoom = tileFormat.getZoomLevelCount() - 1;
			int x = 0; // only one value of x and y at lowest resolution
			int y = 0;
			int zMin = tileFormat.getOrigin()[2];
			int zMax = zMin + tileFormat.getVolumeSize()[2];
			PyramidTileIndex previousIndex = null;
			for (int z = zMin; z <= zMax; ++z) {
				PyramidTileIndex index = new PyramidTileIndex(x, y, z, maxZoom, maxZoom);
				index = getTextureCache().getCanonicalIndex(index);
				if (! index.equals(previousIndex)) {
					minResPreFetcher.loadDisplayedTexture(index, TileServer.this);
					previousIndex = index;
				}
			}
		}		
	};

	private Slot1<TileSet> updateFuturePreFetchSlot = new Slot1<TileSet>() {
		@Override
		public void execute(TileSet tileSet) {
			// log.info("updatePreFetchSlot");
			futurePreFetcher.clear();
			if (tileSet.size() < 1)
				return;
			
			Set<PyramidTileIndex> queuedTextures = new HashSet<PyramidTileIndex>();

			// First in line are current display tiles
			// TODO - separate these into low res and max res
			// getDisplayTiles(); // update current view
			for (PyramidTileIndex ix : neededTextures) {
				if (queuedTextures.contains(ix))
					continue;
				// log.info("queue load of "+ix);
				futurePreFetcher.loadDisplayedTexture(ix, TileServer.this);
				queuedTextures.add(ix);
			}
			
			// Pre-fetch adjacent Z slices:
			// For initial testing, pre fetch in Z only at first
			int zMinus, zPlus, z0, zoom;
			zMinus = zPlus = z0 = zoom = -1;
			// Choose one tile to initialize search area in Z
			PyramidTileIndex ix0 = new PyramidTileIndex(0,0,0,0,0);
			PyramidTileIndex ix1 = new PyramidTileIndex(0,0,0,0,0);
			for (Tile2d tile : tileSet) {
				ix0 = tile.getIndex();
				ix0 = getTextureCache().getCanonicalIndex(ix0); // uniqueify on Z for octree
				// Zoom out one level for pre-cache
				ix1 = ix0.zoomOut();
				if (ix1 == null)
					ix1 = ix0;
				z0 = zMinus = zPlus = ix1.getZ();
				zoom = ix1.getZoom(); // Zoom out one level for precache
				break; // only need one tile to initialize...
			}
			int zMin = getLoadAdapter().getTileFormat().getOrigin()[2];
			int zMax = zMin + getLoadAdapter().getTileFormat().getVolumeSize()[2] - 1;
			for (int deltaZ = 1; deltaZ <= 50; ++deltaZ) {
				// Step away from center in z, one unique step at a time.
				// Drive to the next unique z value in each direction.
				// minus Z:
				PyramidTileIndex ixMinus = new PyramidTileIndex(
						ix1.getX(), ix1.getY(),
						zMinus - 1,
						zoom, ix1.getMaxZoom());
				while (getTextureCache().getCanonicalIndex(ixMinus).getZ() == zMinus) 
				{
					ixMinus = new PyramidTileIndex(
							ixMinus.getX(), ixMinus.getY(),
							ixMinus.getZ() - 1,
							zoom, ixMinus.getMaxZoom());
				}
				zMinus = getTextureCache().getCanonicalIndex(ixMinus).getZ();
				// plus Z:
				PyramidTileIndex ixPlus = new PyramidTileIndex(
						ix1.getX(), ix1.getY(),
						zPlus + 1,
						zoom, ix1.getMaxZoom());
				while (getTextureCache().getCanonicalIndex(ixPlus).getZ() == zPlus) 
				{
					ixPlus = new PyramidTileIndex(
							ixPlus.getX(), ixPlus.getY(),
							ixPlus.getZ() + 1,
							zoom, ixPlus.getMaxZoom());
				}
				zPlus = getTextureCache().getCanonicalIndex(ixPlus).getZ();
				// log.info("zminus = "+zMinus+"; zplus = "+zPlus);
				//
				for (Tile2d tile : tileSet) {
					PyramidTileIndex ix = tile.getIndex();
					ix = ix.zoomOut();
					if (ix == null)
						ix = tile.getIndex();
					PyramidTileIndex m = new PyramidTileIndex(ix.getX(), ix.getY(), 
							zMinus, 
							zoom, ix.getMaxZoom());
					PyramidTileIndex p = new PyramidTileIndex(ix.getX(), ix.getY(), 
							zPlus, 
							zoom, ix.getMaxZoom());
					if ((zMinus >= zMin) && ! queuedTextures.contains(m))
					{
						futurePreFetcher.loadDisplayedTexture(m, TileServer.this);
						queuedTextures.add(m);
					}
					if ((zPlus <= zMax) && ! queuedTextures.contains(p)) {
						futurePreFetcher.loadDisplayedTexture(p, TileServer.this);
						queuedTextures.add(p);
					}
				}
			}
		}
	};
	
	private Slot1<PyramidTileIndex> onTextureLoadedSlot = new Slot1<PyramidTileIndex>() {
		@Override
		public void execute(PyramidTileIndex ix) {
			// log.info("texture loaded "+ix+"; "+neededTextures.size());
			// 
			// TODO - The "needed" textures SHOULD be the only ones we need
			// to send a repaint signal for. But updating is better for some
			// reason when we emit every time. And the performance does not seem
			// bad, so leaving like this for now.
			if (neededTextures.size() > 0)
				viewTextureChangedSignal.emit(); // too often?
			/*
			if (neededTextures.contains(ix)) {
				log.info("View texture loaded"+ix);
				viewTextureChangedSignal.emit();
			}
			*/
		}
	};
	
	public Slot1<PyramidTileIndex> getOnTextureLoadedSlot() {
		return onTextureLoadedSlot;
	}

	public TileServer(String folderName) {
        try {
			loadURL(new File(folderName).toURI().toURL());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	public void clearCache() {
		TextureCache cache = getTextureCache();
		if (cache == null)
			return;
		cache.clear();
		startMinResPreFetchSlot.execute(); // start loading low-res volume
		viewTextureChangedSignal.emit(); // start loading current view
	}
	
	public TileSet createLatestTiles()
	{
		return createLatestTiles(getCamera(), getViewport());
	}
	
	protected TileSet createLatestTiles(Camera3d camera, Viewport viewport)
	{
		TileSet result = new TileSet();
		if (loadAdapter == null)
			return result;

		// Need to loop over x and y
		// Need to compute z, and zoom
		// 1) zoom
		double maxRes = Math.min(getXResolution(), getYResolution());
		double voxelsPerPixel = 1.0 / (camera.getPixelsPerSceneUnit() * maxRes);
		int zoom = 20; // default to very coarse zoom
		if (voxelsPerPixel > 0.0) {
			double topZoom = Math.log(voxelsPerPixel) / Math.log(2.0);
			zoom = (int)(topZoom + zoomOffset);
		}
		int zoomMin = 0;
		PyramidTileFormat tileFormat = loadAdapter.getTileFormat();
		int zoomMax = tileFormat.getZoomLevelCount() - 1;
		zoom = Math.max(zoom, zoomMin);
		zoom = Math.min(zoom, zoomMax);
		// 2) z
		Vec3 focus = camera.getFocus();
		int z = (int)Math.floor(focus.getZ() / getZResolution() + 0.5);
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
		xFMin = Math.max(xFMin, getBoundingBox3d().getMin().getX() + dx);
		yFMin = Math.max(yFMin, getBoundingBox3d().getMin().getY() + dy);
		xFMax = Math.min(xFMax, getBoundingBox3d().getMax().getX() - dx);
		yFMax = Math.min(yFMax, getBoundingBox3d().getMax().getY() - dy);
		double zoomFactor = Math.pow(2.0, zoom);
		// get tile pixel size 1024 from loadAdapter
		int tileSize[] = tileFormat.getTileSize();
		double tileWidth = tileSize[0] * zoomFactor * getXResolution();
		double tileHeight = tileSize[1] * zoomFactor * getYResolution();
		// In tile units
		int xMin = (int)Math.floor(xFMin / tileWidth);
		int xMax = (int)Math.floor(xFMax / tileWidth);
		
		// Correct for bottom Y origin of Raveler tile coordinate system
		// (everything else is top Y origin: image, our OpenGL, user facing coordinate system)
		double bottomY = getBoundingBox3d().getMax().getY();
		int yMin = (int)Math.floor((bottomY - yFMax) / tileHeight);
		int yMax = (int)Math.floor((bottomY - yFMin) / tileHeight);
		
		for (int x = xMin; x <= xMax; ++x) {
			for (int y = yMin; y <= yMax; ++y) {
				PyramidTileIndex key = new PyramidTileIndex(x, y, z, zoom, zoomMax);
				Tile2d tile = new Tile2d(key, tileFormat);
				tile.setYMax(getBoundingBox3d().getMax().getY()); // To help flip y
				result.add(tile);
			}
		}
		
		return result;
	}
	
	public synchronized Set<PyramidTileIndex> getNeededTextures() {
		return neededTextures;
	}

	public Signal1<TileSet> getTileSetChangedSignal() {
		return tileSetChangedSignal;
	}

	public Signal getViewTextureChangedSignal() {
		return viewTextureChangedSignal;
	}

	@Override
	public BoundingBox3d getBoundingBox3d() {
		return boundingBox3d;
	}

	public Camera3d getCamera() {
		return camera;
	}

	@Override
	public int getMaximumIntensity() {
		if (loadAdapter == null)
			return 0;
		return loadAdapter.getTileFormat().getIntensityMax();
	}

	@Override
	public int getNumberOfChannels() {
		if (loadAdapter == null)
			return 0;
		return loadAdapter.getTileFormat().getChannelCount();
	}

	public Slot1<TileSet> getUpdateFuturePreFetchSlot() {
		return updateFuturePreFetchSlot;
	}

	// Produce a list of renderable tiles to complete this view
	public TileSet getDisplayTiles() 
	{
		// Update latest tile set
		latestTiles = createLatestTiles();
		latestTiles.assignTextures(getTextureCache());
		
		// Need to assign textures to emergency tiles too...
		if (emergencyTiles != null)
			emergencyTiles.assignTextures(getTextureCache());
		
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
		Set<PyramidTileIndex> newNeededTextures = new LinkedHashSet<PyramidTileIndex>();
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
		setNeededTextures(newNeededTextures);
		// queueTextureLoad(getNeededTextures());
		
		// put tile set changed signal here
		if (! latestTiles.equals(previousTiles)) {
			previousTiles = latestTiles;
			tileSetChangedSignal.emit(result);
		}
		
		return result;
	}	

	public TextureCache getTextureCache() {
		if (loadAdapter == null)
			return null;
		return loadAdapter.getTextureCache();
	}
	
	public Viewport getViewport() {
		return viewport;
	}

	public Signal getVolumeInitializedSignal() {
		return volumeInitializedSignal;
	}

	@Override
	public double getXResolution() {
		if (loadAdapter == null)
			return 0;
		return loadAdapter.getTileFormat().getVoxelMicrometers()[0];
	}

	@Override
	public double getYResolution() {
		if (loadAdapter == null)
			return 0;
		return loadAdapter.getTileFormat().getVoxelMicrometers()[1];
	}

	@Override
	public double getZResolution() {
		if (loadAdapter == null)
			return 0;
		return loadAdapter.getTileFormat().getVoxelMicrometers()[2];
	}	

	@Override
	public boolean loadURL(URL folderUrl) {
		// Sanity check before overwriting current view
		if (folderUrl == null)
			return false;
		// Sniff which back end we need
		boolean useRaveler = false;
		try {
			// Look for diagnostic block tiff file
			URL testUrl = new URL(folderUrl, "default.0.tif");
			testUrl.openStream();
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			useRaveler = true;
		}
		// Now we can start replacing the previous state
		PyramidTextureLoadAdapter testLoadAdapter = null;
		try {
			if (useRaveler) {
				testLoadAdapter = new RavelerLoadAdapter(folderUrl);
			}
			else {
				File fileFolder = new File(folderUrl.toURI());
				BlockTiffOctreeLoadAdapter btola = new BlockTiffOctreeLoadAdapter();
				btola.setTopFolder(fileFolder);
				getTileSetChangedSignal().connect(getUpdateFuturePreFetchSlot());
				testLoadAdapter = btola;
			}
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			// e.printStackTrace();
			return false;
		}
		if (testLoadAdapter != null) {
			loadAdapter = testLoadAdapter;
			// Initialize pre-fetchers
			minResPreFetcher.setTextureCache(getTextureCache());
			minResPreFetcher.setLoadAdapter(loadAdapter);
			futurePreFetcher.setTextureCache(getTextureCache());
			futurePreFetcher.setLoadAdapter(loadAdapter);
			// Don't pre-fetch before cache is cleared...
			getTextureCache().getCacheClearedSignal().connect(startMinResPreFetchSlot);
			// Compute bounding box
			PyramidTileFormat tf = loadAdapter.getTileFormat();
			double sv[] = tf.getVoxelMicrometers();
			int s0[] = tf.getOrigin();
			int s1[] = tf.getVolumeSize();
			Vec3 b0 = new Vec3(sv[0]*s0[0], sv[1]*s0[1], sv[2]*s0[2]);
			Vec3 b1 = new Vec3(sv[0]*(s0[0]+s1[0]), sv[1]*(s0[1]+s1[1]), sv[2]*(s0[2]+s1[2]));
			boundingBox3d.setMin(b0);
			boundingBox3d.setMax(b1);
			// remove old data
			emergencyTiles = null;
			if (latestTiles != null)
				latestTiles.clear();
			if (lastGoodTiles != null)
				lastGoodTiles.clear();
			// queue disposal of textures on next display event
			getVolumeInitializedSignal().emit();
			return true;
		}
		else
			return false;
	}

	public void setCamera(Camera3d camera) {
		this.camera = camera;
	}

	public void setViewport(Viewport viewport) {
		this.viewport = viewport;
	}

	public PyramidTextureLoadAdapter getLoadAdapter() {
		return loadAdapter;
	}

	public synchronized void setNeededTextures(Set<PyramidTileIndex> neededTextures) {
		Set<PyramidTileIndex> result = new LinkedHashSet<PyramidTileIndex>();
		for (PyramidTileIndex ix : neededTextures) {
			ix = getTextureCache().getCanonicalIndex(ix);
			result.add(ix);
			// log.info("Need texture "+ix);
		}
		this.neededTextures = result;
	}

	public ImageBrightnessStats getCurrentBrightnessStats() {
		ImageBrightnessStats result = null;
		for (Tile2d tile : latestTiles) {
			ImageBrightnessStats bs = tile.getBrightnessStats();
			if (result == null)
				result = bs;
			else if (bs != null)
				result.combine(tile.getBrightnessStats());
		}
		return result;
	}

}
