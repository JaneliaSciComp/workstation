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
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.SwingUtilities;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.janelia.console.viewerapi.BasicGenericObservable;
import org.janelia.console.viewerapi.GenericObservable;
import org.janelia.console.viewerapi.GenericObserver;
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
    private int ramTileCount = 2;
    private int gpuTileCount = 1;
    private final PerspectiveCamera camera;
    private StaticVolumeBrickSource source = null;
    
    // Lightweight metadata
    private final Collection<BrickInfo> nearVolumeMetadata = new ConcurrentHashSet<>();

    // Large in-memory cache
    private final Map<BrickInfo, Texture3d> nearVolumeInRam = new ConcurrentHashMap<>();
    private final Collection<BrickInfo> queuedForLoad = new ConcurrentHashSet();
    
    // Fewer on GPU cache
    private final Map<BrickInfo, BrickActor> actualDisplayTiles = new ConcurrentHashMap<>();
    private final Collection<BrickInfo> desiredDisplayTiles = new ConcurrentHashSet<>();
    
    // Cache camera data for early termination
    float cachedFocusX = Float.NaN;
    float cachedFocusY = Float.NaN;
    float cachedFocusZ = Float.NaN;
    float cachedZoom = Float.NaN;
    
    private final RequestProcessor loadProcessor = new RequestProcessor("for loading tiles", 10);
    private final BrightnessModel brightnessModel;
    private final VolumeMipMaterial.VolumeState volumeState;
    private final Collection<TileDisplayObserver> observers = new java.util.concurrent.ConcurrentLinkedQueue<>();

    public HortaVolumeCache(final PerspectiveCamera camera, 
            final BrightnessModel brightnessModel,
            final VolumeMipMaterial.VolumeState volumeState) 
    {
        this.brightnessModel = brightnessModel;
        this.volumeState = volumeState;
        this.camera = camera;
        camera.addObserver(new CameraObserver());
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
    
    public void updateLocation(float[] xyz, float zoom) 
    {
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
        closestBricks.addAll(veryClosestBricks);
        
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
            if (actualDisplayTiles.containsKey(brick)) // Is it already displayed?
                continue; // already loaded
            if (nearVolumeInRam.containsKey(brick)) { // Is the texture ready?
                uploadToGpu((BrainTileInfo)brick); // then display it!
            }
        }
        
        // Begin loading the new tiles asynchronously to RAM
        // ...starting with the ones we want to display right now
        for (final BrickInfo brick : desiredDisplayTiles) 
        {
            if (actualDisplayTiles.containsKey(brick)) // Is it already displayed?
                continue; // already displayed           
            if (nearVolumeInRam.containsKey(brick)) // Is the texture already loaded in RAM?
                continue; // already loaded
            queueLoad((BrainTileInfo)brick);
        }
        for (final BrickInfo brick : newBricks) 
        {
            queueLoad((BrainTileInfo)brick); // Don't worry; duplicates will be skipped
        }

        // Begin deleting the old tiles        
        for (BrickInfo brick : obsoleteBricks) {
            nearVolumeInRam.remove(brick);
        }
    }
    
    private void queueLoad(final BrainTileInfo tile) 
    {
        if (queuedForLoad.contains(tile))
            return; // already loading
        queuedForLoad.add(tile);
        System.out.println("Horta Volume cache loading tile "+tile.getLocalPath()+"...");
        Runnable loadTask = new Runnable() {
            @Override
            public void run() {
                // Throttle to slow down loading of too many tiles if user is moving a lot
                if (queuedForLoad.size() > 3) { // Hmm... Many things are already loading, so maybe I should play it cool...
                    try {
                        Thread.sleep(1000); // milliseconds to wait before starting load
                    } catch (InterruptedException ex) {
                    }
                }
                // Maybe after that wait, this tile is no longer needed
                if (! nearVolumeMetadata.contains(tile)) {
                    queuedForLoad.remove(tile);
                    return;
                }

                ProgressHandle progress
                    = ProgressHandleFactory.createHandle("Loading Tile " + tile.getLocalPath() + " ...");
                progress.start();
                progress.setDisplayName("Loading Tile " + tile.getLocalPath() + " ...");
                progress.switchToIndeterminate();
                try {
                    Texture3d tileTexture = tile.loadBrick(10);
                    if (nearVolumeMetadata.contains(tile)) { // Make sure this tile is still desired after loading
                        nearVolumeInRam.put(tile, tileTexture);
                        // Trigger GPU upload, if appropriate
                        if (desiredDisplayTiles.contains(tile)) {
                            // Trigger GPU upload
                            uploadToGpu(tile);
                        }
                    }
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
                finally {
                    queuedForLoad.remove(tile);
                    progress.finish();
                }
            };
        };
        // Submit load task asynchronously
        loadProcessor.post(loadTask);
    }
    
    private void uploadToGpu(BrainTileInfo brick) {
        if (actualDisplayTiles.containsKey(brick))
            return; // already displayed
        if (! desiredDisplayTiles.contains(brick))
            return; // not needed
        
        Texture3d texture3d = nearVolumeInRam.get(brick);
        if (texture3d == null)
            return; // Sorry, that volume is not loaded FIXME: error handling here
        
        // TODO:
        System.out.println("I should be displaying tile " + brick.getLocalPath() + " now");
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
                    // TODO:
                    System.out.println("I should be UNdisplaying tile " + tile.getLocalPath() + " now");        
                    hideCount --;
                    if (hideCount <= 0)
                        break;
                }
            }
        }
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                for (TileDisplayObserver observer : observers) {
                    observer.update(actor, actualDisplayTiles.keySet());
                }
            }
        });
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

    private class CameraObserver implements Observer
    {
        @Override
        public void update(Observable o, Object arg) {
            float[] focusXyz = camera.getVantage().getFocus();
            float zoom = camera.getVantage().getSceneUnitsPerViewportHeight();
            updateLocation(focusXyz, zoom);
        }
    }
    
    public static interface TileDisplayObserver {
        public void update(BrickActor newTile, Collection<? extends BrickInfo> allTiles);
    }
}
