package org.janelia.it.FlyWorkstation.gui.viewer3d.masking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 3/25/13
 * Time: 11:40 PM
 *
 * Will allow volumes to be down-sampled in a greatest-frequency fashion.
 */
public class DownSampler {

    private Logger logger = LoggerFactory.getLogger( DownSampler.class );
    private long sx;
    private long sy;
    private long sz;

    public DownSampler( long sx, long sy, long sz ) {
        this.sx = sx;
        this.sy = sy;
        this.sz = sz;
    }

    /**
     * This allows us to carry out a kind of lossy compression of a 3D volume, to get it into a manageable
     * size for use on the GPU hardware.
     *
     * @param oneDVolume an array of bytes, suitable for upload to GPU, but too large.
     * @param voxelBytes number of bytes in each voxel.
     * @param xScale multiplier in x direction.
     * @param yScale multiplier in y direction.
     * @param zScale multiplier in z direction.
     * @return an array of bytes, suitable ofr uplaod to GPU, but with some loss to reduce size.
     */
    protected DownsampledTextureData getDownSampledVolume(
            byte[] oneDVolume, int voxelBytes, double xScale, double yScale, double zScale
    ) {
        DownsampledTextureData rtnVal = getDownSampledVolumeHelper(
                oneDVolume, //create3DVolume( oneDVolume, voxelBytes ),
                voxelBytes,
                xScale,
                yScale,
                zScale
        );

        return rtnVal;
    }

    public static class DownsampledTextureData {
        private byte[] volume;
        private int sx;
        private int sy;
        private int sz;
        private int voxelBytes;

        private double xScale;
        private double yScale;
        private double zScale;

        public DownsampledTextureData(
                byte[] textureBytes, int sx, int sy, int sz, int voxelBytes,
                double xScale, double yScale, double zScale
        ) {
            this.volume = textureBytes;
            this.voxelBytes = voxelBytes;
            this.sx = sx;
            this.sy = sy;
            this.sz = sz;
            this.xScale = xScale;
            this.yScale = yScale;
            this.zScale = zScale;
        }

        public byte[] getVolume() {
            return volume;
        }

        public int getSx() {
            return sx;
        }

        public int getSy() {
            return sy;
        }

        public int getSz() {
            return sz;
        }

        public int getVoxelBytes() {
            return voxelBytes;
        }

        public double getxScale() {
            return xScale;
        }

        public double getyScale() {
            return yScale;
        }

        public double getzScale() {
            return zScale;
        }
    }

    /**
     * This method will "down-sample" the 3D (times pixel bytes) volume to a manageable size
     * using a frequency-of-occurence algorithm, into some fraction of the original size, of cells.
     *
     * @return set of all distinct label values found in all cells.
     */
    private DownsampledTextureData getDownSampledVolumeHelper(
            byte[] fullSizeVolume, int voxelBytes,
            double xScale,
            double yScale,
            double zScale
    ) {

        int outSx = (int)Math.ceil((double) sx / xScale);
        int outSy = (int)Math.ceil((double) sy / yScale);
        int outSz = (int)Math.ceil((double) sz / zScale);

        logger.info( "Downsampling to " + outSx + " by " + outSy + " by " + outSz + " for x,y,z sizes="
                + sx + "," + sy + "," + sz );

        // Here, sample the neighborhoods (or _output_ voxels).
        // Java implicitly sets newly-allocated byte arrays to all zeros.
        byte[] textureByteArray = new byte[(outSx * outSy * outSz) * voxelBytes];

        int outZ = 0;
        for ( int z = 0; z < sz-zScale && outZ < outSz; z += zScale ) {
            int outY = 0;
            int zOffset = outZ * outSx * outSy * voxelBytes;
            for ( int y = 0; y < sy-yScale && outY < outSy; y += yScale ) {
                int yOffset = zOffset + outY * outSx * voxelBytes; //(outSy-outY) * outSx ;
                int outX = 0;
                for ( int x = 0; x < sx-xScale && outX < outSx; x += xScale ) {
                    byte[] value = getNeighborHoodDownSampling(
                            fullSizeVolume, voxelBytes, xScale, yScale, zScale, x, y, z
                    );

                    // Store the value into the output array.
                    if ( value != null ) {
                        for ( int pi = 0; pi < voxelBytes; pi ++ ) {
                            //byte piByte = (byte)(value >>> (pi * 8) & 0x000000ff);
                            byte piByte = value[ pi ];
                            textureByteArray[yOffset + (outX * voxelBytes) + (pi)] = piByte;
                        }
                    }

                    outX ++;
                }

                outY ++;
            }

            outZ ++;
        }

        // Post-adjust the x,y,z sizes to fit the target down-sampled array.
        sx = outSx;
        sy = outSy;
        sz = outSz;

        DownsampledTextureData rtnVal = new DownsampledTextureData(
                textureByteArray, outSx, outSy, outSz, voxelBytes, xScale, yScale, zScale
        );

        return rtnVal;
    }

