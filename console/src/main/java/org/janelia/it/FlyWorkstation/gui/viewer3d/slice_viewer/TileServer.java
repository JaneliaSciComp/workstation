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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TileServer 
implements VolumeImage3d
{
	private static final Logger log = LoggerFactory.getLogger(TileServer.class);
	
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
	private Set<TileIndex> neededTextures;

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
	private AbstractTextureLoadAdapter loadAdapter;
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
			TileFormat tileFormat = loadAdapter.getTileFormat();
			int maxZoom = tileFormat.getZoomLevelCount() - 1;
			int x = 0; // only one value of x and y at lowest resolution
			int y = 0;
			int zMin = tileFormat.getOrigin()[2];
			int zMax = zMin + tileFormat.getVolumeSize()[2];

			// Start at center and move out
			int z0 = (zMin + zMax)/2;
			TileIndex index1 = new TileIndex(x, y, z0, maxZoom, 
					maxZoom, tileFormat.getIndexStyle());
			TileIndex index2 = index1.nextZ();
			while ( (index1.getZ() >= zMin) || (index2.getZ() <= zMax) ) {
				if (index1.getZ() >= zMin)
					minResPreFetcher.loadDisplayedTexture(index1, TileServer.this);
				if (index2.getZ() <= zMax)
					minResPreFetcher.loadDisplayedTexture(index2, TileServer.this);					
				index1 = index1.previousZ();
				index2 = index2.nextZ();
			}
		}		
	};

	private Slot1<TileSet> updateFuturePreFetchSlot = new Slot1<TileSet>() {
		@Override
		public void execute(TileSet tileSet) {
			long startTime = System.nanoTime();
			
			// log.info("updatePreFetchSlot");
			futurePreFetcher.clear();
			if (tileSet.size() < 1)
				return;
			
			Set<TileIndex> queuedTextures = new HashSet<TileIndex>();

			// First in line are current display tiles
			// TODO - separate these into low res and max res
			// getDisplayTiles(); // update current view
			for (TileIndex ix : neededTextures) {
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
			TileFormat tileFormat = getLoadAdapter().getTileFormat();
			TileIndex.IndexStyle indexStyle = tileFormat.getIndexStyle();
			// Choose one tile to initialize search area in Z
			TileIndex ix0 = tileSet.iterator().next().getIndex();
			// Zoom out one level for pre-cache
			TileIndex ix1 = ix0; // ix0.zoomOut();
			if (ix1 == null)
				ix1 = ix0;
			z0 = zMinus = zPlus = ix1.getZ();
			zoom = ix1.getZoom(); // Zoom out one level for precache
			int zMin = tileFormat.getOrigin()[2];
			int zMax = zMin + tileFormat.getVolumeSize()[2] - 1;
			int zStepCount = 0;
			while (((zMinus >= zMin) || (zPlus <= zMax)) // something is within Z-bounds
					&& (queuedTextures.size() < (getTextureCache().getFutureCache().getMaxSize() - 100))) // future cache is not full
			{
				// Zoom out one level at +- 3, +- 20
				if ((zStepCount == 3) || (zStepCount == 20)) {
					TileIndex ixNew = ix1.zoomOut();
					if (ixNew != null)
						ix1 = ixNew;
					zoom = ix1.getZoom();
				}
				zStepCount += 1;
				// Step away from center in z, one unique step at a time.
				// Drive to the next unique z value in each direction.
				// minus Z:
				TileIndex ixMinus = new TileIndex(
						ix1.getX(), ix1.getY(),
						zMinus,
						zoom, ix1.getMaxZoom(), indexStyle);
				ixMinus = ixMinus.previousZ();
				zMinus = ixMinus.getCanonicalZ();
				// plus Z:
				TileIndex ixPlus = new TileIndex(
						ix1.getX(), ix1.getY(),
						zPlus,
						zoom, ix1.getMaxZoom(), indexStyle);
				ixPlus = ixPlus.nextZ();
				zPlus = ixPlus.getCanonicalZ();
				// log.info("zminus = "+zMinus+"; zplus = "+zPlus);
				//
				for (Tile2d tile : tileSet) {
					TileIndex ix = tile.getIndex();
					while (ix.getZoom() < zoom)
						ix = ix.zoomOut();
					if (ix == null)
						ix = tile.getIndex();
					TileIndex m = new TileIndex(ix.getX(), ix.getY(), 
							zMinus, 
							zoom, ix.getMaxZoom(), indexStyle);
					TileIndex p = new TileIndex(ix.getX(), ix.getY(), 
							zPlus, 
							zoom, ix.getMaxZoom(), indexStyle);
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
			long endTime = System.nanoTime();
			// log.info("Prefetch fill elapsed time = "+(endTime-startTime)/1e6+" ms");
		}
	};
	
	private Slot1<TileIndex> onTextureLoadedSlot = new Slot1<TileIndex>() {
		@Override
		public void execute(TileIndex ix) {
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
	
	public Slot1<TileIndex> getOnTextureLoadedSlot() {
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
		TileFormat tileFormat = loadAdapter.getTileFormat();
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
		
		TileIndex.IndexStyle indexStyle = tileFormat.getIndexStyle();
		for (int x = xMin; x <= xMax; ++x) {
			for (int y = yMin; y <= yMax; ++y) {
				TileIndex key = new TileIndex(x, y, z, zoom, 
						zoomMax, indexStyle);
				Tile2d tile = new Tile2d(key, tileFormat);
				tile.setYMax(getBoundingBox3d().getMax().getY()); // To help flip y
				result.add(tile);
			}
		}
		
		return result;
	}
	
	public synchronized Set<TileIndex> getNeededTextures() {
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
		
		// Push latest textures to front of LRU cache
		for (Tile2d tile : latestTiles) {
			TileTexture texture = tile.getBestTexture();
			if (texture == null)
				continue;
			getTextureCache().markHistorical(texture);
		}
		
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
		AbstractTextureLoadAdapter testLoadAdapter = null;
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
			TileFormat tf = loadAdapter.getTileFormat();
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

	public AbstractTextureLoadAdapter getLoadAdapter() {
		return loadAdapter;
	}

	public synchronized void setNeededTextures(Set<TileIndex> neededTextures) {
		Set<TileIndex> result = new LinkedHashSet<TileIndex>();
		for (TileIndex ix : neededTextures) {
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
