package org.janelia.it.workstation.gui.alignment_board_viewer.volume_builder;

import org.janelia.it.workstation.gui.viewer3d.masking.VolumeDataI;
import org.janelia.it.workstation.gui.viewer3d.volume_builder.VolumeDataChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 9/22/13
 * Time: 11:11 PM
 *
 * This probably stop-gap impl will just split up the volume into multiple planes or slices.  The classic
 * single-voxel-width slice can be bundled into multiples.  This is very convenient for dealing with a segmentation
 * of the 1D version of the "array" into small enough sub-arrays.
 */
public class VeryLargeVolumeData implements VolumeDataI {

    public static final int DEFAULT_NUM_SLABS = 256;
    private int slabExtent = 0;
    private long volumeExtent = 0L;
    private final Logger logger = LoggerFactory.getLogger( VeryLargeVolumeData.class );
    private int sizeX;
    private int sizeY;

    private byte[][] slabs;
    private VolumeDataChunk[] chunks;
        
    public VeryLargeVolumeData( int sizeX, int sizeY, int sizeZ, int bytesPerVoxel ) {
        this( sizeX, sizeY, sizeZ, bytesPerVoxel, DEFAULT_NUM_SLABS, null );
    }

    /**
     * Construct with eno info to figure out how big the single-voxel-thick slice is, including voxel
     * byte multiple.  The slice size must not exceed Integer.MAX.
     *
     * @param sizeX ct x
     * @param sizeY ct y
     * @param sizeZ ct z
     * @param bytesPerVoxel how many bytes for each voxel.
     * @param numSlabs how many divisions of the original volume are wanted?
     */
    public VeryLargeVolumeData( int sizeX, int sizeY, int sizeZ, int bytesPerVoxel, int numSlabs, byte[] presetValue ) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;

        long strawSliceSize = sizeX * sizeY * bytesPerVoxel;
        if ( strawSliceSize > Integer.MAX_VALUE ) {
            throw new IllegalArgumentException( sizeX + " x " + sizeY + " too large to represent here." );
        }
        // Increase number of slabs, if the slice size is very large.
        int slicesPerSlab = calculateSlabParameters(sizeX, sizeY, sizeZ, bytesPerVoxel, numSlabs);
        if ( slabExtent > 100000000 ) {
            numSlabs *= 4;
            slicesPerSlab = calculateSlabParameters(sizeX, sizeY, sizeZ, bytesPerVoxel, numSlabs );
        }

        // Recalculate number of slabs, so that excess data is not allocated.
        numSlabs = (int)Math.ceil( (double)volumeExtent / (double)slabExtent );

        logger.debug("Slab extent is {}.", slabExtent );
        logger.debug("Slices per slab is {}.", slicesPerSlab);
        logger.debug("Number of slabs is {}.", numSlabs);
        logger.debug("Volume extent is {}.", volumeExtent);

        slabs = new byte[ numSlabs ][];
        chunks = new VolumeDataChunk[ numSlabs ];
        long slabEnd = 0L;
        int lastSlabIndex = numSlabs - 1;
        int slicesRemaining = sizeZ;
        // Here, make as many slabs as needed to contain the volume.
        for ( int slabIndex = 0; slabIndex < lastSlabIndex && slabEnd <= volumeExtent; slabIndex++ ) {
            slabs[ slabIndex ] = new byte[ slabExtent ];
            slabEnd += slabExtent;
            VolumeDataChunk chunk = getVolumeDataChunk( slicesPerSlab, slabIndex, slicesPerSlab );
            chunks[ slabIndex ] = chunk;
            presetSlabData(presetValue, slabIndex);
            slicesRemaining -= slicesPerSlab;
        }
        if ( slabEnd < volumeExtent ) {
            slabs[ lastSlabIndex ] = new byte[ (int)(volumeExtent - slabEnd) ];
            VolumeDataChunk chunk = getVolumeDataChunk( slicesPerSlab, lastSlabIndex, slicesRemaining );
            chunks[ lastSlabIndex ] = chunk;
            presetSlabData(presetValue, lastSlabIndex);
        }
        else {
            logger.error( "Invalid calculations: final slab would be empty." );
        }
    }

    private void presetSlabData(byte[] presetValue, int slabIndex) {
        // Perform many array copies to get the slabs preset with the chosen
        // value.
        if ( presetValue != null ) {
            for ( int i = 0; i < slabExtent; i += presetValue.length) {
                System.arraycopy(presetValue, 0, slabs[ slabIndex ], i, presetValue.length);
            }
        }
    }

    private int calculateSlabParameters(int sizeX, int sizeY, int sizeZ, int bytesPerVoxel, double numSlabs) {
        int slicesPerSlab = (int)Math.ceil( sizeZ / numSlabs);
        int sliceSize = sizeX * sizeY * bytesPerVoxel;
        long strawSlabExtent = sliceSize * slicesPerSlab;
        if ( strawSlabExtent > Integer.MAX_VALUE ) {
            throw new IllegalArgumentException( "Slab sizes would exceed max array size." );
        }
        slabExtent = sliceSize * slicesPerSlab;
        volumeExtent = (long)sliceSize * (long)sizeZ;
        return slicesPerSlab;
    }

    @Override
    public boolean isVolumeAvailable() {
        return true;
    }

    @Override
    public VolumeDataChunk[] getVolumeChunks() {
        return chunks;
    }

    @Override
    public byte getValueAt(long location) {
        int slabNo = getSlabNo( location );
        if (slabNo > slabs.length) {
            logger.warn("Slab no {} out of bounds, for location {}. Returning 0 for value.", slabNo, location);
            return 0;
        }
        byte[] slab = slabs[ slabNo ];
        int locInSlab = getLocInSlab(location);
        return slab[ locInSlab ];
    }

    @Override
    public void setValueAt(long location, byte value) {
        int slabNo = getSlabNo( location );
        byte[] slab = slabs[ slabNo ];
        slab[ getLocInSlab( location ) ] = value;
    }

    @Override
    public long length() {
        return volumeExtent;
    }

    /**
     * Helper for creating the final slabs to be cached, and returned later.
     *
     * @param slicesPerSlab for Z coord.
     * @param slabNumber which slab
     * @return fully-characterized volume chunk.
     */
    private VolumeDataChunk getVolumeDataChunk(long slicesPerSlab, int slabNumber, int depth) {
        VolumeDataChunk chunk = new VolumeDataChunk();
        chunk.setData( slabs[ slabNumber ] );
        chunk.setStartX( 0 );
        chunk.setStartY( 0 );
        chunk.setWidth( sizeX );
        chunk.setHeight( sizeY );
        chunk.setDepth( depth );
        chunk.setStartZ( (int)(slicesPerSlab * slabNumber) );
        return chunk;
    }

    private int getSlabNo(long location) {
        return (int)(location / slabExtent);
    }

    private int getLocInSlab(long location) {
        return (int)( location % slabExtent );
    }
}

