/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.viewer3d.loader;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferUShort;

import Jama.Matrix;
import java.util.List;

/**
 *
 * @author fosterl
 */
public class LoaderSubsetHelper {
    
    private static final int SPACE_N = 3;
    private static final int START_X_INX = 0;
    private static final int END_X_INX = START_X_INX + SPACE_N;
    private static final int START_Y_INX = 1;
    private static final int END_Y_INX = START_Y_INX + SPACE_N;
    private static final int START_Z_INX = 2;
    private static final int END_Z_INX = START_Z_INX + SPACE_N;

    private int sx;
    private int sy;
    private int sz;
    
    private int sourceWidth;
    private int sourceHeight;
    private int sourceDepth;

    private int[] minCorner;
    private int[] extent;
    private double[][] stageToTile;
    private double[][] tileToStage;
    private double[] queryCoords;
    private int[] cameraToCentroidDistance;
    private int cubicOutputDimension = -1;
    private final int[] boundingBox = new int[6];
    
    private byte[] textureByteArray;
    private int[] argbTextureIntArray;
    private int[] tempIntBuffer;
    
    private int pixelBytes;

    public void setCameraToCentroidDistance( int[] distance ) {
        this.cameraToCentroidDistance = distance; 
    }
    
    public void setCubicOutputDimension( int dimension ) {
        this.cubicOutputDimension = dimension;
    }
    
    public void setTransformCharacteristics(double[][] tileToStage, double[][] stageToTile, int[] minCorner, int[] extent, List<Integer> queryCoords) {
        this.tileToStage = tileToStage;
        this.stageToTile = stageToTile;
        this.minCorner = minCorner;
        this.extent = extent;
        this.queryCoords = convert(queryCoords, 4);
    }
    
    /**
     * @return the cubicOutputDimension
     */
    public int getCubicOutputDimension() {
        return cubicOutputDimension;
    }
    
    /**
     * @return the sourceWidth
     */
    public int getSourceWidth() {
        return sourceWidth;
    }

    /**
     * @param sourceWidth the sourceWidth to set
     */
    public void setSourceWidth(int sourceWidth) {
        this.sourceWidth = sourceWidth;
    }

    /**
     * @return the sourceHeight
     */
    public int getSourceHeight() {
        return sourceHeight;
    }

    /**
     * @param sourceHeight the sourceHeight to set
     */
    public void setSourceHeight(int sourceHeight) {
        this.sourceHeight = sourceHeight;
    }

    public void setSourceDepth( int sourceDepth ) {
        this.sourceDepth = sourceDepth;
    }

    /**
     * @return the sx
     */
    public int getSx() {
        return sx;
    }

    /**
     * @param sx the sx to set
     */
    public void setSx(int sx) {
        this.sx = sx;
    }

    /**
     * @return the sy
     */
    public int getSy() {
        return sy;
    }

    /**
     * @param sy the sy to set
     */
    public void setSy(int sy) {
        this.sy = sy;
    }

    /**
     * @return the sz
     */
    public int getSz() {
        return sz;
    }

    /**
     * @param sz the sz to set
     */
    public void setSz(int sz) {
        this.sz = sz;
    }
    
    /**
     * @return the textureByteArray
     */
    public byte[] getTextureByteArray() {
        return textureByteArray;
    }

    /**
     * @param textureByteArray the textureByteArray to set
     */
    public void setTextureByteArray(byte[] textureByteArray) {
        this.textureByteArray = textureByteArray;
    }

    /**
     * @return the argbTextureIntArray
     */
    public int[] getArgbTextureIntArray() {
        return argbTextureIntArray;
    }

    /**
     * @param argbTextureIntArray the argbTextureIntArray to set
     */
    public void setArgbTextureIntArray(int[] argbTextureIntArray) {
        this.argbTextureIntArray = argbTextureIntArray;
    }

    /**
     * @return the pixelBytes
     */
    public int getPixelBytes() {
        return pixelBytes;
    }

    /**
     * @param pixelBytes the pixelBytes to set
     */
    public void setPixelBytes(int pixelBytes) {
        this.pixelBytes = pixelBytes;
    }
    
    private double[] convert( List<Integer> intList, int pointDim ) {
        // Need to have a 1x4 matrix, to account for translation column.
        double[] rtnVal = new double[ pointDim ];
        for ( int i = 0; i < intList.size(); i++ ) {
            rtnVal[ i ] = intList.get(i);
        }
        // Pad the end with 1's.
        for ( int i = intList.size(); i < pointDim; i++ ) {
            rtnVal[ i ] = 1.0;
        }
        return rtnVal;
    }
    
