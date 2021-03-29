package org.janelia.workstation.controller.tileimagery;

import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.janelia.workstation.geom.CoordinateAxis;
import org.janelia.workstation.controller.listener.LoadStatusListener;
import org.janelia.workstation.controller.listener.StatusUpdateListener;
import org.janelia.workstation.controller.listener.VolumeLoadListener;
import org.janelia.workstation.controller.tileimagery.generator.InterleavedIterator;
import org.janelia.workstation.controller.tileimagery.generator.MinResSliceGenerator;
import org.janelia.workstation.controller.tileimagery.generator.SliceGenerator;
import org.janelia.workstation.controller.tileimagery.generator.UmbrellaSliceGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TileServer implements ComponentListener, // so changes in viewer size/visibility can be tracked
        VolumeLoadListener {

    private static final Logger LOG = LoggerFactory.getLogger(TileServer.class);
    private static final int MIN_RES_TILE_LOADER_CONCURRENCY = 10;
    private static final int HIGHER_RES_TILE_LOADER_CONCURRENCY = 15;

    // Derived from individual ViewTileManagers
    public enum LoadStatus {
        UNINITIALIZED,
        NO_TEXTURES_LOADED,
        IMPERFECT_TEXTURES_LOADED,
        BEST_TEXTURES_LOADED,
        PREFETCH_COMPLETE, // Best textures shown, plus precache is full
    };

    private boolean doPrefetch = true;
    private LoadStatus loadStatus = LoadStatus.UNINITIALIZED;

    // One thread pool to load minimal representation of volume
    private final TexturePreFetcher minResPreFetcher; //!!!! = new TexturePreFetcher(MIN_RES_TILE_LOADER_CONCURRENCY, MIN_RES_TILE_LOADER_CONCURRENCY);
    // One thread pool to load current and prefetch textures
    private final TexturePreFetcher futurePreFetcher; //!!!! = new TexturePreFetcher(MIN_RES_TILE_LOADER_CONCURRENCY, HIGHER_RES_TILE_LOADER_CONCURRENCY);

    // Refactoring 6/12/2013
    private SharedVolumeImage sharedVolumeImage;
    private TextureCache textureCache = new TextureCache();

    private LoadStatusListener loadStatusListener;
    private StatusUpdateListener queueDrainedListener;

    // One for each orthogonal viewer
    // private Set<TileConsumer> tileConsumers = new HashSet<TileConsumer>();
    private Set<ViewTileManager> viewTileManagers = new HashSet<>();

    // New path for handling tile updates July 9, 2013 cmb
    private Set<TileIndex> currentDisplayTiles = new HashSet<>();

    public TileServer(SharedVolumeImage sharedVolumeImage) {
        this.minResPreFetcher = new TexturePreFetcher(MIN_RES_TILE_LOADER_CONCURRENCY, MIN_RES_TILE_LOADER_CONCURRENCY);
        this.futurePreFetcher = new TexturePreFetcher(MIN_RES_TILE_LOADER_CONCURRENCY, HIGHER_RES_TILE_LOADER_CONCURRENCY);

        setSharedVolumeImage(sharedVolumeImage.setTileLoaderProvider(new BlockTiffOctreeTileLoaderProvider() {
            int concurrency = HIGHER_RES_TILE_LOADER_CONCURRENCY;

            @Override
            public BlockTiffOctreeLoadAdapter createLoadAdapter(String baseURI) {
                return TileStackCacheController.createInstance(
                        new TileStackOctreeLoadAdapter(new TileFormat(), URI.create(baseURI), concurrency));
            }
        }));

        minResPreFetcher.setTextureCache(getTextureCache());
        futurePreFetcher.setTextureCache(getTextureCache());
        queueDrainedListener = new StatusUpdateListener() {
            @Override
            public void update() {
                updateLoadStatus();
            }
        };
        getTextureCache().setQueueDrainedListener(queueDrainedListener);
    }

    public void textureLoaded(TileIndex tileIndex) {
        for (ViewTileManager vtm : viewTileManagers) {
            vtm.textureLoaded(tileIndex);
        }
    }

    private void startMinResPreFetch() {
        // log.info("starting pre fetch of lowest resolution tiles");
        // Load X and Y slices too (in addition to Z), if available
        if (!sharedVolumeImage.isLoaded()) {
            return;
        }
        // queue load of all low resolution textures
        minResPreFetcher.clear();
        TileFormat format = sharedVolumeImage.getLoadAdapter().getTileFormat();
        List<MinResSliceGenerator> generators = new ArrayList<>();
        if (format.isHasXSlices()) {
            generators.add(new MinResSliceGenerator(format, CoordinateAxis.X));
        }
        if (format.isHasYSlices()) {
            generators.add(new MinResSliceGenerator(format, CoordinateAxis.Y));
        }
        if (format.isHasZSlices()) {
            generators.add(new MinResSliceGenerator(format, CoordinateAxis.Z));
        }
        Iterable<TileIndex> tileGenerator;
        if (generators.size() < 1) {
            return;
        } else if (generators.size() == 1) {
            tileGenerator = generators.get(0);
        } else {
            Iterator<MinResSliceGenerator> i = generators.iterator();
            tileGenerator = new InterleavedIterator<>(i.next(), i.next());
            while (i.hasNext()) {
                tileGenerator = new InterleavedIterator<>(tileGenerator, i.next());
            }
        }
        for (TileIndex i : tileGenerator) {
            minResPreFetcher.loadDisplayedTexture(i, TileServer.this);
        }
    }

    /**
     * @param loadStatusListener the loadStatusListener to set
     */
    public void setLoadStatusListener(LoadStatusListener loadStatusListener) {
        this.loadStatusListener = loadStatusListener;
    }

    public void addViewTileManager(ViewTileManager viewTileManager) {
        if (viewTileManagers.contains(viewTileManager)) {
            return; // already there
        }
        viewTileManagers.add(viewTileManager);
        viewTileManager.setLoadStatusChangedListener(queueDrainedListener);
        viewTileManager.setTextureCache(getTextureCache());
    }

    public void clearCache() {
        // Replace entire texture cache, to avoid retained textures
        int[] textureIds = null;
        if (textureCache != null) {
            textureCache.clear();
            textureIds = textureCache.popObsoleteTextureIds();
            textureCache.setQueueDrainedListener(null);
        }
        textureCache = new TextureCache();
        textureCache.setQueueDrainedListener(queueDrainedListener);
        if (textureIds != null) {
            textureCache.getHistoryCache().storeObsoleteTextureIds(textureIds); // so old texture ids can get deleted next draw
        }
        minResPreFetcher.setTextureCache(textureCache);
        futurePreFetcher.setTextureCache(textureCache);
        for (ViewTileManager vtm : viewTileManagers) {
            vtm.clear();
            vtm.setTextureCache(textureCache);
        }
        if (!VolumeCache.useVolumeCache()) {
            startMinResPreFetch();
        }
    }
	
    public TileSet createLatestTiles() {
        TileSet result = new TileSet();
        for (ViewTileManager vtm : viewTileManagers) {
            if (vtm.getTileConsumer().isShowing()) {
                result.addAll(vtm.createLatestTiles());
            }
        }
        return result;
    }

    public Set<ViewTileManager> getViewTileManagers() {
        return viewTileManagers;
    }

    private void setLoadStatus(LoadStatus loadStatus) {
        if (this.loadStatus == loadStatus) {
            return; // no change
        }
        LOG.debug("Load status changed to " + loadStatus);
        this.loadStatus = loadStatus;
        if (loadStatusListener != null) {
            loadStatusListener.updateLoadStatus(loadStatus);
        }
    }

    public SharedVolumeImage getSharedVolumeImage() {
        return sharedVolumeImage;
    }

    private void setSharedVolumeImage(SharedVolumeImage sharedVolumeImage) {
        if (this.sharedVolumeImage == sharedVolumeImage) {
            return;
        }
        this.sharedVolumeImage = sharedVolumeImage;
        this.sharedVolumeImage.addVolumeLoadListener(this);
    }

    public TextureCache getTextureCache() {
        return textureCache;
    }

    private void updateLoadStatus() {
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
            if (!vtm.getTileConsumer().isShowing()) {
                continue;
            }
            totalVtmCount += 1;
            if (vtm.getLoadStatus() == ViewTileManager.LoadStatus.BEST_TEXTURES_LOADED) {
                bestVtmCount += 1;
            } else if (vtm.getLoadStatus() == ViewTileManager.LoadStatus.IMPERFECT_TEXTURES_LOADED) {
                imperfectVtmCount += 1;
            } else {
                emptyVtmCount += 1;
            }
        }
        LoadStatus activeLoadStatus;
        if (totalVtmCount == bestVtmCount) {
            activeLoadStatus = LoadStatus.BEST_TEXTURES_LOADED;
        } else if (emptyVtmCount == totalVtmCount) {
            activeLoadStatus = LoadStatus.NO_TEXTURES_LOADED;
        } else {
            activeLoadStatus = LoadStatus.IMPERFECT_TEXTURES_LOADED;
        }
        if (activeLoadStatus.ordinal() < LoadStatus.BEST_TEXTURES_LOADED.ordinal()) // precache does not matter if there are missing display textures
        {
            setLoadStatus(activeLoadStatus);
        } // Is prefetch cache full?
        else if (textureCache.hasQueuedTextures()) {
            setLoadStatus(activeLoadStatus);
        } else {
            setLoadStatus(LoadStatus.PREFETCH_COMPLETE);
        }
    }

    private void rearrangeLoadQueue(TileSet currentTiles) {
        for (ViewTileManager vtm : viewTileManagers) {
            vtm.updateDisplayTiles();
        }
        updateLoadStatus();

        futurePreFetcher.clear();

        Set<TileIndex> cacheableTextures = new HashSet<TileIndex>();
        int maxCacheable = (int) (0.90 * getTextureCache().getFutureCache().getMaxSize());

        LOG.debug("rearrangeLoadQueue for {} ViewTileManagers", viewTileManagers.size());
        // First in line are current display tiles
        // Prepare to analyze each ViewTileManager's loadStatus
        for (ViewTileManager vtm : viewTileManagers) {
            if (!vtm.getTileConsumer().isShowing()) {
                continue;
            }
            for (TileIndex ix : vtm.getNeededTextures()) {
                if (cacheableTextures.contains(ix)) {
                    continue; // already noted
                }
                if (futurePreFetcher.loadDisplayedTexture(ix, TileServer.this)) {
                    cacheableTextures.add(ix);
                }
            }
        }

        if (doPrefetch && !VolumeCache.useVolumeCache()) {
            // Sort tiles into X, Y, and Z slices to help with generators
            Map<CoordinateAxis, TileSet> axisTiles = new HashMap<>();
            for (Tile2d tile : currentTiles) {
                TileIndex i = tile.getIndex();
                CoordinateAxis axis = i.getSliceAxis();
                if (!axisTiles.containsKey(axis)) {
                    axisTiles.put(axis, new TileSet());
                }
                axisTiles.get(axis).add(tile);
            }
            // Create one umbrella generator for each (used) direction.
            List<Iterable<TileIndex>> umbrellas = new ArrayList<>();
            List<Iterable<TileIndex>> fullSlices = new ArrayList<>();
            for (CoordinateAxis axis : axisTiles.keySet()) {
                TileSet tiles = axisTiles.get(axis);
                // Umbrella Z scan
                umbrellas.add(new UmbrellaSliceGenerator(getLoadAdapter().getTileFormat(), tiles));
                // Full resolution Z scan
                fullSlices.add(new SliceGenerator(getLoadAdapter().getTileFormat(), tiles));
            }
            // Interleave the various umbrella generators
            if (umbrellas.size() > 0) {
                Iterable<TileIndex> combinedUmbrella;
                Iterable<TileIndex> combinedFullSlice;
                if (umbrellas.size() == 1) {
                    combinedUmbrella = umbrellas.get(0);
                    combinedFullSlice = fullSlices.get(0);
                } else { // more than one axis
                    Iterator<Iterable<TileIndex>> sliceIter = umbrellas.iterator();
                    combinedUmbrella = new InterleavedIterator<>(sliceIter.next(), sliceIter.next());
                    while (sliceIter.hasNext()) {
                        combinedUmbrella = new InterleavedIterator<>(combinedUmbrella, sliceIter.next());
                    }

                    sliceIter = fullSlices.iterator();
                    combinedFullSlice = new InterleavedIterator<>(sliceIter.next(), sliceIter.next());
                    while (sliceIter.hasNext()) {
                        combinedFullSlice = new InterleavedIterator<>(combinedFullSlice, sliceIter.next());
                    }
                }

                // Load umbrella slices
                for (TileIndex ix : combinedUmbrella) {
                    if (cacheableTextures.contains(ix)) {
                        continue;
                    }
                    if (cacheableTextures.size() >= maxCacheable) {
                        break;
                    }

                    if (futurePreFetcher.loadDisplayedTexture(ix, TileServer.this)) {
                        cacheableTextures.add(ix);
                    }
                }

                // Load full resolution slices
                for (TileIndex ix : combinedFullSlice) {
                    if (cacheableTextures.contains(ix)) {
                        continue;
                    }
                    if (cacheableTextures.size() >= maxCacheable) {
                        break;
                    }

                    if (futurePreFetcher.loadDisplayedTexture(ix, TileServer.this)) {
                        cacheableTextures.add(ix);
                    }
                }
            }
        }
        updateLoadStatus();
    }

    public void refreshCurrentTileSet() {
        LOG.trace("refreshCurrentTileSet");
        TileSet tiles = createLatestTiles();
        Set<TileIndex> indices = new HashSet<>();
        for (Tile2d t : tiles) {
            indices.add(t.getIndex());
        }
        if (indices.equals(currentDisplayTiles)) {
            return; // no change
        }
        currentDisplayTiles = indices;
        rearrangeLoadQueue(tiles);
    }

    public AbstractTextureLoadAdapter getLoadAdapter() {
        return sharedVolumeImage.getLoadAdapter();
    }

    public ImageBrightnessStats getCurrentBrightnessStats() {
        ImageBrightnessStats result = null;
        for (ViewTileManager vtm : viewTileManagers) {
            if (vtm == null) {
                continue;
            }
            TileSet tiles = vtm.getLatestTiles();
            if (tiles == null) {
                continue;
            }
            for (Tile2d tile : vtm.getLatestTiles()) {
                ImageBrightnessStats bs = tile.getBrightnessStats();
                if (result == null) {
                    result = bs;
                } else if (bs != null) {
                    result.combine(tile.getBrightnessStats());
                }
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
    }

    @Override
    public void componentShown(ComponentEvent e) {
        refreshCurrentTileSet();
    }

    @Override
    public void componentHidden(ComponentEvent e) {
        // do nothing
    }

    public void stop() {
        minResPreFetcher.clear();
        futurePreFetcher.clear();
    }

    //-------------------------------------------IMPLEMENTS VolumeLoadListener
    @Override
    public void volumeLoaded(URL url) {
        if (sharedVolumeImage == null) {
            return;
        }
        // Initialize pre-fetchers
        minResPreFetcher.setLoadAdapter(sharedVolumeImage.getLoadAdapter());
        futurePreFetcher.setLoadAdapter(sharedVolumeImage.getLoadAdapter());
        clearCache();
        refreshCurrentTileSet();
    }

}
