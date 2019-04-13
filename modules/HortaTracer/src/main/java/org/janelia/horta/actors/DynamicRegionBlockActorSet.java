package org.janelia.horta.actors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.media.opengl.GL3;
import org.janelia.geometry3d.Vector3;
import org.janelia.gltools.GL3Resource;
import org.janelia.horta.blocks.BlockChooser;
import org.janelia.horta.blocks.BlockTileKey;
import org.janelia.horta.blocks.BlockTileSource;

/**
 *
 * @author brunsc
 */
public class DynamicRegionBlockActorSet<K extends BlockTileKey>
        implements SortableBlockActorSource, // designed to be contained within a TetVolumeActor
                   GL3Resource {

    private final BlockTileSource<K> tileSource;
    private final BlockChooser<K, BlockTileSource<K>> tileChooser;
    private final Map<K, SortableBlockActor> blockActors = new HashMap<>();
    private Collection<SortableBlockActor> obsoleteActors = new ArrayList<>();

    public DynamicRegionBlockActorSet(BlockTileSource<K> tileSource, BlockChooser<K, BlockTileSource<K>> tileChooser) {
        this.tileSource = tileSource;
        this.tileChooser = tileChooser;
    }

    public synchronized void updateActors(Vector3 focus, Vector3 previousFocus) {
        List<K> desiredBlocks = tileChooser.chooseBlocks(tileSource, focus, previousFocus);
        List<K> newBlocks = new ArrayList<>();
        Set<K> desiredSet = new HashSet<>();
        boolean bChanged = false;
        for (K key : desiredBlocks) {
            desiredSet.add(key);
            if (!blockActors.containsKey(key)) {
                newBlocks.add(key);
                bChanged = true;
            }
        }
        for (BlockTileKey key : blockActors.keySet()) {
            if (!desiredSet.contains(key)) {
                SortableBlockActor actor = blockActors.remove(key);
                obsoleteActors.add(actor);
                bChanged = true;
            }
        }
        // TODO: - load more blocks from source
    }

    @Override
    public Collection<SortableBlockActor> getSortableBlockActors() {
        return blockActors.values();
    }

    @Override
    public void dispose(GL3 gl) {
        for (SortableBlockActor actor : blockActors.values()) {
            actor.dispose(gl);
        }
        blockActors.clear();
        for (SortableBlockActor actor : obsoleteActors) {
            actor.dispose(gl);
        }
        obsoleteActors.clear();
    }

    @Override
    public void init(GL3 gl) {
    }
}