    /**
     * Computes the most-frequently-encountered-value among the "neighborhood" of adjacent voxel values, to
     * the target downsampled voxel.
     *
     * @param fullSizeVolume the original non-downsampled full volume.
     * @param voxelBytes number of bytes per voxel.  All dimensions must be multiplied by this exactly once.
     * @param xScale this is the downsampling rate for x
     * @param yScale this is the downsampling rate for y
     * @param zScale this is the downsampling rate for z
     * @param z input location under study.
     * @param y input location under study.
     * @param x input location under study.
     * @return computed value: all bytes of the voxel.
     */
    private byte[] getNeighborHoodDownSampling(
            byte[] fullSizeVolume, int voxelBytes, double xScale, double yScale, double zScale, int x, int y, int z
    ) {

        byte[] value = null;

        java.util.Map<Long,Integer> frequencies =
                new java.util.HashMap<Long,Integer>();

        // Neighborhood starts at the x,y,z values of the loops.  There will be one
        // such neighborhood for each of these down-sampled coord sets: x,y,z
        int maxFreq = 0;
        for ( int zNbh = z; zNbh < z + zScale && zNbh < sz; zNbh ++ ) {
            int nbhZOffset = (int)(sy * sx * zNbh) * voxelBytes;

            for ( int yNbh = y; yNbh < y + yScale && yNbh < sy; yNbh ++ ) {
                int nbhYOffset = (int)(nbhZOffset + (sx * yNbh * voxelBytes ) );

                for ( int xNbh = x; xNbh < x + xScale && xNbh < sx; xNbh++ ) {
                    byte[] voxelVal = new byte[ voxelBytes ];
                    int arrayCopyLoc = nbhYOffset + (xNbh * voxelBytes);
                    try {
                    System.arraycopy(
                            fullSizeVolume, arrayCopyLoc, voxelVal, 0, voxelBytes
                    );
                    } catch ( Exception ex ) {
                        logger.error(
                                "Exception while trying to copy to {} with max of {}.",
                                arrayCopyLoc,
                                fullSizeVolume.length
                        );
                        logger.info( "Expected dimensions are " + sx + " x " + sy + " x " + sz );
                        ex.printStackTrace();
                        throw new RuntimeException(ex);
                    }

                    if ( isZero( voxelVal ) ) {
                        continue;  // Highest freq non-zero is kept.
                    }
                    Long key = getIndex( voxelVal );
                    Integer freq = frequencies.get( key );
                    if ( freq == null ) {
                        freq = 0;
                    }
                    frequencies.put( key, ++ freq );

                    if ( freq > maxFreq ) {
                        maxFreq = freq;
                        value = voxelVal;
                    }
                }
            }
        }
        return value;
    }

    private boolean isZero( byte[] bytes ) {
        for ( int i = 0; i < bytes.length; i++ ) {
            if ( bytes[ i ] != (byte) 0 ) {
                return false;
            }
        }
        return true;
    }

    /**
     * Convert the voxel value into an integer.  Assumes 4 or fewer bytes per voxel.
     *
     * @param voxelValue input bytes
     * @return integer conversion, based on LSB
     */
    private long getIndex( byte[] voxelValue ) {
        long rtnVal = 0;
        int arrLen = voxelValue.length;
        for ( int i = 0; i < arrLen; i++ ) {
            //finalVal += startingArray[ j ] << (8 * (arrLen - j - 1));
            rtnVal += (voxelValue[ i ] << (8 * (arrLen - i - 1)));
        }
        return rtnVal;
    }

}
