package org.janelia.workstation.img_3d_loader;


import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;
import org.janelia.workstation.image.stream.V3dRawImageStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 3/4/13
 * Time: 2:17 PM
 *
 * Convenience class to delegate a common operation by two different types of V3d file loaders.
 */
public class V3dByteReader {
    
    Logger logger = LoggerFactory.getLogger(V3dByteReader.class);

    private byte[] textureByteArray;
    private boolean invertedY = true;

    public byte[] getTextureBytes() {
        return textureByteArray;
    }

    /**
     * This method reads all information from the slice stream into the internal mask-byte-array (1-D) without
     * attempting to subset or interpret the values.
     *
     * @param sliceStream source for data.
     * @return distinct set of all values found in the stream.
     * @throws IOException thrown by called methods.
     */
    public Set<Integer> readBytes(V3dRawImageStream sliceStream, int sx, int sy, int sz, int sc, int pixelBytes)
            throws IOException {
        textureByteArray = new byte[(sx * sy * sz * sc) * pixelBytes];

        logger.info("readBytes 1 start");

        Set<Integer> values = new TreeSet<>();
        for (int c = 0; c < sc; c++) {
            int cOffset = c * sx * sy * sz * pixelBytes;
            for (int z = 0; z < sz; z++) {
                int zOffset = z * sx * sy;
                sliceStream.loadNextSlice();
                V3dRawImageStream.Slice slice = sliceStream.getCurrentSlice();
                for (int y = 0; y < sy; y++) {
                    int yOffset = zOffset + calcYOffset(y, sy) * sx;
                    for (int x = 0; x < sx; x++) {
                        Integer value = slice.getValue(x, y);
                        if (value > 0) {
                            values.add(value);
                            for (int pi = 0; pi < pixelBytes; pi++) {
                                byte piByte = (byte) (value >>> (pi * 8) & 0x000000ff);
                                textureByteArray[cOffset + (yOffset * pixelBytes) + (x * pixelBytes) + (pi)] = piByte;
                            }
                        }
                    }
                }
            }
        }
        sliceStream.close();

        logger.info("readBytes 1 end");

        return values;
    }

    /**
     * This method reads all information from the slice stream into the internal mask-byte-array (1-D).
     *
     * @param sliceStream source for data.
     * @return distinct set of all values found in the stream.
     * @throws IOException thrown by called methods.
     */
    public Set<Integer> readBytes(V3dRawImageStream sliceStream, int sx, int sy, int sz)
            throws IOException {
        
        logger.info("readBytes 2 start");
        
        textureByteArray = new byte[(sx * sy * sz)];

        Set<Integer> values = new TreeSet<>();
        for (int z = 0; z < sz; z ++ ) {
            int zOffset = z * sx * sy;
            sliceStream.loadNextSlice();
            V3dRawImageStream.Slice slice = sliceStream.getCurrentSlice();
            for (int y = 0; y < sy; y ++ ) {
                int yOffset = zOffset + calcYOffset(y, sy) * sx;
                for (int x = 0; x < sx; x ++ ) {
                    Integer value = slice.getValue(x, y);
                    if (value < 0) {
                        value = 256 + value;
                    }
                    
                    // DEBUG
                    if (z<10 && y<10 && x<10) {
                        value=255;
                    }
                    
                    
                    values.add(value);
                    textureByteArray[(yOffset) + x] = value.byteValue();
                }
            }
        }
        sliceStream.close();
        
        logger.info("readBytes 2 end");

        return values;
    }

    /**
     * This method reads all information from the slice stream into the internal mask-byte-array (1-D). Makes
     * triples of all bytes, to simulate RGB + Alpha.
     *
     * @param sliceStream source for data.
     * @return distinct set of all values found in the stream.
     * @throws IOException thrown by called methods.
     */
    public Set<Integer> readBytesToInts(V3dRawImageStream sliceStream, int sx, int sy, int sz)
            throws IOException {
        
        logger.info("readBytes 3 start");


        int pixelBytes = 4;
        textureByteArray = new byte[(sx * sy * sz) * pixelBytes];
        Set<Integer> values = new TreeSet<>();
        for (int z = 0; z < sz; z ++ ) {
            int zOffset = z * sx * sy;
            sliceStream.loadNextSlice();
            V3dRawImageStream.Slice slice = sliceStream.getCurrentSlice();
            for (int y = 0; y < sy; y ++ ) {
                int yOffset = zOffset + calcYOffset(y, sy) * sx;
                for (int x = 0; x < sx; x ++ ) {
                    Integer value = slice.getValue(x, y);
                    if ( value > 0 ) {
                        values.add( value );
                        int intOffset = (yOffset * pixelBytes) + (x * pixelBytes);
                        for ( int pi = 0; pi < pixelBytes - 1; pi++ ) {
                            byte b = value.byteValue();
                            textureByteArray[intOffset + pi] = b;
                        }
                        textureByteArray[(intOffset + 3)] = (byte)255;
                    }
                }
            }
        }
        sliceStream.close();
        
        logger.info("readBytes 3 end");


        return values;
    }

    public void setInvertedY(boolean invertedY) {
        this.invertedY = invertedY;
    }

    private int calcYOffset( int y, int sy ) {
        return invertedY ? (sy-y-1) : y; // SDM - added the -1
    }
}
