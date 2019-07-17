package org.janelia.horta.blocks;

import java.util.ArrayList;
import java.util.List;
import org.janelia.geometry3d.ConstVector3;
import org.janelia.geometry3d.Vantage;

/**
 * Generate sorted list of up to eight max resolution blocks near current focus
 *
 * @author brunsc
 */
public class OneFineDisplayBlockChooser<K extends BlockTileKey, S extends BlockTileSource<K>> implements BlockChooser<K, S> {

    /*
     Choose the eight closest maximum resolution blocks to the current focus point.
     */
    @Override
    public List<K> chooseBlocks(S source, ConstVector3 focus, ConstVector3 previousFocus,
                                Vantage vantage) {
        // Find up to eight closest blocks adjacent to focus
        K centerBlock = source.getBlockKeyAt(focus, source.getMaximumResolution());
        List<K> result = new ArrayList<>();
        result.add(centerBlock);
        return result;
    }
}
