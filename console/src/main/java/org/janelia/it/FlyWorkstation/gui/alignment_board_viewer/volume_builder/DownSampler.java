package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.volume_builder;

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.shared.loader.volume.VolumeDataI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 3/25/13
 * Time: 11:40 PM
 *
 * Will allow volumes to be down-sampled in a greatest-frequency fashion.
 */
public class DownSampler {

    private static final int DOWNSAMPLE_THREAD_COUNT = 5;
    private final Logger logger = LoggerFactory.getLogger( DownSampler.class );
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
     * @param fullSizeVolume an array of bytes, suitable for upload to GPU, but too large.
     * @param voxelBytes number of bytes in each voxel.
     * @param xScale multiplier in x direction.
     * @param yScale multiplier in y direction.
     * @param zScale multiplier in z direction.
     * @return an array of bytes, suitable ofr uplaod to GPU, but with some loss to reduce size.
     */
    protected DownsampledTextureData getDownSampledVolume(
            VolumeDataI fullSizeVolume, int voxelBytes, double xScale, double yScale, double zScale
    ) {
        DownsampledTextureData rtnVal;
        if ( xScale == yScale  &&  yScale == zScale  &&  zScale == 1.0 ) {
            // Trivial case.
            rtnVal = new DownsampledTextureData(
                    fullSizeVolume,
                    (int)sx, (int)sy, (int)sz
            );
        }
        else {
            rtnVal = getDownSampledVolumeHelper(
                    fullSizeVolume,
                    voxelBytes,
                    xScale,
                    yScale,
                    zScale
            );
        }

        return rtnVal;
    }

    /**
     * This method will "down-sample" the 3D (times pixel bytes) volume to a manageable size
     * using a frequency-of-occurence algorithm, into some fraction of the original size, of cells.
     *
     * @return set of all distinct label values found in all cells.
     */
    private DownsampledTextureData getDownSampledVolumeHelper(
            VolumeDataI fullSizeVolume, int voxelBytes,
            double xScale,
            double yScale,
            double zScale
    ) {

        int outSx = (int)Math.ceil((double) sx / xScale);
        int outSy = (int)Math.ceil((double) sy / yScale);
        int outSz = (int)Math.ceil((double) sz / zScale);

        logger.debug( "Downsampling to " + outSx + " by " + outSy + " by " + outSz + " for x,y,z sizes="
                + sx + "," + sy + "," + sz );

        // Here, sample the neighborhoods (or _output_ voxels).
        // Java implicitly sets newly-allocated byte arrays to all zeros.
        //VolumeDataI downsampledVolume = new VolumeDataBean( (outSx * outSy * outSz) * voxelBytes, outSx, outSy, outSz );
        VolumeDataI downsampledVolume = new VeryLargeVolumeData( outSx, outSy, outSz, voxelBytes );
        final DownsampleParameter downsampleBean = new DownsampleParameter(fullSizeVolume, voxelBytes, xScale, yScale, zScale, outSx, outSy, downsampledVolume);

        // Making a thread pool, and submitting slice-downsampling steps to that pool.
        ExecutorService downSamplingThreadPool = Executors.newFixedThreadPool( DOWNSAMPLE_THREAD_COUNT );

        int outZ = 0;
        for ( int z = 0; z < sz-zScale && outZ < outSz; z += zScale ) {
            int zOffset = outZ * outSx * outSy * voxelBytes;
            final SliceDownsampleParamBean sliceBean = new SliceDownsampleParamBean( z, zOffset );

            SimpleWorker simpleWorker = new SimpleWorker() {
                @Override
                protected void doStuff() throws Exception {
                    getDownsampledSlice( downsampleBean, sliceBean );
                }

                @Override
                protected void hadSuccess() {
                }

                @Override
                protected void hadError(Throwable error) {
                    SessionMgr.getSessionMgr().handleException( error );
                }
            };
            downSamplingThreadPool.execute( simpleWorker );

            outZ ++;
        }

        // Post a shutdown, and wait for all requests to terminate.
        downSamplingThreadPool.shutdown();
        awaitThreadpoolCompletion( downSamplingThreadPool );

        // Post-adjust the x,y,z sizes to fit the target down-sampled array.
        sx = outSx;
        sy = outSy;
        sz = outSz;

        DownsampledTextureData rtnVal = new DownsampledTextureData(
                downsampledVolume, outSx, outSy, outSz
        );
        logger.debug("Downsampling complete.");

        return rtnVal;
    }

    /** Wait until the threadpool has completed all processing. */
    private void awaitThreadpoolCompletion(ExecutorService threadPool) {
        try {
            // Now that the pools is laden, we call the milder shutdown, which lets us wait for completion of all.
            logger.debug("Awaiting shutdown.");
            threadPool.shutdown();
            threadPool.awaitTermination( 10, TimeUnit.MINUTES );
            logger.debug("Thread pool termination complete.");
        } catch ( InterruptedException ie ) {
            ie.printStackTrace();
            SessionMgr.getSessionMgr().handleException( ie );
        }
    }

