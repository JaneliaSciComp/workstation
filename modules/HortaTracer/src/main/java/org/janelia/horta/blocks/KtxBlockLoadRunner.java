package org.janelia.horta.blocks;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.function.Consumer;

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
        long startTime = System.currentTimeMillis();
        URI sourceURI = ktxBlockTileSource.getKeyBlockAbsolutePathURI(ktxOctreeBlockTileKey);
        LOG.debug("Load ktx tile {} from {}", ktxOctreeBlockTileKey, sourceURI);
        try (InputStream blockStream = ktxBlockTileSource.streamKeyBlock(ktxOctreeBlockTileKey).get()) {
            loadStream(blockStream, ktxData -> {
                long endTime = System.currentTimeMillis();
                LOG.info("Loading ktx tile {} from {} took {} ms", ktxOctreeBlockTileKey, sourceURI, endTime-startTime);
            });
        } catch (IOException ex) {
            LOG.warn("IOException loading tile {} from block source", ktxOctreeBlockTileKey, ex);
            state = State.FAILED;
        }
    }

    private void loadFromDataSource() {
        long startTime = System.currentTimeMillis();
        loadStream(ktxStreamDataSource.openInputStream(), ktxData -> {
            long endTime = System.currentTimeMillis();
            LOG.info("Loading ktx tile {} from datasource {} took {} ms", ktxOctreeBlockTileKey, ktxStreamDataSource.getFileName(), endTime-startTime);

        });
    }

    @Override
    public void run() {
        if (ktxStreamDataSource == null) {
            loadFromBlockSource();
        } else {
            loadFromDataSource();
        }
    }

    private void loadStream(InputStream stream, Consumer<KtxData> finishLoader) {
        if (stream == null) {
            // no ktx data is available
            return;
        }
        state = State.LOADING;
        KtxData ktxData = new KtxData();
        try {
            ktxData.loadStream(stream);
        } catch (IOException ex) {
            state = State.FAILED;
            LOG.warn("IOException loading tile {} from stream", ktxOctreeBlockTileKey, ex);
            return;
        } catch (InterruptedException ex) {
            LOG.info("loading tile {} was interrupted", ktxOctreeBlockTileKey);
            state = State.INTERRUPTED;
            return;
        }
        TetVolumeActor parentActor = TetVolumeActor.getInstance();
        blockActor = new TetVolumeMeshActor(ktxData, parentActor);
        state = State.LOADED;
        setChanged();
        finishLoader.accept(ktxData);
        // notify listeners
        notifyObservers();
    }

}
