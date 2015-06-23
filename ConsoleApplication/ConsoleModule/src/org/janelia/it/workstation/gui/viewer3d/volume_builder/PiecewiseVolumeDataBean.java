/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.viewer3d.volume_builder;

import org.janelia.it.workstation.gui.viewer3d.masking.VolumeDataI;

/**
 * This volume data bean can be fed from arbitrarily-sized chunks.
 * @author fosterl
 */
public class PiecewiseVolumeDataBean implements VolumeDataI {
    
    private int sx;
    private int sy;
    private int sz;
    
    private int bytesPerVoxel;    
    private int bytesPerChunk;
    private int slicesPerChunk;
    private long totalSize;
    
    private LinearVolumeDataChunk[] chunks;
    
    private long nextInputLocation;
    
    public PiecewiseVolumeDataBean( int x, int y, int z, int bytesPerVoxel, int targetSlicePerChunk ) {
        this.sx = x;
        this.sy = y;
        this.sz = z;
        this.bytesPerVoxel = bytesPerVoxel;
        this.slicesPerChunk = targetSlicePerChunk;
        
        int chunkCount = (int)Math.ceil((double)sz / (double)slicesPerChunk);
        
        totalSize = (long)sx * (long)sy * (long)sz * (long)bytesPerVoxel;
        chunks = new LinearVolumeDataChunk[ chunkCount ];
        bytesPerChunk = sx * sy * slicesPerChunk * bytesPerVoxel;
        if (slicesPerChunk < 1) {
            throw new IllegalArgumentException("You are specifying too many chunk divisions for the data size.");
        }
        buildChunks();
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
        if (location > totalSize) {
            return 0;
        }
        int chunkNum = getChunkNum(location);
        int chunkOffset = getIntraChunkOffset(location);
        return chunks[chunkNum].getData()[chunkOffset];
    }

    @Override
    public void setValueAt(long location, byte value) {
        if (location > totalSize) {
            throw new IllegalArgumentException(location + " exceeds size of " + totalSize);
        }
        // This implementation not used at time of writing.
        int locChunk = getChunkNum(location);
        int chunkOffset = getIntraChunkOffset(location);
        LinearVolumeDataChunk chunk = chunks[locChunk];
        chunk.getData()[chunkOffset] = value;
    }

    @Override
    public long length() {
        return totalSize;
    }
    
    /**
     * Adding the piece-wise data.  Array will be contributed whole, or split
     * across chunk boundaries. All data are expected to be added sequentially
     * using this method.  The data will then be laid out from beginning
     * to end, in the order received.
     * 
     * @param data next piece to add.
     */
    public void addData( byte[] data ) {
        if (nextInputLocation + data.length >= totalSize) {
            throw new IllegalArgumentException("Additional data of length " + data.length + " would exceed total size of " + totalSize);
        }
        LinearVolumeDataChunk chunk = chunks[getChunkNum(nextInputLocation)];
        int ptrInData = 0;
        if (data.length == 0) {
            return;
        }
        if (nextInputLocation + data.length <= chunk.getEndingLinearLocation()) {
            final int readAmount = data.length;
            if (ptrInData + readAmount > data.length) { //TEMP
                throw new IllegalStateException("buffer overrun/input data");
            }
            final int ptrInChunk = (int) (nextInputLocation - chunk.getStartingLinearLocation());
            assert ptrInChunk >= 0 :
                    "Next input location smaller than start of chunk";
            System.arraycopy(data, ptrInData, chunk.getData(), ptrInChunk, readAmount);
            nextInputLocation += readAmount;
        }
        else {
            // Start in current chunk.
            while (ptrInData < data.length) {
                if (getChunkNum(nextInputLocation) >= chunks.length) {
                    throw new IllegalArgumentException("Data size exceeds capacity.");
                }
                chunk = chunks[ getChunkNum(nextInputLocation) ];
                // Start point in current chunk.
                int availableInChunk = (int)(chunk.getEndingLinearLocation() - nextInputLocation);
                int availableInData = data.length - ptrInData;
                int readAmount = Math.min(availableInChunk, availableInData);
                
                int ptrInChunk = (int)(nextInputLocation - chunk.getStartingLinearLocation());
                assert ptrInChunk >= 0 :
                        "Next input location smaller than start of chunk";
                System.arraycopy(data, ptrInData, chunk.getData(), ptrInChunk, readAmount);                
                nextInputLocation += readAmount;
                ptrInData += readAmount;
            }
        }
    }
    
    private int getChunkNum( long location ) {
        return (int)(location / bytesPerChunk);
    }
    
    private int getIntraChunkOffset( long location ) {
        int chunkNum = getChunkNum( location );
        return (int)(location - (chunkNum * bytesPerChunk));
    }
    
    private void buildChunks() {
        int nextSlice = 0;
        long nextLinearLocation = 0;
        for (int i = 0; i < chunks.length; i++) {
            LinearVolumeDataChunk chunk = new LinearVolumeDataChunk();
            chunk.setWidth(sx);
            chunk.setHeight(sy);
            // Final chunk can be irregular.
            int slices = slicesPerChunk;
            if (nextSlice + slices > sz) {
                slices = sz - nextSlice;
            }
            chunk.setDepth(slices);

            chunk.setStartX(0);
            chunk.setStartY(0);
            chunk.setStartZ(nextSlice);
            
            chunk.setStartingLinearLocation(nextLinearLocation);
            chunks[i] = chunk;
            final int linearVolumeSize = sx * sy * slices * bytesPerVoxel;
            chunk.setEndingLinearLocation(nextLinearLocation + linearVolumeSize);
            
            chunk.setData(new byte[ linearVolumeSize ]); 
            nextLinearLocation += linearVolumeSize;
            nextSlice += slices;
        }
    }

    /**
     * A data chunk which conveniently can keep track of linear--not just
     * 3D--start/end locations.
     */
    private static class LinearVolumeDataChunk extends VolumeDataChunk {
        private long startingLinearLocation;
        private long endingLinearLocation;

        /**
         * @return the startingLinearLocation
         */
        public long getStartingLinearLocation() {
            return startingLinearLocation;
        }

        /**
         * @param startingLinearLocation the startingLinearLocation to set
         */
        public void setStartingLinearLocation(long startingLinearLocation) {
            this.startingLinearLocation = startingLinearLocation;
        }

        /**
         * @return the endingLinearLocation
         */
        public long getEndingLinearLocation() {
            return endingLinearLocation;
        }

        /**
         * @param endingLinearLocation the endingLinearLocation to set
         */
        public void setEndingLinearLocation(long endingLinearLocation) {
            this.endingLinearLocation = endingLinearLocation;
        }
    }
}
