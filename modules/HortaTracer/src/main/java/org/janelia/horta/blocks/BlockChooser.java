package org.janelia.horta.blocks;

import java.util.List;
import org.janelia.geometry3d.ConstVector3;
import org.janelia.geometry3d.Vantage;

/**
 * Create a list of volume tiles, in order of decreasing importance
 * @author brunsc
 */
public interface BlockChooser<K extends BlockTileKey, S extends BlockTileSource<K>> {
    List<K> chooseBlocks(S source, ConstVector3 focus, ConstVector3 previousFocus, Vantage vantage);
}
