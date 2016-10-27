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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.media.opengl.GL3;
import org.janelia.geometry3d.Vector3;
import org.janelia.horta.actors.SortableBlockActor;
import org.janelia.horta.actors.SortableBlockActorSource;

/**
 *
 * @author brunsc
 */
public class GpuTileCache
implements SortableBlockActorSource
{
    private final Map<BlockTileKey, SortableBlockActor> cachedBlocks = new ConcurrentHashMap<>();
    private final Map<BlockTileKey, SortableBlockActor> displayedBlocks = new ConcurrentHashMap<>();
    private final Map<BlockTileKey, SortableBlockActor> obsoleteBlocks = new ConcurrentHashMap<>();
    private final BlockChooser blockChooser;
    private Vector3 cachedFocus;
    private List<BlockTileKey> desiredBlocks;
    
    public GpuTileCache(BlockChooser blockChooser) {
        this.blockChooser = blockChooser;
    }

    public boolean refreshBlocks(Vector3 focus) 
    {
        if (focus.equals(cachedFocus)) {
            return false; // short circuit when nothing has changed...
        }
        Vector3 previousFocus = cachedFocus;
        cachedFocus = focus;
        desiredBlocks = blockChooser.chooseBlocks(null, focus, previousFocus);
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
            // TODO:
        }
        return bChanged;
    }
    
    // Dispose of obsolete actors
    public void updateGL(GL3 gl) {
        for (SortableBlockActor actor : obsoleteBlocks.values()) {
            actor.dispose(gl);
        }
        obsoleteBlocks.clear();
    }

    @Override
    public Collection<SortableBlockActor> getSortableBlockActors() {
        return displayedBlocks.values();
    }
}
