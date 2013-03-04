package org.janelia.it.FlyWorkstation.gui.viewer3d.loader;

import org.janelia.it.FlyWorkstation.gui.viewer3d.stream.V3dRawImageStream;

import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 3/4/13
 * Time: 2:17 PM
 *
 * Convenience class to delegate a common operation by two different types of V3d file loaders.
 */
public class V3dByteReader {

    private byte[] textureByteArray;

    public byte[] getTextureBytes() {
        return textureByteArray;
    }

    /**
     * This method reads all information from the slice stream into the internal mask-byte-array (1-D) without
     * attempting to subset or interpret the values.
     *
     * @param sliceStream source for data.
     * @return distinct set of all values found in the stream.
     * @throws java.io.IOException thrown by called methods.
     */
    public Set<Integer> readBytes(V3dRawImageStream sliceStream, int sx, int sy, int sz, int pixelBytes)
            throws IOException {
        textureByteArray = new byte[(sx * sy * sz) * pixelBytes];

        Set<Integer> values = new TreeSet<Integer>();
        for (int z = 0; z < sz; z ++ ) {
            int zOffset = z * sx * sy;
            sliceStream.loadNextSlice();
            V3dRawImageStream.Slice slice = sliceStream.getCurrentSlice();
            for (int y = 0; y < sy; y ++ ) {
                int yOffset = zOffset + (sy-y) * sx;
                for (int x = 0; x < sx; x ++ ) {
                    Integer value = slice.getValue(x, y);
                    if ( value > 0 ) {
                        values.add( value );
                        for ( int pi = 0; pi < pixelBytes; pi ++ ) {
                            byte piByte = (byte)(value >>> (pi * 8) & 0x000000ff);
                            textureByteArray[(yOffset * pixelBytes) + (x * pixelBytes) + (pi)] = piByte;
                        }
                    }
                }
            }
        }
        sliceStream.close();

        return values;
    }


}
