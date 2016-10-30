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

package org.janelia.horta.blocks;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ConcurrentHashMap;
import javax.media.opengl.GL3;
import org.janelia.console.viewerapi.ComposableObservable;
import org.janelia.console.viewerapi.ObservableInterface;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.geometry3d.ConstVector3;
import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.Vantage;
import org.janelia.geometry3d.Vector3;
import org.janelia.horta.actors.SortableBlockActor;
import org.janelia.horta.actors.SortableBlockActorSource;
import org.openide.util.RequestProcessor;

/**
 *
 * @author brunsc
 */
public class GpuTileCache
implements SortableBlockActorSource
{
    private final Map<BlockTileKey, RequestProcessor.Task> queuedTiles = new ConcurrentHashMap();
    private final Map<BlockTileKey, SortableBlockActor> cachedBlocks = new ConcurrentHashMap<>();
    private final Map<BlockTileKey, SortableBlockActor> displayedBlocks = new ConcurrentHashMap<>();
    private final Map<BlockTileKey, SortableBlockActor> obsoleteBlocks = new ConcurrentHashMap<>();

    
    private List<BlockTileKey> cachedDesiredBlocks;
    private final BlockChooser blockChooser;
    private ConstVector3 cachedFocus;
    // private List<BlockTileKey> desiredBlocks;
    private Vantage vantage;
    private final ObservableInterface displayChangeObservable = new ComposableObservable();
    private final CameraObserver cameraObserver = new CameraObserver();
    private BlockTileSource ktxSource;
    
    private RequestProcessor loadProcessor = new RequestProcessor("KtxTileLoad", 1, true);

    public GpuTileCache(BlockChooser blockChooser) {
        this.blockChooser = blockChooser;
    }
    
    public boolean canDisplay() {
        return ! displayedBlocks.isEmpty();
    }
    
    public void setVantage(Vantage vantage) {
        if (this.vantage == vantage)
            return;
        if (this.vantage != null) {
            this.vantage.deleteObserver(cameraObserver);
        }
        this.vantage = vantage;
        vantage.addObserver(cameraObserver);
    }

    public void setKtxSource(BlockTileSource ktxSource) {
        this.ktxSource = ktxSource;
    }

    public ObservableInterface getDisplayChangeObservable() {
        return displayChangeObservable;
    }
    
    public void refreshBlocks(ConstVector3 focus) 
    {
        if (ktxSource == null) {
            return;
        }
        if (focus.equals(cachedFocus)) {
            return; // short circuit when nothing has changed...
        }
        ConstVector3 previousFocus = cachedFocus;
        cachedFocus = new Vector3(focus);
        List<BlockTileKey> desiredBlocks = blockChooser.chooseBlocks(
                ktxSource, focus, previousFocus);
        if (desiredBlocks.equals(cachedDesiredBlocks))
            return; // no change in desired set
        cachedDesiredBlocks = desiredBlocks;
        boolean bChanged = false;
        for (BlockTileKey key : desiredBlocks) {
            // Is this block already displayed?
            if (displayedBlocks.containsKey(key))
                continue; // Already got this one
            // Is this block already available?
            if (obsoleteBlocks.containsKey(key)) {
                // Oops, rescue this one
                SortableBlockActor actor = obsoleteBlocks.get(key);
                obsoleteBlocks.remove(key);
                displayedBlocks.put(key, actor);
                bChanged = true;
                continue; // rescued block will be displayed next time
            }
            if (cachedBlocks.containsKey(key)) {
                SortableBlockActor actor = cachedBlocks.get(key);
                cachedBlocks.remove(key);
                displayedBlocks.put(key, actor);
                bChanged = true;
                continue; // rescued block will be displayed next time
            }
            // Is this block actively being loaded?
            queueLoad(key);
        }
        if (bChanged) {
            displayChangeObservable.setChanged();
        }
    }
    
    private void queueLoad(final BlockTileKey key) {
        if (queuedTiles.containsKey(key))
            return; // Already queued
        if (displayedBlocks.containsKey(key))
            return; // Already displayed
        
        final Runnable loader0 = new Runnable() {
            @Override
            public void run() {
                final KtxBlockLoadRunner loader1 = new KtxBlockLoadRunner(ktxSource, key);
                loader1.addObserver(new Observer() {
                    @Override
                    public void update(Observable o, Object arg) {
                        if (loader1.state != KtxBlockLoadRunner.State.LOADED)
                            return;
                        if (displayedBlocks.containsKey(key))
                            return; // Already displayed
                        if (! cachedDesiredBlocks.contains(key))
                            return; // No longer needed
                        // Remove obsolete blocks
                        Iterator<BlockTileKey> iter = displayedBlocks.keySet().iterator();
                        while(iter.hasNext()) {
                            BlockTileKey key = iter.next();
                            if (! cachedDesiredBlocks.contains(key)) {
                                SortableBlockActor actor = displayedBlocks.get(key);
                                obsoleteBlocks.put(key, actor);
                                iter.remove();
                            }
                        }
                        displayedBlocks.put(key, loader1.blockActor);
                        displayChangeObservable.setChanged();
                        displayChangeObservable.notifyObservers();
                    }
                });
                loader1.run();
                queuedTiles.remove(key);
            }
        };
        synchronized(queuedTiles) {
            queuedTiles.put(key, loadProcessor.post(loader0));
        }
    }
    
    public void displayGL(GL3 gl, AbstractCamera camera, Matrix4 parentModelViewMatrix) 
    {
        for (SortableBlockActor actor : displayedBlocks.values()) {
            actor.display(gl, camera, parentModelViewMatrix);
        }
    }
    
    public void disposeGL(GL3 gl) {
        disposeActorGroup(gl, obsoleteBlocks);
        disposeActorGroup(gl, cachedBlocks);
        disposeActorGroup(gl, displayedBlocks);
    }
    
    private void disposeActorGroup(GL3 gl, Map<BlockTileKey, SortableBlockActor> group) {
        for (SortableBlockActor actor : group.values())
            actor.dispose(gl);
        group.clear();        
    }

    // Dispose of obsolete actors
    public void updateGL(GL3 gl) {
        disposeActorGroup(gl, obsoleteBlocks);
    }

    @Override
    public Collection<SortableBlockActor> getSortableBlockActors() {
        return displayedBlocks.values();
    }

    public Iterable<SortableBlockActor> getDisplayedActors() {
        return displayedBlocks.values();
    }

    private class CameraObserver implements Observer {
        @Override
        public void update(Observable o, Object arg) {
            if (ktxSource == null) {
                return;
            }
            ConstVector3 focus = vantage.getFocusPosition();
            refreshBlocks(focus);
            if (displayChangeObservable.hasChanged())
                displayChangeObservable.notifyObservers();
        }
    }
}
