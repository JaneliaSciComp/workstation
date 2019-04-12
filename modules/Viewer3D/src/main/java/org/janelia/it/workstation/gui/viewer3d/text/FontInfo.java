/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.viewer3d.text;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Describes a font for 2D text.
 * 
 * @author fosterl
 */
public class FontInfo {
    public enum FontComponentExtension { properties, png }
    public enum FontStyle { Plain, Bold, Italic }
    public static final String FONT_RESOURCE_LOC = "/font/";

    public static final String NAME_PART_SEP = "_";
    private String[] allWidths = null;
    private int[] intWidths = null;
    private String[] allOffsets = null;
    private char[] allChars = null;
    private int fontHeight = -1;
    private String baseFontName;
    private int totalWidth = -1;
    private Map<Character, Integer> charVsOffset = new HashMap<>();

    public FontInfo(String fontFamily, FontStyle fontStyle, int fontSize, String colorName) {
        baseFontName = fontFamily + NAME_PART_SEP + fontStyle.toString().toLowerCase() + NAME_PART_SEP + fontSize + NAME_PART_SEP + colorName + ".";
        Properties fontProperties = new Properties();
        try {
            fontProperties.load(getFontComponentStream( FontComponentExtension.properties ));            
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new AxisLabel.UnknownFontException(baseFontName);
        }
        init(fontProperties);
    }

    /**
     * A font is made up of more than one component.  Each is designated by
     * its own extension, and each is represented by a file.  This method
     * returns the stream associated with the component/file/extension provided.
     * 
     * @param ext which component, by enum'd extension.
     * @return input stream.
     */
    public InputStream getFontComponentStream( FontComponentExtension ext ) {
        final String fullResourceName = getFullResourceName( ext );
        System.out.println("Full Resource Name is " + fullResourceName);
        return FontInfo.class.getResourceAsStream( fullResourceName );
    }

    public String[] getAllWidths() {
        return allWidths;
    }

    public void setAllWidths(String[] allWidths) {
        this.allWidths = allWidths;
    }

    public int[] getIntWidths() {
        return intWidths;
    }
    
    /** In terms of the font, how long is this text? */
    public int getWidth( String text ) {
        int combinedWidth = 0;
        for ( Character c: text.toCharArray() ) {
            int offset = charVsOffset.get( c );
            combinedWidth += intWidths[ offset ];
        }
        return combinedWidth;
    }

    public String[] getAllOffsets() {
        return allOffsets;
    }

    public void setAllOffsets(String[] allOffsets) {
        this.allOffsets = allOffsets;
    }

    public char[] getAllChars() {
        return allChars;
    }

    public void setAllChars(char[] allChars) {
        this.allChars = allChars;
    }

    public int getFontHeight() {
        return fontHeight;
    }

    public void setFontHeight(int fontHeight) {
        this.fontHeight = fontHeight;
    }

    public String getBaseFontName() {
        return baseFontName;
    }

    /**
     * @return the totalWidth
     */
    public int getTotalWidth() {
        return totalWidth;
    }

    /**
     * @param totalWidth the totalWidth to set
     */
    public void setTotalWidth(int totalWidth) {
        this.totalWidth = totalWidth;
    }
    
    public String toString() {
        StringBuilder rtnVal = new StringBuilder();
        for ( FontComponentExtension value: FontComponentExtension.values() ) {
            if ( rtnVal.length() > 0 ) {
                rtnVal.append( " " );
            }
            rtnVal.append( value );
        }
        return rtnVal.toString();
    }

    private void init(Properties fontProperties) {
        setAllChars(fontProperties.getProperty("alphabet").toCharArray());
        setAllWidths(fontProperties.getProperty("charwidths").split(","));
        setAllOffsets(fontProperties.getProperty("charoffsets").split(","));
        setFontHeight(Integer.parseInt(fontProperties.getProperty("height")));
        setTotalWidth(Integer.parseInt(fontProperties.getProperty("totalwidth")));
        intWidths = new int[ allWidths.length ];
        for (int i = 0; i < allWidths.length; i++) {
            intWidths[i] = Integer.parseInt(allWidths[i]);
        }
        for ( int i = 0; i < allChars.length; i++ ) {
            charVsOffset.put( allChars[i], i );
        }
    }

    private String getFullResourceName(FontComponentExtension ext) {
        return FONT_RESOURCE_LOC + baseFontName + ext;
    }

}
