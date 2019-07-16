package org.janelia.horta.blocks;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import org.apache.commons.lang3.StringUtils;
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
        try (InputStream blockStream = ktxBlockTileSource.streamKeyBlock(ktxOctreeBlockTileKey)) {
            URI sourceURI = ktxBlockTileSource.getKeyBlockRelativePathURI(ktxOctreeBlockTileKey);
            loadStream(ktxBlockTileSource.getDataServerURI().toString() + sourceURI.toString(), blockStream);
        } catch (IOException ex) {
            LOG.warn("IOException loading tile {} from block source", ktxOctreeBlockTileKey, ex);
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
            ktxData.loadStream(stream);
            if (ktxOctreeBlockTileKey == null) {
                blockDescription = ktxData.header.keyValueMetadata.get("octree_path");
            }
        } catch (IOException ex) {
            state = State.FAILED;
            LOG.warn("IOException loading tile {} from stream", blockDescription, ex);
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
