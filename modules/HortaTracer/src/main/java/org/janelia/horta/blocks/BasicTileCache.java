package org.janelia.horta.blocks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

import org.janelia.geometry3d.ComposableObservable;
import org.janelia.geometry3d.ObservableInterface;
import org.janelia.horta.options.TileLoadingPanel;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.util.NbPreferences;
import org.openide.util.RequestProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic version of HortaVolumeCache, for use with newer Ktx block loading
 *
 * @author brunsc
 * @param <TILE_KEY> - tile key type
 * @param <TILE_DATA> - tile data type
 */
public abstract class BasicTileCache<TILE_KEY, TILE_DATA> {

    public interface LoadRunner<TILE_KEY, TILE_DATA> {
        TILE_DATA loadTile(TILE_KEY key) throws InterruptedException, IOException;
    }

    private final Map<TILE_KEY, RequestProcessor.Task> queuedTiles = new ConcurrentHashMap<>();
    private final Map<TILE_KEY, RequestProcessor.Task> loadingTiles = new ConcurrentHashMap<>();

    private final Set<TILE_KEY> nearVolumeMetadata = new HashSet<>();

    final Map<TILE_KEY, TILE_DATA> nearVolumeInRam = new ConcurrentHashMap<>();
    final Map<TILE_KEY, TILE_DATA> obsoleteTiles = new ConcurrentHashMap<>();

    // To enable/disable loading
    BlockChooser blockStrategy;
    private RequestProcessor loadProcessor;
    private final ObservableInterface displayChangeObservable = new ComposableObservable();

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    BasicTileCache() {
        Preferences pref = NbPreferences.forModule(TileLoadingPanel.class);

        String concurrentLoadsStr = pref.get(TileLoadingPanel.PREFERENCE_CONCURRENT_LOADS, TileLoadingPanel.PREFERENCE_CONCURRENT_LOADS_DEFAULT);
        setConcurrentLoads(concurrentLoadsStr);

        pref.addPreferenceChangeListener(new PreferenceChangeListener() {
            @Override
            public void preferenceChange(PreferenceChangeEvent evt) {
                if (evt.getKey().equals(TileLoadingPanel.PREFERENCE_CONCURRENT_LOADS)) {
                    setConcurrentLoads(evt.getNewValue());
                }
            }
        });

    }

    abstract LoadRunner<TILE_KEY, TILE_DATA> getLoadRunner();

    public int getBlockCount() {
        return nearVolumeMetadata.size();
    }

    public synchronized void addDesiredTile(TILE_KEY key) {
        if (nearVolumeMetadata.contains(key)) {
            return; // already queued
        }
        nearVolumeMetadata.add(key);
        if (nearVolumeInRam.containsKey(key)) {
            return; // already loaded
        }
        queueLoad(key, getLoadRunner());
    }

    public synchronized void updateDesiredTiles(List<TILE_KEY> desiredTiles) {
        List<TILE_KEY> newTiles = new ArrayList<>();
        for (TILE_KEY key : desiredTiles) {
            if (!nearVolumeMetadata.contains(key)) {
                nearVolumeMetadata.add(key);
            }

            if (queuedTiles.containsKey(key)) {
                continue; // already queued
            }
            if (loadingTiles.containsKey(key)) {
                continue; // already loading
            }
            if (nearVolumeInRam.containsKey(key)) {
                continue; // already loaded
            }
            newTiles.add(key);
        }

        removeIfNotDesired (desiredTiles, queuedTiles);
        removeIfNotDesired (desiredTiles, loadingTiles);

        for (TILE_KEY key : newTiles) {
            queueLoad(key, getLoadRunner());
        }
    }

    private void removeIfNotDesired (List<TILE_KEY> desiredTiles, Map<TILE_KEY, RequestProcessor.Task> tileSet) {
        Iterator<Map.Entry<TILE_KEY, RequestProcessor.Task>> mapIter = tileSet.entrySet().iterator();
        while (mapIter.hasNext()) {
            Map.Entry<TILE_KEY, RequestProcessor.Task> entry = mapIter.next();
            TILE_KEY key = entry.getKey();
            if (!desiredTiles.contains(key)) {
                RequestProcessor.Task task = loadingTiles.get(key);
                if (task != null) {
                    task.cancel();
                }
                mapIter.remove();
            }
        }
    }

    public ObservableInterface getDisplayChangeObservable() {
        return displayChangeObservable;
    }

    public Collection<TILE_DATA> popObsoleteTiles() {
        Collection<TILE_DATA> result = new ArrayList<>(obsoleteTiles.values());
        if (!result.isEmpty()) {
            obsoleteTiles.clear();
        }
        return result;
    }

