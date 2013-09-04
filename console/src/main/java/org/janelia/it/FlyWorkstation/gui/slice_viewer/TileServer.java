package org.janelia.it.FlyWorkstation.gui.slice_viewer;

import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.janelia.it.FlyWorkstation.gui.viewer3d.CoordinateAxis;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.generator.InterleavedIterator;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.generator.MinResSliceGenerator;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.generator.UmbrellaSliceGenerator;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.generator.SliceGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TileServer 
implements ComponentListener // so changes in viewer size/visibility can be tracked
// implements VolumeImage3d
{
	private static final Logger log = LoggerFactory.getLogger(TileServer.class);

	// Derived from individual ViewTileManagers
	public static enum LoadStatus {
		UNINITIALIZED,
		NO_TEXTURES_LOADED,
		IMPERFECT_TEXTURES_LOADED,
		BEST_TEXTURES_LOADED,
		PREFETCH_COMPLETE, // Best textures shown, plus precache is full
	};
	
	private boolean doPrefetch = true;
	private LoadStatus loadStatus = LoadStatus.UNINITIALIZED;
	
	// One thread pool to load minimal representation of volume
	private TexturePreFetcher minResPreFetcher = new TexturePreFetcher(10);
	// One thread pool to load current and prefetch textures
	private TexturePreFetcher futurePreFetcher = new TexturePreFetcher(10);

	// Refactoring 6/12/2013
	private SharedVolumeImage sharedVolumeImage;
	private TextureCache textureCache = new TextureCache();
	
	// One for each orthogonal viewer
	// private Set<TileConsumer> tileConsumers = new HashSet<TileConsumer>();
	private Set<ViewTileManager> viewTileManagers = new HashSet<ViewTileManager>();
	
	// New path for handling tile updates July 9, 2013 cmb
	private Set<TileIndex> currentDisplayTiles = new HashSet<TileIndex>();
	public Slot refreshCurrentTileSetSlot = new Slot() {
		@Override
		public void execute() {refreshCurrentTileSet();}
	};
	
	// Initiate loading of low resolution textures
	private Slot startMinResPreFetchSlot = new Slot() {
		@Override
		public void execute() {
			// log.info("starting pre fetch of lowest resolution tiles");
			// Load X and Y slices too (in addition to Z), if available
			if (sharedVolumeImage.getLoadAdapter() == null)
				return;
			// queue load of all low resolution textures
			minResPreFetcher.clear();
			TileFormat format = sharedVolumeImage.getLoadAdapter().getTileFormat();
			List<MinResSliceGenerator> generators = new Vector<MinResSliceGenerator>();
			if (format.isHasXSlices())
				generators.add(new MinResSliceGenerator(format, CoordinateAxis.X));
			if (format.isHasYSlices())
				generators.add(new MinResSliceGenerator(format, CoordinateAxis.Y));
			if (format.isHasZSlices())
				generators.add(new MinResSliceGenerator(format, CoordinateAxis.Z));

			Iterable<TileIndex> tileGenerator;
			if (generators.size() < 1)
				return;
			else if (generators.size() == 1)
				tileGenerator = generators.get(0);
			else {
				Iterator<MinResSliceGenerator> i = generators.iterator();
				tileGenerator = new InterleavedIterator<TileIndex>(i.next(), i.next());
				while (i.hasNext()) {
					tileGenerator = new InterleavedIterator<TileIndex>(tileGenerator, i.next());
				}
			}
			int tileCount = 0;
			for (TileIndex i : tileGenerator) {
				minResPreFetcher.loadDisplayedTexture(i, TileServer.this);
				tileCount += 1;
			}
			// log.info(tileCount+" min resolution tiles queued");
		}
	};

	public Signal1<TileIndex> textureLoadedSignal = new Signal1<TileIndex>();
	public Signal1<LoadStatus> loadStatusChangedSignal = new Signal1<LoadStatus>();

	public Slot onVolumeInitializedSlot = new Slot() {
		@Override
		public void execute() {
			if (sharedVolumeImage == null)
				return;
			// Initialize pre-fetchers
			minResPreFetcher.setLoadAdapter(sharedVolumeImage.getLoadAdapter());
			futurePreFetcher.setLoadAdapter(sharedVolumeImage.getLoadAdapter());
			// remove old data
			for (ViewTileManager vtm : viewTileManagers)
				vtm.clear();
			// queue disposal of textures on next display event
			setCacheSizesAsFractionOfMaxHeap(0.15, 0.35);
			clearCache();
			refreshCurrentTileSet();
		}
	};
	
	private Slot updateLoadStatusSlot = new Slot() {
		@Override
		public void execute() {
			updateLoadStatus();
		}
	};
	
	public TileServer(SharedVolumeImage sharedVolumeImage) {
		setSharedVolumeImage(sharedVolumeImage);
		minResPreFetcher.setTextureCache(getTextureCache());
		futurePreFetcher.setTextureCache(getTextureCache());
		textureCache.textureLoadedSignal.connect(textureLoadedSignal);
		getTextureCache().queueDrainedSignal.connect(updateLoadStatusSlot);
	}

	public void addViewTileManager(ViewTileManager viewTileManager) {
		if (viewTileManagers.contains(viewTileManager))
			return; // already there
		viewTileManagers.add(viewTileManager);
		textureLoadedSignal.connect(viewTileManager.onTextureLoadedSlot);
		viewTileManager.loadStatusChanged.connect(updateLoadStatusSlot);
		// viewTileManager.tileSetChangedSignal.connect(updateFuturePreFetchSlot);
		viewTileManager.setTextureCache(getTextureCache());
	}
	
	public void clearCache() 
	{
		TextureCache cache = getTextureCache();
		if (cache == null)
			return;
		cache.clear();
		startMinResPreFetchSlot.execute(); // start loading low-res volume
	};
	
	public TileSet createLatestTiles() {
		TileSet result = new TileSet();
		for (ViewTileManager vtm : viewTileManagers) {
			if (vtm.getTileConsumer().isShowing())
				result.addAll(vtm.createLatestTiles());
		}
		return result;
	}
	
	public Set<ViewTileManager> getViewTileManagers() {
		return viewTileManagers;
	}

	public LoadStatus getLoadStatus() {
		return loadStatus;
	}

	public void setLoadStatus(LoadStatus loadStatus) {
		if (this.loadStatus == loadStatus)
			return; // no change
		// log.info("Load status changed to "+loadStatus);
		this.loadStatus = loadStatus;
		loadStatusChangedSignal.emit(loadStatus);
	}

	public SharedVolumeImage getSharedVolumeImage() {
		return sharedVolumeImage;
	}

	public void setSharedVolumeImage(SharedVolumeImage sharedVolumeImage) {
		if (this.sharedVolumeImage == sharedVolumeImage)
			return;
		this.sharedVolumeImage = sharedVolumeImage;
		sharedVolumeImage.volumeInitializedSignal.connect(onVolumeInitializedSlot);
	}

	public TextureCache getTextureCache() {
		return textureCache;
	}
	
	public Signal1<URL> getVolumeInitializedSignal() {
		return sharedVolumeImage.volumeInitializedSignal;
	}

	private void updateLoadStatus() {
		LoadStatus result;
		if (sharedVolumeImage == null) {
			setLoadStatus(LoadStatus.UNINITIALIZED);
			return;
		}
		// Prepare to analyze each ViewTileManager's loadStatus
		int totalVtmCount = 0;
		int bestVtmCount = 0;
		int imperfectVtmCount = 0;
		int emptyVtmCount = 0;
		for (ViewTileManager vtm : viewTileManagers) {
			if (! vtm.getTileConsumer().isShowing())
				continue;
			totalVtmCount += 1;
			if (vtm.getLoadStatus() == ViewTileManager.LoadStatus.BEST_TEXTURES_LOADED)
				bestVtmCount += 1;
			else if (vtm.getLoadStatus() == ViewTileManager.LoadStatus.IMPERFECT_TEXTURES_LOADED)
				imperfectVtmCount += 1;
			else
				emptyVtmCount += 1;
		}
		LoadStatus activeLoadStatus;
		if (totalVtmCount == bestVtmCount)
			activeLoadStatus = LoadStatus.BEST_TEXTURES_LOADED;
		else if (emptyVtmCount == totalVtmCount)
			activeLoadStatus = LoadStatus.NO_TEXTURES_LOADED;
		else
			activeLoadStatus = LoadStatus.IMPERFECT_TEXTURES_LOADED;
		if (activeLoadStatus.ordinal() < LoadStatus.BEST_TEXTURES_LOADED.ordinal())
			// precache does not matter if there are missing display textures
			setLoadStatus(activeLoadStatus);
		// Is prefetch cache full?
		else if (textureCache.hasQueuedTextures())
			setLoadStatus(activeLoadStatus);
		else
			setLoadStatus(LoadStatus.PREFETCH_COMPLETE);
	}
	
	private void rearrangeLoadQueue(TileSet currentTiles) {
		for (ViewTileManager vtm : viewTileManagers) {
			vtm.updateDisplayTiles();
		}
		updateLoadStatus();
		
		// log.info("updatePreFetchSlot");
		futurePreFetcher.clear();
		
		Set<TileIndex> cacheableTextures = new HashSet<TileIndex>();
		int maxCacheable = (int)(0.90 * getTextureCache().getFutureCache().getMaxSize());

		// First in line are current display tiles
		// Prepare to analyze each ViewTileManager's loadStatus
		for (ViewTileManager vtm : viewTileManagers) {
			if (! vtm.getTileConsumer().isShowing())
				continue;
			for (TileIndex ix : vtm.getNeededTextures()) {
				if (cacheableTextures.contains(ix))
					continue; // already noted
				// log.info("queue load of "+ix);
				if (futurePreFetcher.loadDisplayedTexture(ix, TileServer.this))
					cacheableTextures.add(ix);
			}
		}
		
		if (doPrefetch) {
			/* TODO - LOD tiles are not working yet...
			// Get level-of-detail tiles
			Iterable<TileIndex> lodGen = new LodGenerator(TileServer.this);
			for (TileIndex ix : lodGen) {
				if (cacheableTextures.contains(ix))
					continue;
				if (cacheableTextures.size() >= maxCacheable)
					break;
				if (futurePreFetcher.loadDisplayedTexture(ix, TileServer.this))
					cacheableTextures.add(ix);
			}
			*/
			
			// Sort tiles into X, Y, and Z slices to help with generators
			Map<CoordinateAxis, TileSet> axisTiles = new HashMap<CoordinateAxis, TileSet>();
			for (Tile2d tile : currentTiles) {
				TileIndex i = tile.getIndex();
				CoordinateAxis axis = i.getSliceAxis();
				if (! axisTiles.containsKey(axis))
					axisTiles.put(axis, new TileSet());
				axisTiles.get(axis).add(tile);
			}
			// Create one umbrella generator for each (used) direction.
			List<Iterable<TileIndex>> umbrellas = new Vector<Iterable<TileIndex>>();
			List<Iterable<TileIndex>> fullSlices = new Vector<Iterable<TileIndex>>();
			for (CoordinateAxis axis : axisTiles.keySet()) {
				TileSet tiles = axisTiles.get(axis);
				// Umbrella Z scan
				Iterable<TileIndex> sliceGen = new UmbrellaSliceGenerator(getLoadAdapter().getTileFormat(), tiles);
				umbrellas.add(sliceGen);
				// Full resolution Z scan
				sliceGen = new SliceGenerator(getLoadAdapter().getTileFormat(), tiles);
				fullSlices.add(sliceGen);
			}
			// Interleave the various umbrella generators
			if (umbrellas.size() > 0) {
				Iterable<TileIndex> combinedUmbrella;
				Iterable<TileIndex> combinedFullSlice;
				if (umbrellas.size() == 1) {
					combinedUmbrella = umbrellas.get(0);
					combinedFullSlice = fullSlices.get(0);
				}
				else { // more than one axis
					Iterator<Iterable<TileIndex>> sliceIter = umbrellas.iterator();
					combinedUmbrella = new InterleavedIterator<TileIndex>(sliceIter.next(), sliceIter.next());
					while (sliceIter.hasNext())
						combinedUmbrella = new InterleavedIterator<TileIndex>(combinedUmbrella, sliceIter.next());
					//
					sliceIter = fullSlices.iterator();
					combinedFullSlice = new InterleavedIterator<TileIndex>(sliceIter.next(), sliceIter.next());
					while (sliceIter.hasNext())
						combinedFullSlice = new InterleavedIterator<TileIndex>(combinedFullSlice, sliceIter.next());
				}
				
				// Load umbrella slices
				for (TileIndex ix : combinedUmbrella) {
					if (cacheableTextures.contains(ix))
						continue;
					if (cacheableTextures.size() >= maxCacheable)
						break;
					if (futurePreFetcher.loadDisplayedTexture(ix, TileServer.this))
						cacheableTextures.add(ix);						
				}

				// Load full resolution slices
				for (TileIndex ix : combinedFullSlice) {
					if (cacheableTextures.contains(ix))
						continue;
					if (cacheableTextures.size() >= maxCacheable)
						break;
					if (futurePreFetcher.loadDisplayedTexture(ix, TileServer.this))
						cacheableTextures.add(ix);						
				}
			}
			
			/*
			Iterable<TileIndex> zGen = new UmbrellaSliceGenerator(getLoadAdapter().getTileFormat(), currentTiles);
			// Get nearby Z-tiles, with decreasing LOD
			for (TileIndex ix : zGen) {
				// TODO - restrict to Z tiles for testing
				if (ix.getSliceAxis() != CoordinateAxis.Z) // TODO remove test
					continue;
				if (cacheableTextures.contains(ix))
					continue;
				if (cacheableTextures.size() >= maxCacheable)
					break;
				if (futurePreFetcher.loadDisplayedTexture(ix, TileServer.this))
					cacheableTextures.add(ix);
			}
			*/
			
			/*
			// Get more Z-tiles, at current LOD
			zGen = new ZGenerator(getLoadAdapter().getTileFormat(), currentTiles);
			for (TileIndex ix : zGen) {
				if (cacheableTextures.contains(ix))
					continue;
				if (cacheableTextures.size() >= maxCacheable)
					break;
				if (futurePreFetcher.loadDisplayedTexture(ix, TileServer.this))
					cacheableTextures.add(ix);
			}
			*/

		}			

		// log.info("Number of queued textures = "+cacheableTextures.size());
		
		updateLoadStatus();
	}
	
	// Part of new way July 9, 2013
	private void refreshCurrentTileSet() {
		TileSet tiles = createLatestTiles();
		Set<TileIndex> indices = new HashSet<TileIndex>();
		for (Tile2d t : tiles)
			indices.add(t.getIndex());
		if (indices.equals(currentDisplayTiles))
			return; // no change
		// log.info("Tile Set Changed!");
		currentDisplayTiles = indices;
		rearrangeLoadQueue(tiles);
	}
	
	// TODO - could move this to TextureCache class?
	public void setCacheSizesAsFractionOfMaxHeap(double historyFraction, double futureFraction) {
		if ((historyFraction + futureFraction) >= 1.0)
			log.warn("Combined cache sizes are larger than max heap size.");
		Runtime rt = Runtime.getRuntime();
		long maxHeapBytes = rt.maxMemory();
		TileFormat format = sharedVolumeImage.getLoadAdapter().getTileFormat();
		long tileBytes = format.getTileBytes();
		int historyTileMax = (int)(historyFraction * maxHeapBytes / tileBytes);
		int futureTileMax = (int)(futureFraction * maxHeapBytes / tileBytes);
		getTextureCache().getHistoryCache().setMaxEntries(historyTileMax);
		getTextureCache().getFutureCache().setMaxEntries(futureTileMax);
		// log.info("History cache size = "+historyTileMax);
		// log.info("Future cache size = "+futureTileMax);
	}
	
	public AbstractTextureLoadAdapter getLoadAdapter() {
		return sharedVolumeImage.getLoadAdapter();
	}

	public ImageBrightnessStats getCurrentBrightnessStats() {
		ImageBrightnessStats result = null;
		for (ViewTileManager vtm : viewTileManagers) {
			if (vtm == null)
				continue;
			TileSet tiles = vtm.getLatestTiles();
			if (tiles == null)
				continue;
			for (Tile2d tile : vtm.getLatestTiles()) {
				ImageBrightnessStats bs = tile.getBrightnessStats();
				if (result == null)
					result = bs;
				else if (bs != null)
					result.combine(tile.getBrightnessStats());
			}
		}
		return result;
	}

	// ComponentListener interface, to viewer changes can be tracked
	@Override
	public void componentResized(ComponentEvent e) {
		refreshCurrentTileSet();
	}

	@Override
	public void componentMoved(ComponentEvent e) {
		// refreshCurrentTileSet();
	}

	@Override
	public void componentShown(ComponentEvent e) {
		refreshCurrentTileSet();
	}

	@Override
	public void componentHidden(ComponentEvent e) {
		refreshCurrentTileSet();
	}

}
