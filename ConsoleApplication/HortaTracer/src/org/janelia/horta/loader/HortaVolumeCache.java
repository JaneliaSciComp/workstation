/*
 * Licensed under the Janelia Farm Research Campus Software Copyright 1.1
 * 
 * Copyright (c) 2014, Howard Hughes Medical Institute, All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *     1. Redistributions of source code must retain the above copyright notice, 
 *        this list of conditions and the following disclaimer.
 *     2. Redistributions in binary form must reproduce the above copyright 
 *        notice, this list of conditions and the following disclaimer in the 
 *        documentation and/or other materials provided with the distribution.
 *     3. Neither the name of the Howard Hughes Medical Institute nor the names 
 *        of its contributors may be used to endorse or promote products derived 
 *        from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, ANY 
 * IMPLIED WARRANTIES OF MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A 
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * REASONABLE ROYALTIES; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.janelia.horta.loader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.SwingUtilities;

import org.eclipse.jetty.util.ConcurrentHashSet;
import org.janelia.geometry3d.BrightnessModel;
import org.janelia.geometry3d.PerspectiveCamera;
import org.janelia.gltools.material.VolumeMipMaterial;
import org.janelia.gltools.texture.Texture3d;
import org.janelia.horta.BrainTileInfo;
import org.janelia.horta.NeuronTraceLoader;
import org.janelia.horta.volume.BrickActor;
import org.janelia.horta.volume.BrickInfo;
import org.janelia.horta.volume.BrickInfoSet;
import org.janelia.horta.volume.StaticVolumeBrickSource;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.util.Exceptions;
import org.openide.util.RequestProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keeps in memory a configurable number of nearby volume tiles.
 * Manages transfer of volume imagery:
 *   A) from disk/network, 
 *   B) to RAM, 
 *   C) and thence to GPU video memory
 * 
 * TODO: Trim tiles to non-overlapping subvolumes before processing.
 * 
 * @author brunsc
 */
public class HortaVolumeCache
{
    private static final Logger log = LoggerFactory.getLogger(HortaVolumeCache.class);

    private int ramTileCount = 3; // Three is better than two for tile availability
    private int gpuTileCount = 1;
    private final PerspectiveCamera camera;
    private StaticVolumeBrickSource source = null;
    
    // Lightweight metadata
    private final Collection<BrickInfo> nearVolumeMetadata = new ConcurrentHashSet<>();

    // Large in-memory cache
    private final Map<BrickInfo, Texture3d> nearVolumeInRam = new ConcurrentHashMap<>();

    private final Map<BrickInfo, RequestProcessor.Task> queuedTiles = new ConcurrentHashMap();
    private final Map<BrickInfo, RequestProcessor.Task> loadingTiles = new ConcurrentHashMap();

    // Fewer on GPU cache
    private final Map<BrickInfo, BrickActor> actualDisplayTiles = new ConcurrentHashMap<>();
    private final Collection<BrickInfo> desiredDisplayTiles = new ConcurrentHashSet<>();
    
    // To enable/disable loading
    private boolean doUpdateCache = true;
    
    // Cache camera data for early termination
    float cachedFocusX = Float.NaN;
    float cachedFocusY = Float.NaN;
    float cachedFocusZ = Float.NaN;
    float cachedZoom = Float.NaN;
    
    private final RequestProcessor loadProcessor = new RequestProcessor("VolumeTileLoad", 1, true);
    private final BrightnessModel brightnessModel;
    private final VolumeMipMaterial.VolumeState volumeState;
    private final Collection<TileDisplayObserver> observers = new java.util.concurrent.ConcurrentLinkedQueue<>();
    private int currentColorChannel = 0;

    public HortaVolumeCache(final PerspectiveCamera camera, 
            final BrightnessModel brightnessModel,
            final VolumeMipMaterial.VolumeState volumeState,
            int currentColorChannel) 
    {
        this.brightnessModel = brightnessModel;
        this.volumeState = volumeState;
        this.currentColorChannel = currentColorChannel;

        this.camera = camera;
        camera.addObserver(new CameraObserver());
    }