    private double[] convert( int[] intArr, int pointDim ) {
        // Need to have a 1x4 matrix, to account for translation column.
        double[] rtnVal = new double[ pointDim ];
        for ( int i = 0; i < intArr.length; i++ ) {
            rtnVal[ i ] = intArr[ i ];
        }
        // Pad the end with 1's.
        for ( int i = intArr.length; i < pointDim; i++ ) {
            rtnVal[ i ] = 1.0;
        }
        return rtnVal;
    }
    
    public int captureAndUsePageDimensions(final int zCount, final long fileLength) {
        int pointDim = 4;
        // Experiment time!
        System.out.println( this.queryCoords[0] + "," + this.queryCoords[1] + "," + this.queryCoords[2] );
        Matrix transform = new Matrix( stageToTile );
        Matrix pointMatrix = new Matrix( queryCoords, pointDim );
        // This should yield the point in TIFF coordinates, 0..MaxX, 0..MaxY, 0..MaxZ.
        Matrix newPoint = transform.times( pointMatrix );

        /*
        DEBUG: if doubts should arise, uncomment this, and check results.
        Matrix newMin = transform.times( new Matrix( new double[]{ minCorner[0], minCorner[1], minCorner[2], 1.0 }, 4 ) );        
        Matrix forwardTransform = new Matrix( tileToStage );
        Matrix testMin = transform.times( new Matrix( new double[] {0,0,0,1}, 4 ) );
        */
        
        final int halfCubeDim = getCubicOutputDimension() / 2;
        boundingBox[START_X_INX] = clamp( 0, getSourceWidth() - getCubicOutputDimension(), (int)newPoint.getArray()[ 0 ][ 0 ] - halfCubeDim );
        boundingBox[START_Y_INX] = clamp( 0, getSourceHeight() - getCubicOutputDimension(), (int)newPoint.getArray()[ 1 ][ 0 ] - halfCubeDim );
        boundingBox[START_Z_INX] = clamp( 0, zCount - getCubicOutputDimension(), (int)newPoint.getArray()[ 2 ][ 0 ] - halfCubeDim );

        boundingBox[END_X_INX] = boundingBox[START_X_INX] + getCubicOutputDimension();
        boundingBox[END_Y_INX] = boundingBox[START_Y_INX] + getCubicOutputDimension();
        boundingBox[END_Z_INX] = boundingBox[START_Z_INX] + getCubicOutputDimension();
        
        // Adjust the dimensions, to adjust the subsetting requirement.
        setSx(boundingBox[ END_X_INX ] - boundingBox[ START_X_INX ]);
        setSy(boundingBox[ END_Y_INX ] - boundingBox[ START_Y_INX ]);
        setSz(boundingBox[ END_Z_INX ] - boundingBox[ START_Z_INX ]);

        int sheetSize = getSx() * getSy();
        final int totalVoxels = sheetSize * getSz();
        setPixelBytes((int)Math.floor( fileLength / ((getSourceWidth()*getSourceHeight()) * sourceDepth) ));
        //System.out.println("File size is " + file.length());
        //System.out.println("Expected number of voxels = " + (sourceWidth*sourceHeight) * sheetCountFromFile );
        if ( getPixelBytes() < 4  ||  getCubicOutputDimension() != -1 ) {
            setTextureByteArray(new byte[totalVoxels * getPixelBytes()]);
        }
        else {
            setArgbTextureIntArray(new int[totalVoxels]);
        }

        // DEBUG CODE>
        for ( int i = 0; i < 3; i++ ) {
            System.out.println( "Bounding Box offset=" + i + " value=" + boundingBox[i] );
            System.out.println( "Bounding Box offset=" + (i+3) + " value=" + boundingBox[i+3] );
            System.out.println();
        }
        return sheetSize;
    }
    
