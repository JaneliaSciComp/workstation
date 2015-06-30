/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package janelia.lvv.tileloader;

import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;

/**
 *
 * @author Christopher Bruns
 */
public interface BrickSliceLoader
{
    ByteBuffer loadSlice(URL brickSource, int sliceNumber) throws IOException;
}

