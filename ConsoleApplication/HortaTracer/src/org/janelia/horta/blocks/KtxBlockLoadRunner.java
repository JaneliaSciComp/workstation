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

import java.io.IOException;
import java.io.InputStream;
import org.janelia.console.viewerapi.ComposableObservable;
import org.janelia.horta.actors.TetVolumeActor;
import org.janelia.horta.actors.TetVolumeMeshActor;
import org.janelia.horta.ktx.KtxData;
import org.janelia.horta.loader.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brunsc
 */
public class KtxBlockLoadRunner
        extends ComposableObservable
        implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(KtxBlockLoadRunner.class);

    public enum State {
        INITIAL,
        LOADING,
        INTERRUPTED,
        LOADED,
        FAILED,
    }

    private DataSource ktxStreamDataSource;
    private KtxOctreeBlockTileSource ktxBlockTileSource;
    private KtxOctreeBlockTileKey ktxOctreeBlockTileKey;

    public State state = State.INITIAL;
    public TetVolumeMeshActor blockActor;

    public KtxBlockLoadRunner(DataSource ktxStreamDataSource) {
        this.ktxStreamDataSource = ktxStreamDataSource;
    }

    public KtxBlockLoadRunner(KtxOctreeBlockTileSource source, KtxOctreeBlockTileKey key) {
        this.ktxBlockTileSource = source;
        this.ktxOctreeBlockTileKey = key;
    }

    private void loadFromBlockSource() {
        try (InputStream is = ktxBlockTileSource.streamKeyBlock(ktxOctreeBlockTileKey)) {
            loadStream(ktxBlockTileSource.getOriginatingSampleURL().toString(), is);
        } catch (IOException ex) {
            LOG.warn("IOException loading tile {} from block source", ktxOctreeBlockTileKey);
            state = State.FAILED;
        }
    }

    @Override
    public void run() {
        if (ktxStreamDataSource == null) {
            loadFromBlockSource();
        } else {
            loadStream(ktxStreamDataSource.getFileName(), ktxStreamDataSource.getInputStream());
        }
    }

    private void loadStream(String sourceName, InputStream stream) {
        long start = System.nanoTime();
        state = State.LOADING;
        KtxData ktxData = new KtxData();
        String blockDescription = "Some Ktx block...";
        if (ktxOctreeBlockTileKey != null) {
            blockDescription = ktxOctreeBlockTileKey.toString();
        }
        try {
            ktxData.loadStreamInterruptably(stream);
            if (ktxOctreeBlockTileKey == null) {
                blockDescription = ktxData.header.keyValueMetadata.get("octree_path");
            }
        } catch (IOException ex) {
            state = State.FAILED;
            LOG.warn("IOException loading tile {} from stream", blockDescription);
            return;
        } catch (InterruptedException ex) {
            LOG.info("loading tile {} was interrupted", blockDescription);
            state = State.INTERRUPTED;
            return;
        }
        TetVolumeActor parentActor = TetVolumeActor.getInstance();
        blockActor = new TetVolumeMeshActor(ktxData, parentActor);
        state = State.LOADED;
        setChanged();
        long end = System.nanoTime();
        double elapsed = (end - start) / 1.0e9;
        LOG.info("Loading ktx tile {} from {} took {} seconds", blockDescription, sourceName, elapsed);
        // notify listeners
        notifyObservers();
    }

}