    public void registerLoneDisplayedTile(BrickActor actor) 
    {
        if (actualDisplayTiles.containsKey(actor.getBrainTile()))
            return;
        Texture3d texture = ((VolumeMipMaterial)actor.getMaterial()).getTexture();
        synchronized(actualDisplayTiles) {
            nearVolumeMetadata.clear();
            nearVolumeMetadata.add(actor.getBrainTile());
            nearVolumeInRam.clear();
            nearVolumeInRam.put(actor.getBrainTile(), texture);
            actualDisplayTiles.clear();
            actualDisplayTiles.put(actor.getBrainTile(), actor);
        }
    }
    
    public boolean isUpdateCache() {
        return doUpdateCache;
    }

    public void setUpdateCache(boolean doUpdateCache) {
        if (this.doUpdateCache == doUpdateCache)
            return; // no change
        this.doUpdateCache = doUpdateCache;
        if (this.doUpdateCache) {
            updateLocation(); // Begin any pending loads
        }
    }
    
    public void toggleUpdateCache() {
        setUpdateCache(! isUpdateCache());
    }
    
    public int getRamTileCount() {
        return ramTileCount;
    }

    public void setRamTileCount(int ramTileCount) {
        this.ramTileCount = ramTileCount;
    }

    public StaticVolumeBrickSource getSource() {
        return source;
    }

    public void setSource(StaticVolumeBrickSource source) {
        this.source = source;
    }

    public int getGpuTileCount() {
        return gpuTileCount;
    }

    public void setGpuTileCount(int videoTileCount) {
        this.gpuTileCount = videoTileCount;
    }
    
    private void updateLocation() {
        updateLocation(camera);
    }
    
    private void updateLocation(PerspectiveCamera cam) {
        float[] focusXyz = cam.getVantage().getFocus();
        float zoom = cam.getVantage().getSceneUnitsPerViewportHeight();
        updateLocation(focusXyz, zoom);
    }
    
    public void updateLocation(float[] xyz, float zoom) 
    {
        if (! doUpdateCache)
            return;

        // Cache previous location for early termination
        if (xyz[0] == cachedFocusX
                && xyz[1] == cachedFocusY
                && xyz[2] == cachedFocusZ
                && zoom == cachedZoom) 
        {
            return; // no important change to camera
        }
        cachedFocusX = xyz[0];
        cachedFocusY = xyz[1];
        cachedFocusZ = xyz[2];
        cachedZoom = zoom;
        
        // System.out.println("HortaVolumeCache location changed");

        if (source == null) {
            return;
        }
        
        // Find the metadata for the closest volume tiles
        BrickInfoSet allBricks = NeuronTraceLoader.getBricksForCameraResolution(source, camera);
        Collection<BrickInfo> closestBricks = allBricks.getClosestBricks(xyz, ramTileCount);
        Collection<BrickInfo> veryClosestBricks = allBricks.getClosestBricks(xyz, gpuTileCount);

        synchronized(desiredDisplayTiles) {
            desiredDisplayTiles.clear();
            desiredDisplayTiles.addAll(veryClosestBricks);
        }
        
        // Just in case; should not be necessary
        // closestBricks.addAll(veryClosestBricks);
        
        // Compare to cached list of tile metadata
        // Create list of new and obsolete tiles
        Collection<BrickInfo> obsoleteBricks = new ArrayList<>();
        Collection<BrickInfo> newBricks = new ArrayList<>();
        for (BrickInfo brickInfo : nearVolumeMetadata) {
            if (closestBricks.contains(brickInfo)) 
                continue;
            obsoleteBricks.add(brickInfo);
        }
        for (BrickInfo brickInfo : closestBricks) {
            if (nearVolumeMetadata.contains(brickInfo))
                continue;
            newBricks.add(brickInfo);
        }
        
        // Update local cache
        nearVolumeMetadata.removeAll(obsoleteBricks);
        nearVolumeMetadata.addAll(newBricks);
        // but don't update new tiles in ram until they are loaded

        // Upload closest tiles to GPU, if already loaded in RAM
        for (BrickInfo brick : desiredDisplayTiles) { // These are the tiles we want do display right now.
            BrainTileInfo tile = (BrainTileInfo)brick;
            if (actualDisplayTiles.containsKey(brick)) {// Is it already displayed?
                log.debug("Already displaying: "+tile.getLocalPath());
                continue; // already loaded
            }
            if (nearVolumeInRam.containsKey(brick)) { // Is the texture ready?
                log.debug("Already in RAM: "+tile.getLocalPath());
                uploadToGpu((BrainTileInfo)brick); // then display it!
            }
        }
        
        // Begin loading the new tiles asynchronously to RAM
        // ...starting with the ones we want to display right now
        for (BrickInfo brick : desiredDisplayTiles) {
            BrainTileInfo tile = (BrainTileInfo)brick;
            if (actualDisplayTiles.containsKey(brick)) {// Is it already displayed?
                continue; // already displayed
            }
            if (nearVolumeInRam.containsKey(brick)) { // Is the texture already loaded in RAM?
                continue; // already loaded
            }
            log.debug("Queueing brick with norm priority: "+tile.getLocalPath());
            queueLoad(tile, Thread.NORM_PRIORITY);
        }
        for (BrickInfo brick : newBricks) {
            BrainTileInfo tile = (BrainTileInfo)brick;
            log.debug("Queueing brick with min priority: "+tile.getLocalPath());
            queueLoad(tile, Thread.MIN_PRIORITY); // Don't worry; duplicates will be skipped
        }

        // Begin deleting the old tiles        
        for (BrickInfo brick : obsoleteBricks) {
            BrainTileInfo tile = (BrainTileInfo)brick;
            log.info("Removing from RAM: "+tile.getLocalPath());
            nearVolumeInRam.remove(brick);
        }
    }
    
