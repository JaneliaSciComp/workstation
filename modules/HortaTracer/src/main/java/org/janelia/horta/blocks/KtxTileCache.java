package org.janelia.horta.blocks;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import javax.media.opengl.GL3;
import org.janelia.horta.actors.SortableBlockActor;

/**
 *
 * @author brunsc
 */
public class KtxTileCache extends BasicTileCache<KtxOctreeBlockTileKey, SortableBlockActor> {

    private KtxOctreeBlockTileSource source;

    public KtxTileCache(KtxOctreeBlockTileSource source) {
        this.source = source;
    }

    public void setSource(KtxOctreeBlockTileSource source) {
        this.source = source;
    }

    @Override
    LoadRunner<KtxOctreeBlockTileKey, SortableBlockActor> getLoadRunner() {
        return new LoadRunner<KtxOctreeBlockTileKey, SortableBlockActor>() {
            @Override
            public SortableBlockActor loadTile(KtxOctreeBlockTileKey key) throws InterruptedException, IOException {
                final KtxBlockLoadRunner loader = new KtxBlockLoadRunner(source, key);
                loader.run();
                return loader.blockActor;
            }
        };
    }

    public void disposeObsoleteTiles(GL3 gl) {
        Collection<SortableBlockActor> obs = popObsoleteTiles();
        for (SortableBlockActor actor : obs) {
            actor.dispose(gl);
        }
    }

    public void disposeGL(GL3 gl) {
        disposeActorGroup(gl, nearVolumeInRam);
    }

    private void disposeActorGroup(GL3 gl, Map<KtxOctreeBlockTileKey, SortableBlockActor> group) {
        for (SortableBlockActor actor : group.values()) {
            actor.dispose(gl);
        }
        group.clear();
    }
}
