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
    private int nextInputChunkNum;
    
    public PiecewiseVolumeDataBean( int x, int y, int z, int bytesPerVoxel, int chunkCount ) {
        this.sx = x;
        this.sy = y;
        this.sz = z;
        this.bytesPerVoxel = bytesPerVoxel;
        
        totalSize = (long)(sx * sy * sz * bytesPerVoxel);
        chunks = new LinearVolumeDataChunk[ chunkCount ];
        bytesPerChunk = (int)(totalSize / chunkCount);
        slicesPerChunk = bytesPerChunk / sx / sy / bytesPerVoxel;
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
        LinearVolumeDataChunk chunk = chunks[nextInputChunkNum];
        int ptrInData = 0;
        if (nextInputLocation + data.length <= chunk.getEndingLinearLocation()) {
            System.arraycopy(data, ptrInData, chunk.getData(), (int)(nextInputLocation - chunk.getStartingLinearLocation()), data.length);
            nextInputLocation += data.length;
        }
        else {
            // Start in current chunk.
            while (ptrInData < data.length) {
                if (nextInputChunkNum >= chunks.length) {
                    throw new IllegalArgumentException("Data size exceeds capacity.");
                }
                chunk = chunks[ nextInputChunkNum ];
                // Start point in current chunk.
                int availableInChunk = (int)(chunk.getEndingLinearLocation() - nextInputLocation);
                int availableInData = data.length - ptrInData;
                int readAmount = Math.min(availableInChunk, availableInData);
                
                int ptrInChunk = (int)(nextInputLocation - chunk.getStartingLinearLocation());
                if (ptrInData > data.length) { //TEMP
                    throw new IllegalStateException("buffer overrun/input data");
                }
                if (ptrInChunk > chunk.getEndingLinearLocation() - chunk.getStartingLinearLocation()) { //TEMP
                    throw new IllegalStateException("buffer overrun/output data");
                }
                System.arraycopy(data, ptrInData, chunk.getData(), ptrInChunk, readAmount);
                
                nextInputLocation += readAmount;
                ptrInData += readAmount;
                nextInputChunkNum++;
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
            if (nextSlice + slicesPerChunk > sz) {
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