    private void queueLoad(final BrainTileInfo tile, int priority) 
    {
        if (! doUpdateCache)
            return;

        if (loadingTiles.containsKey(tile))
            return; // already loading

        // System.out.println("Horta Volume cache loading tile "+tile.getLocalPath()+"...");

        Runnable loadTask = new Runnable() {
            @Override
            public void run() {

                log.info("Beginning load for {}", tile.getLocalPath());

                if (Thread.currentThread().isInterrupted()) {
                    log.info("loadTask was interrupted before it began");
                    queuedTiles.remove(tile);
                    return;
                }

                // Move from "queued" to "loading" state
                synchronized(queuedTiles) {
                    RequestProcessor.Task task = queuedTiles.get(tile);
                    if (task==null) {
                        log.warn("Tile has no task: "+tile.getLocalPath());
                        return;
                    }
                    loadingTiles.put(tile, task);
                    queuedTiles.remove(tile);
                }

                ProgressHandle progress = ProgressHandleFactory.createHandle("Loading Tile " + tile.getLocalPath() + " ...");

                try {
                    // Check whether this tile is still relevant
                    if (! nearVolumeMetadata.contains(tile)) {
                        return;
                    }

                    // Throttle to slow down loading of too many tiles if user is moving a lot
    //                if (queuedForLoad.size() > 2) { // Hmm... Many things are already loading, so maybe I should play it cool...
    //                    try {
    //                        Thread.sleep(1000); // milliseconds to wait before starting load
    //                    } catch (InterruptedException ex) {
    //                    }
    //                }

                    // Maybe after that wait, this tile is no longer needed
                    if (! nearVolumeMetadata.contains(tile)) {
                        return;
                    }

                    if (! doUpdateCache) {
                        return;
                    }

                    progress.start();
                    progress.setDisplayName("Loading Tile " + tile.getLocalPath() + " ...");
                    progress.switchToIndeterminate();
                    Texture3d tileTexture = tile.loadBrick(10, currentColorChannel);
                    if (tileTexture!=null) {
                        if (nearVolumeMetadata.contains(tile)) { // Make sure this tile is still desired after loading
                            nearVolumeInRam.put(tile, tileTexture);
                            // Trigger GPU upload, if appropriate
                            if (desiredDisplayTiles.contains(tile)) {
                                // Trigger GPU upload
                                uploadToGpu(tile);
                            }
                        }
                    }
                    else {
                        log.info("Load was interrupted for: {}", tile.getLocalPath());
                    }
                }
                catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
                finally {
                    loadingTiles.remove(tile);
                    progress.finish();
                }
            };
        };
        // Submit load task asynchronously
        int start_lag = 0; // milliseconds
        if (priority < Thread.NORM_PRIORITY) {
            start_lag = 0;
        }

        synchronized (queuedTiles) {
            if (priority == Thread.NORM_PRIORITY) {
                log.debug("Cancelling all current tasks to make room for {}", tile.getLocalPath());
                // Cancel all current tasks so that this one can execute with haste
                if (expediteTileLoad(queuedTiles, tile)) return;
                if (expediteTileLoad(loadingTiles, tile)) return;
            }
            log.info("Queueing brick {} with priority {} (queued={}, loading={})", tile.getLocalPath(),priority, queuedTiles.size(), loadingTiles.size());
            queuedTiles.put(tile, loadProcessor.post(loadTask, start_lag, priority));
        }
    }

