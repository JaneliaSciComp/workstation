package org.janelia.horta.actors;

import org.janelia.geometry3d.CentroidHaver;
import org.janelia.gltools.GL3Actor;
import org.janelia.horta.blocks.BlockTileResolution;

/**
 *
 * @author brunsc
 */
public interface SortableBlockActor extends CentroidHaver, GL3Actor {
    BlockTileResolution getResolution();
}
