package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.volume_builder;

import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.masking.VolumeDataI;

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

    public static final int DEFAULT_NUM_SLABS = 64;
    private int slabExtent = 0;
    private long volumeExtent = 0L;

    private byte[][] slabs;

    public VeryLargeVolumeData( int sizeX, int sizeY, int sizeZ, int bytesPerVoxel ) {
        this( sizeX, sizeY, sizeZ, bytesPerVoxel, DEFAULT_NUM_SLABS );
    }

    /**
     * Construct with eno info to figure out how bit the single-voxel-thick slice is, including voxel
     * byte multiple.  The slice size must not exceed Integer.MAX.
     *
     * @param sizeX ct x
     * @param sizeY ct y
     * @param sizeZ ct z
     * @param bytesPerVoxel how many bytes for each voxel.
     * @param numSlabs how many divisions of the original volume are wanted?
     */
    public VeryLargeVolumeData( int sizeX, int sizeY, int sizeZ, int bytesPerVoxel, int numSlabs ) {
        int sliceSize = sizeX * sizeY * bytesPerVoxel;
        volumeExtent = sliceSize * sizeZ;
        slabExtent = (int)((long)sliceSize * (long)sizeZ / (long) numSlabs);
        slabs = new byte[ numSlabs ][];
        long slabEnd = 0L;
        for ( int i = 0; i < numSlabs - 1; i++ ) {
            slabs[ i ] = new byte[ slabExtent ];
            slabEnd += slabExtent;
        }
        if ( slabEnd < volumeExtent ) {
            slabs[ numSlabs - 1 ] = new byte[ (int)(volumeExtent - slabEnd) ];
        }
    }

    @Override
    public boolean isVolumeAvailable() {
        return true;
    }

    @Override
    public byte[] getCurrentVolumeData() {
        throw new RuntimeException("Not implmented");
    }

    @Override
    public byte getValueAt(long location) {
        int slabNo = getSlabNo( location );
        byte[] slab = slabs[ slabNo ];
        return slab[ getLocInSlab( location, slabNo ) ];
    }

    @Override
    public void setValueAt(long location, byte value) {
        int slabNo = getSlabNo( location );
        byte[] slab = slabs[ slabNo ];
        slab[ getLocInSlab( location, slabNo )] = value;
    }

    @Override
    public long length() {
        return volumeExtent;
    }

    private int getSlabNo(long location) {
        return (int)(location / slabExtent);
    }

    private int getLocInSlab(long location, int slabNo) {
        return (int)(location - ( (long)slabNo * slabExtent ) );
    }
}
