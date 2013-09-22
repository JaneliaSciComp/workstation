package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.volume_builder;

import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.masking.VolumeDataI;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 9/20/13
 * Time: 3:21 PM
 *
 * Capable of breaking up the volume into smaller parts for management purposes (performance, etc.).
 */
public class PartitionedVolumeData implements VolumeDataI {

    private VolumeChunk[][][] cachedVolumeChunks;
    private PartitionParameters partitionParameters;

    /**
     * Sets up partition parameters for the simplified scenario: all same partition dimensions.
     *
     * @param dimX how long on X,
     * @param dimY on Y,
     * @param dimZ on Z
     * @param cubeDim dims of all sides of sub-cubes.
     * @return a volume data that satisfies those conditions.
     */
    public static PartitionedVolumeData createCubedPartitions(
            long dimX, long dimY, long dimZ, int cubeDim, int bytesPerAtom
    ) {
        PartitionParameters params = new PartitionParameters();
        params.setDimX( dimX );
        params.setDimY(dimY);
        params.setDimZ(dimZ);
        params.setPartitionDimX(cubeDim);
        params.setPartitionDimY(cubeDim);
        params.setPartitionDimZ(cubeDim);
        params.setBytesPerAtom(bytesPerAtom);
        params.setTotalSize( dimX * dimY * dimZ * bytesPerAtom );

        return new PartitionedVolumeData( params );
    }

    /**
     * Convenience override, in case an array of dimensions is available in int.
     * @see #createCubedPartitions(long, long, long, int)
     */
    public static PartitionedVolumeData createCubedPartitions(
            int[] dimensions, int cubeDim, int bytesPerAtom
    ) {
        return createCubedPartitions( dimensions[ 0 ], dimensions[ 1 ], dimensions[ 2 ], cubeDim, bytesPerAtom );
    }

    /**
     * Construct with params object.  This offers flexibility over the "factory" method
     * @see #createCubedPartitions(long, long, long, int)
     * @param params fill this in with all its setters.
     */
    public PartitionedVolumeData( PartitionParameters params ) {
        partitionParameters = params;

        initializePartitions();
    }

    public int[] getAxialDimensions() {
        return new int[] { partitionParameters.getPartitionDimX(), partitionParameters.getPartitionDimY(), partitionParameters.getPartitionDimZ() };
    }

    @Override
    public boolean isVolumeAvailable() {
        return true;
    }

    /** This violates the assumption that we can have volumes greater than Max-Int in linear dimension. */
    @Deprecated
    @Override
    public byte[] getCurrentVolumeData() {
        throw new IllegalStateException("This method shall not be called on this implementation.");
    }

    @Override
    public byte getValueAt(long location) {
        VolumeChunk volumeChunk = findChunk( location );
        int locationInChunk = getLocationInChunk(location, volumeChunk);

        return volumeChunk.getVolumeData()[ locationInChunk ];
    }

    @Override
    public void setValueAt(long location, byte value) {
        VolumeChunk volumeChunk = findChunk( location );
        int locationInChunk = getLocationInChunk(location, volumeChunk);
        try {
            volumeChunk.getVolumeData()[ locationInChunk ] = value;
        } catch ( Exception ex ) {
            System.err.println("Location in chunk = " +
                    getLocationInChunk( location, volumeChunk )
                    + " for chunk=" + volumeChunk.getChunksLocation()[0]+","+volumeChunk.getChunksLocation()[1]+","+volumeChunk.getChunksLocation()[2]+
            " and for raw location=" + location);
            ex.printStackTrace();
        }
    }

    @Override
    public long length() {
        return partitionParameters.getTotalSize();
    }

    /** Get the dump of everything. */
    public VolumeChunk[][][] getCachedVolumeChunks() {
        return cachedVolumeChunks;
    }

    //todo use 3D location to get the location in chunk.
    private int getLocationInChunk(long location, VolumeChunk volumeChunk) {
        return (int)(location - volumeChunk.getStart1D());
    }

