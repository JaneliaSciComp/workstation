/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package janelia.lvv.tileloader;

import java.io.IOException;
import java.net.URL;
import java.util.List;

/**
 *
 * @author Christopher Bruns
 */
public interface BrickSliceLoader
{
    // Single slice retrieval API
    SliceBytes loadSlice(URL brickSource, int sliceNumber) throws IOException;

    // Burst slice retrieval. Backwards order is permitted (i.e. beginSlice larger than endSlice)
    SliceBytes[] loadSliceRange(URL brickSource, List<Integer> sliceIndices) throws IOException;
}
