package org.janelia.it.FlyWorkstation.gui.viewer3d.loader;

import org.apache.juli.JdkLoggerFormatter;
import org.janelia.it.FlyWorkstation.gui.viewer3d.stream.V3dRawImageStream;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.MaskTextureDataBean;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.DataFormatException;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 2/6/13
 * Time: 3:33 PM
 *
 * Invoke this to build a mask-specific file from v3d file types.  Such files will contain "labels", rather than
 * color data, which are treated in transit-to-GPU as luminance values.
 */
public class V3dMaskFileLoader extends TextureDataBuilder implements VolumeFileLoaderI {
    public static final String COMPARTMENT_MASK_INDEX = "maskIndex";
    public static final String CONSOLIDATED_LABEL_MASK = "ConsolidatedLabel";

    private int[][][] maskVolume;
    private Logger logger = LoggerFactory.getLogger( V3dMaskFileLoader.class );

    @Override
    protected TextureDataI createTextureDataBean() {
        return new MaskTextureDataBean( maskByteArray, sx, sy, sz );
    }

    @Override
    public void loadVolumeFile( String fileName ) throws Exception {
        unCachedFileName = fileName;
        loadV3dMask(
                new BufferedInputStream(
                    new FileInputStream(unCachedFileName)
                )
        );
    }

    private void loadV3dMask(InputStream inputStream)
            throws IOException, DataFormatException {
        //isMask = true;

        V3dRawImageStream sliceStream = new V3dRawImageStream(inputStream);
        sx = sliceStream.getDimension(0);
        sy = sliceStream.getDimension(1);
        sz = sliceStream.getDimension(2);
        pixelBytes = sliceStream.getPixelBytes();
        int sc = sliceStream.getDimension(3);
        channelCount = sc;
        pixelByteOrder = sliceStream.getEndian();

        if ( sc > 1 ) {
            throw new RuntimeException( "Unexpected multi-channel mask file." );
        }

        if ( sc == 0 ) {
            throw new RuntimeException( "Unexpected zero channel count mask file." );
        }

        Set<Integer> values = null;
        long rawRequired = (long)(sx * sy * sz) * (long)pixelBytes;

        if ( rawRequired > Integer.MAX_VALUE ) {
            logger.info( "Downsampling {}.", unCachedFileName );
            values = readDownSampled(sliceStream);
        }
        else {
            values = readBytes(sliceStream);
        }

        for ( Integer value: values ) {
            System.out.print( value + "," );
        }
        System.out.println();
        header = sliceStream.getHeaderKey();
    }