    /**
     * Return the chunk, from the cache of chunks, that should hold this byteLoc's byte.
     * NOTE: byteLoc is always an absolute byte pointer.  It is not a pointer to a voxel (that is, its byteLoc
     * "knows" bytes, and not multi-byte voxel locations).
     *
     * @param byteLoc along a linear interpretation of all bytes contained herein, find the chunk holding this atom.
     * @return a chunk whose data should have this in it.
     */
    private VolumeChunk findChunk( long byteLoc ) {
        //todo : convert byteLoc into a 3D location: three coords.  Then use that to compute the chunk.
        int chunkZ = (int)(byteLoc / partitionParameters.getSheetDivisor());
        long zOffset = chunkZ * partitionParameters.getSheetDivisor();
        int chunkY = (int)((byteLoc - zOffset) / partitionParameters.getLineDivisor() );
        long yOffset = partitionParameters.getLineDivisor() * chunkY;
        int chunkX = (int)((byteLoc - zOffset - yOffset) / partitionParameters.bytesPerAtom / partitionParameters.getPartitionDimX() / partitionParameters.getDimX());

        VolumeChunk rtnVal = null;
        try {
            rtnVal = cachedVolumeChunks[ chunkX ][ chunkY ][ chunkZ ];
        } catch ( Exception ex ) {
            ex.printStackTrace();
        }
        return rtnVal;
    }

    private void initializePartitions() {
        int xPartCt = (int) Math.ceil((double)partitionParameters.getDimX() / (double)partitionParameters.getPartitionDimX());
        int yPartCt = (int) Math.ceil((double)partitionParameters.getDimY() / (double)partitionParameters.getPartitionDimY());
        int zPartCt = (int) Math.ceil((double)partitionParameters.getDimZ() / (double)partitionParameters.getPartitionDimZ());
        cachedVolumeChunks = new VolumeChunk[ xPartCt ][ yPartCt ][ zPartCt ];

        for ( int iZ = 0; iZ < zPartCt; iZ++ ) {
            for ( int iY = 0; iY < yPartCt; iY++ ) {
                for ( int iX = 0; iX < zPartCt; iX++ ) {
                    VolumeChunk nextChunk = new VolumeChunk();

                    // Setting up the X axis.
                    nextChunk.setChunksLocation(
                            iX, iY, iZ,
                            partitionParameters.getSheetDivisor() * iZ + partitionParameters.getLineDivisor() * iY + partitionParameters.getPartitionDimX() * partitionParameters.getBytesPerAtom() * iX
                    );
                    int partitionXDim;
                    if ( ( 1 + iX) * partitionParameters.getPartitionDimX() < partitionParameters.getDimX() ) {
                        partitionXDim = partitionParameters.getPartitionDimX();
                    }
                    else {
                        partitionXDim = (int)(partitionParameters.getDimX() % partitionParameters.getPartitionDimX() );
                    }
                    nextChunk.setDimX( partitionXDim );
                    nextChunk.setStartX(iX * partitionParameters.getPartitionDimX());


                    // Setting up the Y axis.
                    int partitionYDim;
                    if ( ( 1 + iY) * partitionParameters.getPartitionDimY() < partitionParameters.getDimY() ) {
                        partitionYDim = partitionParameters.getPartitionDimY();
                    }
                    else {
                        partitionYDim = (int) (partitionParameters.getDimY() % partitionParameters.getPartitionDimY() );
                    }
                    nextChunk.setDimY(partitionYDim);
                    nextChunk.setStartY(iY * partitionParameters.getPartitionDimY());


                    // Setting up the Z axis.
                    int partitionZDim;
                    if ( ( 1 + iZ) * partitionParameters.getPartitionDimZ() < partitionParameters.getDimZ() ) {
                        partitionZDim = partitionParameters.getPartitionDimZ();
                    }
                    else {
                        partitionZDim = (int) (partitionParameters.getDimZ() % partitionParameters.getPartitionDimZ() );
                    }
                    nextChunk.setDimZ( partitionZDim );
                    nextChunk.setStartZ(iZ * partitionParameters.getPartitionDimZ());

                    // Initialize the data holder.
                    nextChunk.setVolumeData( new byte[ partitionXDim * partitionYDim * partitionZDim * partitionParameters.getBytesPerAtom() ] );

                    // Capture this chunk.
                    cachedVolumeChunks[ iX ][ iY ][ iZ ] = nextChunk;
                }
            }
        }
    }

