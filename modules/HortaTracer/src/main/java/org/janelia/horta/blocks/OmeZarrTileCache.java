package org.janelia.horta.blocks;

import org.janelia.horta.actors.SortableBlockActor;

import javax.media.opengl.GL3;
import java.util.Collection;
import java.util.Map;

public class OmeZarrTileCache  extends BasicTileCache<OmeZarrBlockTileKey, SortableBlockActor> {
    private OmeZarrBlockTileSource source;
    private int colorChannel;

    public OmeZarrTileCache(OmeZarrBlockTileSource source, int colorChannel) {
        this.source = source;
        this.colorChannel = colorChannel;
    }

    public void setSource(OmeZarrBlockTileSource source) {
        this.source = source;
    }

    @Override
    LoadRunner<OmeZarrBlockTileKey, SortableBlockActor> getLoadRunner() {
        return key -> {
            final OmeZarrBlockLoadRunner loader = new OmeZarrBlockLoadRunner(source, key, colorChannel);
            loader.run();
            return loader.blockActor;
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

    private void disposeActorGroup(GL3 gl, Map<OmeZarrBlockTileKey, SortableBlockActor> group) {
        for (SortableBlockActor actor : group.values()) {
            actor.dispose(gl);
        }
        group.clear();
    }

    @Override
    protected String getTileName(OmeZarrBlockTileKey key) {
        return key.getRelativePath();
    }
}