    public void clearAllTiles() {
        obsoleteTiles.clear();
        nearVolumeInRam.clear();
    }

    private synchronized boolean queueLoad(final TILE_KEY key, final LoadRunner<TILE_KEY, TILE_DATA> loadRunner) {
        if (queuedTiles.containsKey(key)) {
            return false; // already queued
        }
        if (loadingTiles.containsKey(key)) {
            return false; // already loading
        }
        Runnable loadTask = new Runnable() {
            @Override
            public void run() {
                // Move from "queued" to "loading" state
                synchronized (queuedTiles) {
                    RequestProcessor.Task task = queuedTiles.get(key);
                    if (task == null) {
                        log.warn("Tile has no task: " + getTileName(key));
                        return;
                    }
                    log.info("Tile has loaded: {}", getTileName(key));
                    loadingTiles.put(key, task);
                    queuedTiles.remove(key);
                }

                ProgressHandle progress = ProgressHandleFactory.createHandle("Loading Tile " + getTileName(key) + " ...", null, null);

                try {
                    // Check whether this tile is still relevant
                    if (!nearVolumeMetadata.contains(key)) {
                        return;
                    }

                    progress.start();
                    progress.setDisplayName("Loading Tile " + getTileName(key) + " ...");
                    progress.switchToIndeterminate();

                    log.debug("Tile cache load tile data for {}", key);

                    TILE_DATA tileTexture = loadRunner.loadTile(key);

                    if (tileTexture == null) {
                        log.info("Tile loaded was null {}", getTileName(key));
                        return;
                    }

                    if (!nearVolumeMetadata.contains(key)) {
                        log.info("Tile loaded was no longer needed {}", getTileName(key));
                        return; // no longer needed
                    }

                    if (nearVolumeInRam.containsKey(key)) {
                        log.info("Tile loaded was already loaded {}", getTileName(key));
                        return; // already loaded by another thread?
                    }

                    nearVolumeInRam.put(key, tileTexture);
                    displayChangeObservable.setChanged();
                    displayChangeObservable.notifyObservers();
                } catch (IOException ex) {
                    log.info("loadTask was IOException {}", getTileName(key), ex);
                } catch (InterruptedException ex) {
                    log.info("loadTask was interrupted {}", getTileName(key), ex);
                } finally {
                    loadingTiles.remove(key);
                    // figure out if there are tiles we need to remove after successful load of a tile
                    Map<TILE_KEY, TILE_DATA> obsoleteTiles = blockStrategy.chooseObsoleteTiles(nearVolumeInRam, queuedTiles, (BlockTileKey)key);
                    if (obsoleteTiles != null) {
                        for (TILE_KEY key : obsoleteTiles.keySet()) {
                            nearVolumeInRam.remove(key);
                        }
                    }
                    progress.finish();
                }
            }
        };

        // Submit load task asynchronously
        synchronized (queuedTiles) {
            log.info("Queueing brick {} (queued={}, loading={})", getTileName(key), queuedTiles.size(), loadingTiles.size());
            queuedTiles.put(key, loadProcessor.post(loadTask));
        }
        return true;
    }

    private void setConcurrentLoads(String preferenceValue) {
        int loadThreads = Integer.parseInt(preferenceValue);
        log.info("Configuring loadThreads={}", loadThreads);
        if (loadProcessor != null) {
            loadProcessor.shutdown();
        }
        loadProcessor = new RequestProcessor("VolumeTileLoad", loadThreads, true);
    }

    public boolean canDisplay() {
        if (nearVolumeInRam.isEmpty()) {
            return false;
        }
        if (nearVolumeMetadata.isEmpty()) {
            return false;
        }
        for (TILE_KEY key : nearVolumeMetadata) {
            if (nearVolumeInRam.containsKey(key)) {
                return true;
            }
        }
        return false;
    }

    public Collection<TILE_DATA> getDisplayedActors() {
        List<TILE_DATA> result = new ArrayList<>();
        for (TILE_KEY key : nearVolumeMetadata) {
            if (nearVolumeInRam.containsKey(key)) {
                result.add(nearVolumeInRam.get(key));
            }
        }
        return result;
    }

    public BlockChooser getBlockStrategy() {
        return blockStrategy;
    }

    public void setBlockStrategy(BlockChooser blockStrategy) {
        this.blockStrategy = blockStrategy;
    }

    protected String getTileName(TILE_KEY key) {
        return key.toString();
    }
}
