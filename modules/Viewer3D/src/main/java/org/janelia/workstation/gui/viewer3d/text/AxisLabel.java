/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.workstation.gui.viewer3d.text;

import java.util.HashMap;
import java.util.Map;

/**
 * Information to represent a label for OpenGL use.
 * 
 * @author fosterl
 */
public class AxisLabel {

    private static final double TEXT_CHAR_SPACER = 0.0001;
    private static final int VERTICES_PER_CHARACTER = 18;

    public enum AxisOfParallel {
        x, y, z;
        public static AxisOfParallel getValue( int axisOffset ) {
            switch ( axisOffset ) {
                case 0:
                    return x;
                case 1:
                    return y;
                case 2:
                    return z;
                default:
                    throw new IllegalArgumentException("No axis corresponds to " + axisOffset + " in 3D space.");
            }
        }
    }
    private Map<Character,FontCharacterInfo> characterToInfo;

    private FontInfo fontInfo;
    
    private float[] texCoords;
    private float[] vtxCoords;
    
    private float totalWidth = 0f;

    /**
     * Creates something that can be used for placing text into OpenGL display.
     * 
     * @param text what you want to say.
     * @param fontInfo for describing the font.
     * @param textBounds for placing the vertices onto which the label is stenciled.
     * @param axisOfParallel helps to move the label relative to some axis.
     * @throws AxisLabel.UnknownFontException
     */
    public AxisLabel( String text, FontInfo fontInfo, TextBounds textBounds, AxisOfParallel axisOfParallel ) throws UnknownFontException {
        totalWidth = fontInfo.getTotalWidth();
        this.fontInfo = fontInfo;

        texCoords = createTexCoords(text, totalWidth);
        vtxCoords = createVtxCoords(text, fontInfo, textBounds, axisOfParallel);

    }

    /**
     * Helps work out total required storage.
     * @return total bytes for whole label's corners.
     */
    public int getVtxAttributeSize() {
        return Float.SIZE / 8 * (
                        vtxCoords.length +
                        texCoords.length
               ); 
    }
    
    public float[] getVtxCoords() {
        return vtxCoords;
    }
    
    public float[] getTexCoords() {
        return texCoords;
    }
    
    public float getWidth() {
        return totalWidth;
    }
    
    public float getHeight() {
        return fontInfo.getFontHeight();
    }

    private float[] createVtxCoords(String text, FontInfo fontInfo, TextBounds textBounds, AxisOfParallel axisOfParallel) {
        float labelWidth = textBounds.getWidth();
        float x = textBounds.getX();
        float y = textBounds.getY();
        float z = textBounds.getZ();
        
        int charCount = text.length();
        int[] charWidths = fontInfo.getIntWidths();
        float[] coordinateData = new float[VERTICES_PER_CHARACTER * charCount];

        int startLabelVertexNum = 0;

        byte[] indices = new byte[ 6 * charCount];
        int totalMessageWidth = 0;
        for ( int i = 0; i < charCount; i++ ) {
            totalMessageWidth += charWidths[ i ] + TEXT_CHAR_SPACER;
        }
        float fitFactor = labelWidth / (float)totalMessageWidth;

        for ( int i = 0; i < charCount; i++ ) {
            // Two triangles per character.
            // Triangle 1
            float xAdvance = charWidths[ i ] * fitFactor;
            coordinateData[startLabelVertexNum] = x;
            coordinateData[startLabelVertexNum + 1] = y;
            coordinateData[startLabelVertexNum + 2] = z;

            coordinateData[startLabelVertexNum + 3] = x;
            coordinateData[startLabelVertexNum + 4] = y + textBounds.getHeight();
            coordinateData[startLabelVertexNum + 5] = z;

            coordinateData[startLabelVertexNum + 6] = x + xAdvance;
            coordinateData[startLabelVertexNum + 7] = y + textBounds.getHeight();
            coordinateData[startLabelVertexNum + 8] = z;

            // Triangle 2
            coordinateData[startLabelVertexNum + 9] = x + xAdvance;
            coordinateData[startLabelVertexNum + 10] = y + textBounds.getHeight();
            coordinateData[startLabelVertexNum + 11] = z;

            coordinateData[startLabelVertexNum + 12] = x + xAdvance;
            coordinateData[startLabelVertexNum + 13] = y;
            coordinateData[startLabelVertexNum + 14] = z;

            coordinateData[startLabelVertexNum + 15] = x;
            coordinateData[startLabelVertexNum + 16] = y;
            coordinateData[startLabelVertexNum + 17] = z;

            startLabelVertexNum += 18;

            x += xAdvance + TEXT_CHAR_SPACER;

        }
        
        if ( axisOfParallel != AxisOfParallel.x ) {
            coordinateData = transformGeometry( axisOfParallel, coordinateData );
        }
        
        totalWidth = x - textBounds.getX();
        
        return coordinateData;
    }