    private boolean expediteTileLoad(Map<BrickInfo, RequestProcessor.Task> taskMap, BrainTileInfo wantedTile) {
        // Walk the map and abort if the given wanted tile is already there. Cancel any other tasks that may be in its way.
        for(Iterator<Map.Entry<BrickInfo, RequestProcessor.Task>> iterator = taskMap.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<BrickInfo, RequestProcessor.Task> entry = iterator.next();
            BrainTileInfo tileToCancel = (BrainTileInfo)entry.getKey();
            RequestProcessor.Task task = entry.getValue();
            if (tileToCancel.equals(wantedTile) && task.getPriority()==Thread.NORM_PRIORITY) {
                // The tile we want is already loading at the correct priority
                log.debug("Tile is already in flight: {}", tileToCancel.getLocalPath());
                return true;
            }
            log.info("Cancelling load for {} (priority {})", tileToCancel.getLocalPath(), task.getPriority());
            task.cancel();
            iterator.remove();
        }
        return false;
    }

    private void uploadToGpu(BrainTileInfo brick) {
        if (! doUpdateCache)
            return;
        if (actualDisplayTiles.containsKey(brick))
            return; // already displayed
        if (! desiredDisplayTiles.contains(brick))
            return; // not needed
        
        Texture3d texture3d = nearVolumeInRam.get(brick);
        if (texture3d == null) {
            log.error("Volume should be loaded but isn't: "+brick.getLocalPath());
            return; // Sorry, that volume is not loaded FIXME: error handling here
        }

        log.info("Loading to GPU: "+brick.getLocalPath());

        // System.out.println("I should be displaying tile " + brick.getLocalPath() + " now");
        final BrickActor actor = new BrickActor(brick, texture3d, brightnessModel, volumeState);
        actualDisplayTiles.put(brick, actor);
        
        // Hide obsolete displayed tiles
        int hideCount = actualDisplayTiles.size() - desiredDisplayTiles.size() + 1;
        if (hideCount > 0) {
            // Using iterator to avoid ConcurrentModificationException
            Iterator<Map.Entry<BrickInfo, BrickActor>> iter = actualDisplayTiles.entrySet().iterator();
            while(iter.hasNext()) {
                Map.Entry<BrickInfo, BrickActor> entry = iter.next();
                BrainTileInfo tile = (BrainTileInfo)entry.getKey();
                if (! desiredDisplayTiles.contains(tile)) {
                    iter.remove();
                    // System.out.println("I should be UNdisplaying tile " + tile.getLocalPath() + " now");        
                    hideCount --;
                    if (hideCount <= 0)
                        break;
                }
            }
        }
        
        fireUpdateInEDT(actor);
    }

    public void addObserver(TileDisplayObserver observer) {
        if (observers.contains(observer))
            return;
        observers.add(observer);
    }

    public void deleteObserver(TileDisplayObserver observer) {
        observers.remove(observer);
    }

    public void deleteObservers() {
        observers.clear();
    }

    public void setColorChannel(int colorChannel) {
        this.currentColorChannel = colorChannel;
    }

    private class CameraObserver implements Observer
    {
        @Override
        public void update(Observable o, Object arg) {
            updateLocation();
        }
    }
    
    public static interface TileDisplayObserver {
        public void update(BrickActor newTile, Collection<? extends BrickInfo> allTiles);
    }

    private void fireUpdateInEDT(final BrickActor actor) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                for (TileDisplayObserver observer : observers) {
                    observer.update(actor, actualDisplayTiles.keySet());
                }
            }
        });
    }
}