    /**
     * If this read is meant to take only a subset of the input and push that
     * to the output buffer, this method will carve out that chunk, and place
     * it into a buffer of the exact size needed.
     * 
     * @param destZ where in the output buffer to place this "sheet".
     * @param destSheetSize size of sheet: dest X * dest Y
     * @param zSlice a buffered image from which to extract a partial sheet.
     */
    public void storeSubsetToBuffer(int destZ, int destSheetSize, BufferedImage zSlice) {
        DataMover dataMover;
        if ( getPixelBytes() == 1 ) {
            DataBufferByte db = ((DataBufferByte)zSlice.getTile(0, 0).getDataBuffer());
            byte[] pixels = db.getData();
            dataMover = new ByteDataMover( pixels );
        }
        else if ( getPixelBytes() == 2 ) {
            DataBufferUShort db = ((DataBufferUShort)zSlice.getTile(0, 0).getDataBuffer());
            short[] pixels = db.getData();
            dataMover = new ShortDataMover( pixels );
        }
        else if ( getPixelBytes() == 4 ) {            
            if ( tempIntBuffer == null ) {
                tempIntBuffer = new int[ getSourceWidth() * getSourceHeight() ];
            }
            zSlice.getRGB(0, 0, getSx(), getSy(),
                    tempIntBuffer, 0, getSx());
            dataMover = new IntDataMover( tempIntBuffer );
        }
        else {
            throw new IllegalStateException( "Unexpected pixelBytes count == " + getPixelBytes() );
        }
        transferSubset(destZ, destSheetSize, dataMover);
    }
    
    public boolean inZSubset( int zOffset ) {
        return zOffset >= boundingBox[START_Z_INX] && zOffset < boundingBox[END_Z_INX];
    }

    /**
     * Convenience method encapsulating common functionality, to all widths
     * of data that will be transferred.
     * 
     * @param destZ how far along in output.
     * @param destSheetSize size of one x * y of data.
     * @param dataMover specific to pixel bytes count.
     */
    private void transferSubset(int destZ, int destSheetSize, DataMover dataMover) {
        // The source will be organized into one-voxel-per-array-entry.  That is
        // the source will be in bytes, words, ints, etc.
        int destOffset = ((destZ * destSheetSize) * getPixelBytes());
        for ( int sourceY = boundingBox[ START_Y_INX ]; sourceY < boundingBox[ END_Y_INX ]; sourceY++ ) {
            int sourceOffset = (getSourceWidth() * sourceY) + boundingBox[ START_X_INX ];
            for ( int sourceX = boundingBox[ START_X_INX ]; sourceX < boundingBox[ END_X_INX ]; sourceX++ ) {
                // move pixelbytes worth of data from source to destination.
                dataMover.moveData(sourceOffset, destOffset);
                destOffset += getPixelBytes();
                sourceOffset ++;
            }
        }
        
    }
    
    private int clamp( int min, int max, int startingValue ) {
        int rtnVal = startingValue;
        System.out.print("StartingValue=" + startingValue);
        if ( startingValue < min ) {
            rtnVal = min;
        }
        else if ( startingValue > max ) {
            rtnVal = max;
        }
        System.out.println(", clamped to " + rtnVal);
        return rtnVal;
    }

    private interface DataMover {
        void moveData(int sourceOffset, int destOffset);
    }
    
    private class ByteDataMover implements DataMover {
        private final byte[] pixels;
        
        public ByteDataMover(byte[] pixels) {
            this.pixels = pixels;
        }
        
        @Override
        public void moveData( int sourceOffset, int destOffset ) {
            System.arraycopy(pixels, sourceOffset, getTextureByteArray(), destOffset, getPixelBytes());            
        }
    }
    
    private class ShortDataMover implements DataMover {
        private final short[] pixels;
        
        public ShortDataMover(short[] pixels) {
            this.pixels = pixels;
        }
        
        @Override
        public void moveData( int sourceOffset, int destOffset ) {            
            // Changing the order.
            int unsignedPixelVal = pixels[ sourceOffset ];
            if (unsignedPixelVal < 0) {
                unsignedPixelVal += 65536;
            }
            byte byteVal = (byte) ((unsignedPixelVal & 0x0000ff00) >> 8);
            getTextureByteArray()[ destOffset + 1 ] = byteVal;
            byteVal = (byte) (unsignedPixelVal & 0x000000ff);
            getTextureByteArray()[ destOffset ] = byteVal;
        }
    }
    
    private class IntDataMover implements DataMover {
        private final int[] pixels;
        
        public IntDataMover(int[] pixels) {
            this.pixels = pixels;
        }
        
        @Override
        public void moveData( int sourceOffset, int destOffset ) {
            // Changing the order.
            long value = pixels[sourceOffset];
            if ( value < 0 ) {
                value += Integer.MAX_VALUE;
            }
            for (int pi = 0; pi < getPixelBytes(); pi++) {
                byte piByte = (byte) (value >>> (pi * 8) & 0x000000ff);
                getTextureByteArray()[ destOffset + pi ] = piByte;
            }
        }
    }
    
}