    private float[] createTexCoords(String text, float totalWidth) {
        int textLength = text.length();
        int coordCount = 6 * 2 * textLength;
        float[] allTexCoords = new float[coordCount];
        int vi = 0;
        for (int i = 0; i < textLength; i++) {
            FontCharacterInfo info = getFontCharacterInfo(text.charAt(i));
            
            // Triangle one.
            allTexCoords[ vi++] = info.getOffset() / totalWidth;        // Bottom left corner.
            allTexCoords[ vi++] = 1.0f;
            allTexCoords[ vi++] = info.getOffset() / totalWidth;        // Top left corner.
            allTexCoords[ vi++] = 0.0f;
            allTexCoords[ vi++] = (info.getOffset() + info.getWidth()) / totalWidth; // Top right corner.
            allTexCoords[ vi++] = 0.0f;
            
            // Triangle two.
            allTexCoords[ vi++] = (info.getOffset() + info.getWidth()) / totalWidth; // Top right corner.
            allTexCoords[ vi++] = 0.0f;
            allTexCoords[ vi++] = (info.getOffset() + info.getWidth()) / totalWidth; // Bottom right corner.
            allTexCoords[ vi++] = 1.0f;
            allTexCoords[ vi++] = info.getOffset() / totalWidth;        // Bottom left corner.
            allTexCoords[ vi++] = 1.0f;
            
        }
        return allTexCoords;
    }
    
    private float[] transformGeometry( AxisOfParallel axisOfParallel, float[] coordinateData ) {
        float[] returnVal = new float[ coordinateData.length ];
        if ( axisOfParallel == axisOfParallel.z ) {
            // rotate 90 degrees around Y axis, then slide it forward by 2 times its distance from origin.
            
        }
        return returnVal;
    }
    
    private FontCharacterInfo getFontCharacterInfo(char ch) {
        FontCharacterInfo fci = null;
        if ( characterToInfo == null ) {
            characterToInfo = new HashMap<>();
        }
        else {
            fci = characterToInfo.get( ch );
        }
        if ( fci == null ) {
            fci = new FontCharacterInfo( ch, fontInfo );
            characterToInfo.put( ch, fci );
        }
        return fci;
    }

    public static class TextBounds {
        private float x;
        private float y;
        private float z;
        private float width;
        private float height;

        public TextBounds(
                float x,
                float y,
                float z,
                float width,
                float height
        ) {
            setX( x );
            setY( y );
            setZ( z );
            setWidth( width );
            setHeight( height );
        }
        
        /**
         * @return the x
         */
        public float getX() {
            return x;
        }

        /**
         * @param x the x to set
         */
        public final void setX(float x) {
            this.x = x;
        }

        /**
         * @return the y
         */
        public float getY() {
            return y;
        }

        /**
         * @param y the y to set
         */
        public final void setY(float y) {
            this.y = y;
        }

        /**
         * @return the z
         */
        public float getZ() {
            return z;
        }

        /**
         * @param z the z to set
         */
        public final void setZ(float z) {
            this.z = z;
        }

        /**
         * @return the width
         */
        public float getWidth() {
            return width;
        }

        /**
         * @param width the width to set
         */
        public final void setWidth(float width) {
            this.width = width;
        }

        /**
         * @return the height
         */
        public float getHeight() {
            return height;
        }

        /**
         * @param height the height to set
         */
        public final void setHeight(float height) {
            this.height = height;
        }
    }

    private static class FontCharacterInfo {
        private int width;
        private int offset;
        private char ch;

        public FontCharacterInfo( int index, FontInfo fontInfo ) {
            init(index, fontInfo.getAllChars(), fontInfo.getAllWidths(), fontInfo.getAllOffsets() );
        }

        public FontCharacterInfo( char ch, FontInfo fontInfo ) {
            if ( Character.isWhitespace( ch ) ) ch = ' ';
            for ( int i = 0; i < fontInfo.getAllChars().length; i++ ) {
                if ( ch == fontInfo.getAllChars()[ i ] ) {
                    init( i, fontInfo.getAllChars(), fontInfo.getAllWidths(), fontInfo.getAllOffsets() );
                    break;
                }
            }
        }

        public int getWidth() {
            return width;
        }

        public int getOffset() {
            return offset;
        }

        public char getCh() {
            return ch;
        }

        private void init(int index, char[] allChars, String[] widthStrs, String[] offsetStrs) {
            width = Integer.parseInt( widthStrs[ index ] );
            offset = Integer.parseInt( offsetStrs[ index ] );
            ch = allChars[ index ];
        }

    }

    public static class UnknownFontException extends RuntimeException {
        public UnknownFontException( String fontImageName ) {
            super( "Unknown Font Family or Size+Style Variant: " + fontImageName + ", please try a different combination." );
        }
    }
    
}