    private void getDownsampledSlice(DownsampleParameter downSampleParam, SliceDownsampleParamBean sliceParam ) {

        for ( int y = 0; y < sy- downSampleParam.getYScale() && sliceParam.getOutY() < downSampleParam.getOutSy(); y += downSampleParam.getYScale()) {
            int yOffset = sliceParam.getZOffset() + sliceParam.getOutY() * downSampleParam.getOutSx() * downSampleParam.getVoxelBytes(); //(outSy-outY) * outSx ;
            int outX = 0;
            for ( int x = 0; x < sx- downSampleParam.getXScale() && outX < downSampleParam.getOutSx(); x += downSampleParam.getXScale()) {
                byte[] value = getNeighborHoodDownSampling( downSampleParam, x, y, sliceParam.getZ() );

                // Store the value into the output array.
                if ( value != null ) {
                    for ( int pi = 0; pi < downSampleParam.getVoxelBytes(); pi ++ ) {
                        //byte piByte = (byte)(value >>> (pi * 8) & 0x000000ff);
                        byte piByte = value[ pi ];
                        int location = yOffset + (outX * downSampleParam.getVoxelBytes()) + (pi);
                        downSampleParam.getDownsampledVolume().setValueAt(location, piByte);
                    }
                }

                outX ++;
            }

            sliceParam.setOutY(sliceParam.getOutY() + 1);
        }
    }

    /**
     * Computes the most-frequently-encountered-value among the "neighborhood" of adjacent voxel values, to
     * the target downsampled voxel.
     *
     * @param sliceParameter metadata about the slice being calculated.
     * @param y input location under study.
     * @param x input location under study.
     * @return computed value: all bytes of the voxel.
     */
    private byte[] getNeighborHoodDownSampling(
            DownsampleParameter sliceParameter, int x, int y, int z
    ) {

        byte[] value = null;

        java.util.Map<Long,Integer> frequencies =
                new java.util.HashMap<Long,Integer>();

        // Neighborhood starts at the x,y,z values of the loops.  There will be one
        // such neighborhood for each of these down-sampled coord sets: x,y,z
        int maxFreq = 0;
        for ( int zNbh = z; zNbh < z + sliceParameter.getZScale() && zNbh < sz; zNbh ++ ) {
            long nbhZOffset = (sy * sx * zNbh) * sliceParameter.getVoxelBytes();

            for ( int yNbh = y; yNbh < y + sliceParameter.getYScale() && yNbh < sy; yNbh ++ ) {
                long nbhYOffset = nbhZOffset + (sx * yNbh * sliceParameter.getVoxelBytes() );

                for ( int xNbh = x; xNbh < x + sliceParameter.getXScale() && xNbh < sx; xNbh++ ) {
                    byte[] voxelVal = new byte[ sliceParameter.getVoxelBytes() ];
                    long arrayCopyLoc = nbhYOffset + (xNbh * sliceParameter.getVoxelBytes());
                    try {
                        VolumeDataI fullSizeVolume = sliceParameter.getFullSizeVolume();
                        for ( int i = 0; i < (sliceParameter.getVoxelBytes() ); i++ ) {
                            voxelVal[ i ] = fullSizeVolume.getValueAt(i + arrayCopyLoc);
                        }
                    } catch ( Exception ex ) {
                        logger.error(
                                "Exception while trying to copy to {} with max of {}.",
                                arrayCopyLoc,
                                sliceParameter.getFullSizeVolume().length()
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
        for (byte aByte : bytes) {
            if (aByte != (byte) 0) {
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

    /**
     * Parameter object to facilitate downsample method calls.
     */
    private static class SliceDownsampleParamBean {
        private int outY = 0;
        private final int z;
        private final int zOffset;

        public SliceDownsampleParamBean( int z, int zOffset ) {
            this.z = z;
            this.zOffset = zOffset;
        }

        public int getOutY() {
            return outY;
        }

        public void setOutY( int outY ) {
            this.outY = outY;
        }

        public int getZ() {
            return z;
        }

        public int getZOffset() {
            return zOffset;
        }
    }

    /**
     * Return-value object to conveniently-collect various values about downsample output.
     */
    public static class DownsampledTextureData {
        private final VolumeDataI volume;
        private final int sx;
        private final int sy;
        private final int sz;

        public DownsampledTextureData(
                VolumeDataI textureBytes, int sx, int sy, int sz
        ) {
            this.volume = textureBytes;
            this.sx = sx;
            this.sy = sy;
            this.sz = sz;
        }

        public VolumeDataI getVolume() {
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

    }

}