    /** Parameter bean for holding parameters, for making a partitioned volume. */
    public static class PartitionParameters {
        private long dimX;
        private long dimY;
        private long dimZ;
        private long totalSize;
        private int partitionDimX;
        private int partitionDimY;
        private int partitionDimZ;
        private int bytesPerAtom;

        private long sheetDivisor = -1;
        private long lineDivisor = -1;

        public long getDimX() {
            return dimX;
        }

        public void setDimX(long dimX) {
            this.dimX = dimX;
        }

        public long getDimY() {
            return dimY;
        }

        public void setDimY(long dimY) {
            this.dimY = dimY;
        }

        public long getDimZ() {
            return dimZ;
        }

        public void setDimZ(long dimZ) {
            this.dimZ = dimZ;
        }

        public long getTotalSize() {
            return totalSize;
        }

        public void setTotalSize(long totalSize) {
            this.totalSize = totalSize;
        }

        public int getPartitionDimX() {
            return partitionDimX;
        }

        public void setPartitionDimX(int partitionDimX) {
            this.partitionDimX = partitionDimX;
        }

        public int getPartitionDimY() {
            return partitionDimY;
        }

        public void setPartitionDimY(int partitionDimY) {
            this.partitionDimY = partitionDimY;
        }

        public int getPartitionDimZ() {
            return partitionDimZ;
        }

        public void setPartitionDimZ(int partitionDimZ) {
            this.partitionDimZ = partitionDimZ;
        }

        public int getBytesPerAtom() {
            return bytesPerAtom;
        }

        public void setBytesPerAtom(int bytesPerAtom) {
            this.bytesPerAtom = bytesPerAtom;
        }

        public long getSheetDivisor() {
            if ( sheetDivisor == -1 ) {
                sheetDivisor = dimY * dimX * bytesPerAtom;
            }
            return sheetDivisor;
        }

        public long getLineDivisor() {
            if ( lineDivisor == -1 ) {
                lineDivisor = dimX * bytesPerAtom * (int)(Math.floor( dimY / partitionDimY ) );
            }
            return lineDivisor;
        }
    }

    /**
     * Data in this partitioned volume is kept in chunks like this. Chunk dimensions are in voxels (which may
     * be multi-byte) rather than raw bytes.
     */
    public static class VolumeChunk {
        private byte[] volumeData;
        private int dimX;
        private int dimY;
        private int dimZ;
        private int startX;
        private int startY;
        private int startZ;
        private long start1D;

        // These correspond to x,y,z; however, they indicate how this chunk is stored, relative to
        // other chunks, in some array or other collection.
        private int chunkColumn;
        private int chunkRow;
        private int chunkSlice;

        public void setChunksLocation( int[] chunksLocation, long start1D ) {
            setChunksLocation( chunksLocation[ 0 ], chunksLocation[ 1 ], chunksLocation[ 2 ], start1D );
        }

        public void setChunksLocation( int col, int row, int slice, long start1D ) {
            chunkColumn = col;
            chunkRow = row;
            chunkSlice = slice;
            this.start1D = start1D;
        }

        public int[] getChunksLocation() {
            return new int[] { chunkColumn, chunkRow, chunkSlice };
        }

        public byte[] getVolumeData() {
            return volumeData;
        }

        public void setVolumeData(byte[] volumeData) {
            this.volumeData = volumeData;
        }

        public int getDimX() {
            return dimX;
        }

        public void setDimX(int dimX) {
            this.dimX = dimX;
        }

        public int getDimY() {
            return dimY;
        }

        public void setDimY(int dimY) {
            this.dimY = dimY;
        }

        public int getDimZ() {
            return dimZ;
        }

        public void setDimZ(int dimZ) {
            this.dimZ = dimZ;
        }

        public int getStartX() {
            return startX;
        }

        public void setStartX(int startX) {
            this.startX = startX;
        }

        public int getStartY() {
            return startY;
        }

        public void setStartY(int startY) {
            this.startY = startY;
        }

        public int getStartZ() {
            return startZ;
        }

        public void setStartZ(int startZ) {
            this.startZ = startZ;
        }

        public long getStart1D() {
            return start1D;
        }
    }
}
