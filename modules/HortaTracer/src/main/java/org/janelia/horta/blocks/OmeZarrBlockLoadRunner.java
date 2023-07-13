package org.janelia.horta.blocks;

import org.janelia.geometry3d.ComposableObservable;
import org.janelia.horta.actors.OmeZarrVolumeActor;
import org.janelia.horta.actors.OmeZarrVolumeMeshActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class OmeZarrBlockLoadRunner extends ComposableObservable implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(OmeZarrBlockLoadRunner.class);

    public enum State {
        INITIAL,
        LOADING,
        INTERRUPTED,
        LOADED,
        FAILED,
    }

    private OmeZarrBlockTileSource omeZarrBlockTileSource;
    private OmeZarrBlockTileKey omeZarrOctreeBlockTileKey;

    public OmeZarrBlockLoadRunner.State state = OmeZarrBlockLoadRunner.State.INITIAL;

    public OmeZarrVolumeMeshActor blockActor;

    public OmeZarrBlockLoadRunner(OmeZarrBlockTileSource source, OmeZarrBlockTileKey key) {
        this.omeZarrBlockTileSource = source;
        this.omeZarrOctreeBlockTileKey = key;
    }

    @Override
    public void run() {
        loadFromBlockSource();
    }

    private void loadFromBlockSource() {
        long startTime = System.currentTimeMillis();
        LOG.debug("Load OmeZarr tile {}", omeZarrOctreeBlockTileKey);
        try {
            state = OmeZarrBlockLoadRunner.State.LOADING;
            OmeZarrVolumeActor parentActor = OmeZarrVolumeActor.getInstance();
            blockActor = new OmeZarrVolumeMeshActor(omeZarrBlockTileSource, omeZarrOctreeBlockTileKey, parentActor.getVolumeState(), 0);
            state = OmeZarrBlockLoadRunner.State.LOADED;
            setChanged();
            long endTime = System.currentTimeMillis();
            LOG.info("Loading OmeZarr tile {} took {} ms", omeZarrOctreeBlockTileKey, endTime - startTime);
            // notify listeners
            notifyObservers();
        } catch (IllegalStateException ex) {
            // these are 404 errors for files which are missing (possibly correctly, our octree
            //  isn't 100% complete) on disk
            LOG.warn("IllegalStateException loading OmeZarr tile {} from block source", omeZarrOctreeBlockTileKey, ex);
            state = OmeZarrBlockLoadRunner.State.FAILED;
        } catch (IOException ex) {
            LOG.warn("Exception loading OmeZarr tile {} from block source", omeZarrOctreeBlockTileKey, ex);
            state = OmeZarrBlockLoadRunner.State.FAILED;
        }
    }
}