    /**
     * This method will read in all bytes from the slice stream into a 3-D array. It will then "down-sample" those
     * using a frequency-of-occurence algorithm, into some fraction of the original size, of cells.
     *
     * @param sliceStream source of input data.
     * @return set of all distinct label values found in all cells.
     * @throws IOException thrown by called methods.
     */
    private Set<Integer> readDownSampled(V3dRawImageStream sliceStream) throws IOException {
        maskVolume = new int[ sx ][ sy ][ sz ];

        // Temporary values, subject to change, by use of metadata file accompanying linked downsample.
        double xScale = 2.0;
        double yScale = 2.0;
        double zScale = 2.0;
        int outSx = (int)Math.ceil( (double)sx / xScale );
        int outSy = (int)Math.ceil( (double)sy / yScale );
        int outSz = (int)Math.ceil( (double)sz / zScale );

        // Here, store all the values into a massive 3D array.  Dimensions very very unlikely
        // to exceed 16K.
        Set<Integer> values = new TreeSet<Integer>();
        for (int z = 0; z < sz; z ++ ) {
            sliceStream.loadNextSlice();
            V3dRawImageStream.Slice slice = sliceStream.getCurrentSlice();
            for (int y = 0; y < sy; y ++ ) {
                for (int x = 0; x < sx; x ++ ) {
                    Integer value = slice.getValue(x, y);
                    // NOTE: java zeros its arrays at allocation.  Therefore, can skip matrix-pos calculation.
                    if ( value > 0 ) {
                        maskVolume[x][y][z] = value;
                        values.add( value );
                    }
                }
            }
        }

        // Here, sample the neighborhoods (or _output_ voxels).
        // Java implicitly sets newly-allocated byte arrays to all zeros.
        maskByteArray = new byte[(outSx * outSy * outSz) * pixelBytes];

        int outZ = 0;
        for ( int z = 0; z < sz-zScale; z += zScale ) {
            int outY = 0;
            int zOffset = outZ * outSx * outSy;
            for ( int y = 0; y < sy-yScale; y += yScale ) {
                int yOffset = zOffset + (outSy-outY) * outSx; // zOffset + outY * outSx;
                int outX = 0;
                for ( int x = 0; x < sx-xScale; x += xScale ) {
                    java.util.Map<Integer,Integer> frequencies = new java.util.HashMap<Integer,Integer>();

                    int value = 0; // Arrive at our final value using the neighborhood comparisons.

                    // Neighborhood starts at the x,y,z values of the loops.  There will be one
                    // such neighborhood for each of these down-sampled coord sets: x,y,z
                    int maxFreq = 0;
                    for ( int zNbh = z; zNbh < z + zScale && zNbh < sz; zNbh ++ ) {

                        for ( int yNbh = y; yNbh < y + yScale && yNbh < sy; yNbh ++ ) {

                            for ( int xNbh = x; xNbh < x + xScale && xNbh < sx; xNbh++ ) {
                                int voxelVal = maskVolume[xNbh][yNbh][zNbh];
                                Integer freq = frequencies.get( voxelVal );
                                if ( freq == null ) {
                                    freq = 0;
                                }
                                frequencies.put(voxelVal, ++ freq );

                                if ( freq > maxFreq ) {
                                    maxFreq = freq;
                                    value = voxelVal;
                                }
                            }
                        }
                    }

                    // Store the value into the output array.
                    for ( int pi = 0; pi < pixelBytes; pi ++ ) {
                        byte piByte = (byte)(value >>> (pi * 8) & 0x000000ff);
                        maskByteArray[(yOffset * pixelBytes) + (outX * pixelBytes) + (pi)] = piByte;
                    }

                    outX ++;
                }

                outY ++;
            }

            outZ ++;
        }

        maskVolume = null; // Discard this and allow GC to take its course.

        // Post-adjust the x,y,z sizes to fit the target down-sampled array.
        sx = outSx;
        sy = outSy;
        sz = outSz;
        return values;
    }

    /**
     * This method reads all information from the slice stream into the internal mask-byte-array (1-D) without
     * attempting to subset or interpret the values.
     *
     * @param sliceStream source for data.
     * @return distinct set of all values found in the stream.
     * @throws IOException thrown by called methods.
     */
    private Set<Integer>  readBytes(V3dRawImageStream sliceStream) throws IOException {
        maskByteArray = new byte[(sx * sy * sz) * pixelBytes];

        // DEBUG INFO.
        //int tgtVal = 3;
        //Set<Integer[]> coordsForTgtValue = new java.util.HashSet<Integer[]>();

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
                            maskByteArray[(yOffset * pixelBytes) + (x * pixelBytes) + (pi)] = piByte;
                        }
                        // DEBUG BLOCK
                        //if ( value == tgtVal ) {
                        //    coordsForTgtValue.add( new Integer[] { x, y, z } );
                        //}
                        // END: DEBUG BLOCK
                    }
                }
            }
        }
        sliceStream.close();

        // DEBUG BLOCK
//        int maxX = 0;
//        int minX = Integer.MAX_VALUE;
//        int maxY = 0;
//        int minY = Integer.MAX_VALUE;
//        Integer[] maxXCoord = null;
//        Integer[] minXCoord = null;
//        for ( Integer[] coord: coordsForTgtValue ) {
//            if ( coord[0] > maxX ) {
//                maxX = coord[0];
//                maxXCoord = coord;
//            }
//            if ( coord[0] < minX ) {
//                minX = coord[0];
//                minXCoord = coord;
//            }
//
//            if ( coord[1] > maxY )
//                maxY = coord[1];
//            if ( coord[1] < minY )
//                minY = coord[1];
//        }
//        System.out.println("MinX for " + tgtVal + " is " + minX );
//        System.out.println("MaxX for " + tgtVal + " is " + maxX );
//        System.out.println("Size of X " + sx );
//        System.out.println("Maximum X's full coordinates: " + maxXCoord[0] + "," + maxXCoord[1] + "," + maxXCoord[2]);
//        System.out.println("Minimum X's full coordinates: " + minXCoord[0] + "," + minXCoord[1] + "," + minXCoord[2]);
//
//        System.out.println("MinY for " + tgtVal + " is " + minY );
//        System.out.println("MaxY for " + tgtVal + " is " + maxY );
//        System.out.println("Size of Y " + sy );

        // END: DEBUG BLOCK.

        return values;
    }

}
